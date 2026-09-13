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

    @Transactional
    public Optional<ExtractionJobResult> start(Long extractionJobId) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
        if (extractionJob.getStatus() != ExtractionJobStatus.PENDING) {
            return Optional.empty();
        }
        extractionJob.start();
        log.info(
                "[DraftDictionaryExtractionExecutionService.start] Draft dictionary extraction started. extractionJobId={}",
                extractionJobId);
        return Optional.of(ExtractionJobResult.from(extractionJob));
    }

    @Transactional
    public void complete(Long extractionJobId, List<Long> sourceDocumentIds, List<ExtractedTerm> extractedTerms) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
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

    @Transactional
    public void fail(Long extractionJobId, String failureReason) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
        if (!extractionJob.isInProgress()) return;
        extractionJob.fail(failureReason);
        log.warn(
                "[DraftDictionaryExtractionExecutionService.fail] Draft dictionary extraction failed. extractionJobId={}, reason={}",
                extractionJobId,
                extractionJob.getFailureReason());
    }
}
