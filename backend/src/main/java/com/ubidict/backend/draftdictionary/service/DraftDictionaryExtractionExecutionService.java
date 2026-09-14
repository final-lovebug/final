package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.implement.CandidateTermWriter;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryEventPublisher;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryWriter;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobCompletionEventPublisher;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobReader;
import com.ubidict.backend.draftdictionary.implement.ExtractionResultValidator;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDictionaryExtractionExecutionService {

    private final ExtractionJobReader extractionJobReader;
    private final DraftDictionaryWriter draftDictionaryWriter;
    private final CandidateTermWriter candidateTermWriter;
    private final DraftDictionaryCreationPolicyValidator creationPolicyValidator;
    private final ExtractionResultValidator extractionResultValidator;
    private final DraftDictionaryEventPublisher draftDictionaryEventPublisher;
    private final ExtractionJobCompletionEventPublisher completionEventPublisher;

    /**
     * 워커에게 넘기기 직전에 작업을 {@code RUNNING}으로 옮기고 상관 식별자를 기록한다.
     *
     * <p>{@code PENDING}이 아니면 {@code Optional.empty()}다 — 발행 신호가 중복으로 도착해도 두 번 보내지 않는다.
     */
    @Transactional
    public Optional<ExtractionJobResult> markDispatching(Long extractionJobId, String requestId) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
        if (extractionJob.getStatus() != ExtractionJobStatus.PENDING) {
            return Optional.empty();
        }
        extractionJob.markDispatching(requestId);
        log.info(
                "[DraftDictionaryExtractionExecutionService.markDispatching] Draft dictionary extraction dispatched. extractionJobId={}, requestId={}",
                extractionJobId,
                requestId);
        return Optional.of(ExtractionJobResult.from(extractionJob));
    }

    /**
     * 워커가 돌려준 추출 결과를 초안으로 굳힌다.
     *
     * <p><b>이미 끝난 작업이면 아무 일도 하지 않는다</b>(D-72). SQS는 at-least-once라 같은 결과가 두 번 올 수 있는데, 가드가 없으면 두 번째
     * 호출이 「워크스페이스에 진행 중인 초안이 있다」는 409로 튕겨 워커에게 <i>재시도하라</i>는 잘못된 신호를 준다.
     */
    @Transactional
    public void complete(
            Long extractionJobId, String requestId, List<Long> sourceDocumentIds, List<ExtractedTerm> extractedTerms) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
        validateCallback(extractionJob, requestId);
        if (!extractionJob.isInProgress()) {
            log.info(
                    "[DraftDictionaryExtractionExecutionService.complete] Callback for terminal job ignored. extractionJobId={}, status={}",
                    extractionJobId,
                    extractionJob.getStatus());
            return;
        }
        if (!extractionJob.getSourceDocumentIds().containsAll(sourceDocumentIds)
                || sourceDocumentIds.size()
                        != extractionJob.getSourceDocumentIds().size()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_RESULT);
        }
        creationPolicyValidator.validate(extractionJob.getWorkspaceId());
        extractionResultValidator.validate(sourceDocumentIds, extractedTerms);

        DraftDictionary draftDictionary = draftDictionaryWriter.create(
                extractionJob.getWorkspaceId(),
                extractionJob.getDictionaryId(),
                sourceDocumentIds,
                extractionJob.getRequestedBy());
        for (ExtractedTerm term : extractedTerms) {
            candidateTermWriter.add(new AddCandidateTermCommand(
                    draftDictionary.getId(),
                    term.form(),
                    term.proposedDefinition(),
                    term.proposedEnglishName(),
                    term.occurredDocumentIds(),
                    term.occurrenceCount(),
                    term.contextSnippets(),
                    term.variantForms(),
                    extractionJob.getRequestedBy()));
        }
        extractionJob.succeed(draftDictionary.getId());
        draftDictionaryEventPublisher.publishCreated(draftDictionary);
        completionEventPublisher.publishCompleted(extractionJob);
        log.info(
                "[DraftDictionaryExtractionExecutionService.complete] Draft dictionary extraction completed. extractionJobId={}, draftDictionaryId={}",
                extractionJobId,
                draftDictionary.getId());
    }

    /**
     * 워커가 알려 온 실패를 기록한다. 종료된 작업에 늦게 도착한 실패는 무시한다 — 성공을 뒤집지 않는다.
     */
    @Transactional
    public void fail(Long extractionJobId, String requestId, String failureReason) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
        validateCallback(extractionJob, requestId);
        expire(extractionJob, failureReason);
    }

    /**
     * 콜백을 거치지 않는 실패 경로. 발행 자체가 실패했거나 타임아웃 스위퍼가 회수할 때 쓴다.
     *
     * <p>상관 식별자를 대조하지 않는다 — 호출자가 외부가 아니라 우리 코드다.
     */
    @Transactional
    public void expire(Long extractionJobId, String failureReason) {
        expire(extractionJobReader.read(extractionJobId), failureReason);
    }

    private void expire(ExtractionJob extractionJob, String failureReason) {
        if (!extractionJob.isInProgress()) return;
        extractionJob.fail(failureReason);
        log.warn(
                "[DraftDictionaryExtractionExecutionService.expire] Draft dictionary extraction failed. extractionJobId={}, reason={}",
                extractionJob.getId(),
                extractionJob.getFailureReason());
    }

    /**
     * 콜백을 보낸 쪽이 우리가 요청을 넘긴 그 워커인지 확인한다(D-70).
     *
     * <p>{@code /api/internal/**}은 인증 필터를 통과하므로 <b>이 대조가 유일한 방어선</b>이다.
     */
    private void validateCallback(ExtractionJob extractionJob, String requestId) {
        if (!extractionJob.matchesRequestId(requestId)) {
            log.warn(
                    "[DraftDictionaryExtractionExecutionService.validateCallback] Callback rejected. extractionJobId={}",
                    extractionJob.getId());
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_CALLBACK_FORBIDDEN);
        }
    }
}
