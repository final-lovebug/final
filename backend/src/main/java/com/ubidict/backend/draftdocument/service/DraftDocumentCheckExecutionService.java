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
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentCheckExecutionService {

    private final CheckJobReader checkJobReader;
    private final DraftDocumentWriter draftDocumentWriter;
    private final SuggestionTermWriter suggestionTermWriter;
    private final DraftDocumentCreationPolicyValidator creationPolicyValidator;
    private final CheckSuggestionValidator checkSuggestionValidator;
    private final DraftDocumentEventPublisher draftDocumentEventPublisher;
    private final CheckJobCompletionEventPublisher completionEventPublisher;

    @Transactional
    public boolean start(Long checkJobId) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        if (checkJob.getStatus() != CheckJobStatus.PENDING) {
            return false;
        }
        checkJob.start();
        log.info("[DraftDocumentCheckExecutionService.start] Draft document check started. checkJobId={}", checkJobId);
        return true;
    }

    @Transactional
    public void complete(Long checkJobId, DocumentSnapshot document, List<CheckSuggestion> suggestions) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        if (!checkJob.getDocumentId().equals(document.documentId())) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_RESULT);
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

    @Transactional
    public void fail(Long checkJobId, String failureReason) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        if (!checkJob.isInProgress()) {
            return;
        }
        checkJob.fail(failureReason);
        log.warn(
                "[DraftDocumentCheckExecutionService.fail] Draft document check failed. checkJobId={}, reason={}",
                checkJobId,
                checkJob.getFailureReason());
    }
}
