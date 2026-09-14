package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.implement.CheckJobCompletionEventPublisher;
import com.ubidict.backend.draftdocument.implement.CheckJobReader;
import com.ubidict.backend.draftdocument.implement.CheckSuggestionValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentCreationPolicyValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentEventPublisher;
import com.ubidict.backend.draftdocument.implement.DraftDocumentWriter;
import com.ubidict.backend.draftdocument.implement.SuggestionTermWriter;
import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentCheckExecutionService {

    private final CheckJobReader checkJobReader;
    private final DocumentQueryPort documentQueryPort;
    private final DraftDocumentWriter draftDocumentWriter;
    private final SuggestionTermWriter suggestionTermWriter;
    private final DraftDocumentCreationPolicyValidator creationPolicyValidator;
    private final CheckSuggestionValidator checkSuggestionValidator;
    private final DraftDocumentEventPublisher draftDocumentEventPublisher;
    private final CheckJobCompletionEventPublisher completionEventPublisher;

    /**
     * 워커에게 넘기기 직전에 작업을 {@code RUNNING}으로 옮기고 상관 식별자를 기록한다.
     *
     * <p>{@code PENDING}이 아니면 {@code Optional.empty()}다 — 발행 신호가 중복으로 도착해도 두 번 보내지 않는다.
     */
    @Transactional
    public Optional<CheckJobResult> markDispatching(Long checkJobId, String requestId) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        if (checkJob.getStatus() != CheckJobStatus.PENDING) {
            return Optional.empty();
        }
        checkJob.markDispatching(requestId);
        log.info(
                "[DraftDocumentCheckExecutionService.markDispatching] Draft document check dispatched. checkJobId={}, requestId={}",
                checkJobId,
                requestId);
        return Optional.of(CheckJobResult.from(checkJob));
    }

    /**
     * 워커가 돌려준 제안어를 초안으로 굳힌다.
     *
     * <p><b>본문을 콜백에서 받지 않고 여기서 다시 읽는다</b> — 컨트롤러가 {@link DocumentSnapshot}을 조립하면 계층이 뒤집히고, 무엇보다 제안어
     * 앵커의 기준이 되는 본문은 우리가 읽은 것이어야 한다. 워커가 본 버전({@code documentVersionNo})과 지금 본문의 버전이 다르면 앵커 오프셋이 이미
     * 무효이므로, {@link CheckSuggestionValidator}가 터지기 전에 분명한 사유로 작업을 끝낸다.
     *
     * <p>이미 끝난 작업이면 아무 일도 하지 않는다(D-72) — SQS는 at-least-once라 같은 결과가 두 번 올 수 있다.
     */
    @Transactional
    public void complete(Long checkJobId, String requestId, int documentVersionNo, List<CheckSuggestion> suggestions) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        validateCallback(checkJob, requestId);
        if (!checkJob.isInProgress()) {
            log.info(
                    "[DraftDocumentCheckExecutionService.complete] Callback for terminal job ignored. checkJobId={}, status={}",
                    checkJobId,
                    checkJob.getStatus());
            return;
        }

        DocumentSnapshot document = documentQueryPort
                .read(checkJob.getDocumentId())
                .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND));
        if (document.currentVersionNo() != documentVersionNo) {
            log.warn(
                    "[DraftDocumentCheckExecutionService.complete] Document changed while checking. checkJobId={}, checkedVersionNo={}, currentVersionNo={}",
                    checkJobId,
                    documentVersionNo,
                    document.currentVersionNo());
            checkJob.fail("대조하는 동안 문서가 새 버전으로 바뀌었습니다.");
            return;
        }
        creationPolicyValidator.validate(document.documentId(), document.workspaceId());
        checkSuggestionValidator.validate(document.body(), suggestions);

        DraftDocument draftDocument = draftDocumentWriter.append(
                document.documentId(),
                document.currentVersionNo(),
                document.body(),
                checkJob.getRequestedBy(),
                checkJob.getRequestedBy());
        for (CheckSuggestion suggestion : suggestions) {
            suggestionTermWriter.add(
                    draftDocument.getId(),
                    suggestion.anchor(),
                    suggestion.originTerm(),
                    suggestion.suggestionTerm(),
                    checkJob.getRequestedBy());
        }
        checkJob.succeed(draftDocument.getId());
        draftDocumentEventPublisher.publishCreated(draftDocument);
        completionEventPublisher.publishCompleted(checkJob);
        log.info(
                "[DraftDocumentCheckExecutionService.complete] Draft document check completed. checkJobId={}, draftDocumentId={}",
                checkJobId,
                draftDocument.getId());
    }

    /**
     * 워커가 알려 온 실패를 기록한다. 종료된 작업에 늦게 도착한 실패는 무시한다 — 성공을 뒤집지 않는다.
     */
    @Transactional
    public void fail(Long checkJobId, String requestId, String failureReason) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        validateCallback(checkJob, requestId);
        expire(checkJob, failureReason);
    }

    /**
     * 콜백을 거치지 않는 실패 경로. 발행 자체가 실패했거나 타임아웃 스위퍼가 회수할 때 쓴다.
     *
     * <p>상관 식별자를 대조하지 않는다 — 호출자가 외부가 아니라 우리 코드다.
     */
    @Transactional
    public void expire(Long checkJobId, String failureReason) {
        expire(checkJobReader.read(checkJobId), failureReason);
    }

    private void expire(CheckJob checkJob, String failureReason) {
        if (!checkJob.isInProgress()) {
            return;
        }
        checkJob.fail(failureReason);
        log.warn(
                "[DraftDocumentCheckExecutionService.expire] Draft document check failed. checkJobId={}, reason={}",
                checkJob.getId(),
                checkJob.getFailureReason());
    }

    /**
     * 콜백을 보낸 쪽이 우리가 요청을 넘긴 그 워커인지 확인한다(D-70).
     *
     * <p>{@code /api/internal/**}은 인증 필터를 통과하므로 <b>이 대조가 유일한 방어선</b>이다.
     */
    private void validateCallback(CheckJob checkJob, String requestId) {
        if (!checkJob.matchesRequestId(requestId)) {
            log.warn(
                    "[DraftDocumentCheckExecutionService.validateCallback] Callback rejected. checkJobId={}",
                    checkJob.getId());
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN);
        }
    }
}
