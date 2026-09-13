package com.ubidict.backend.draftdocument.infra.event;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCheckRequestedEvent;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.infra.port.TermCheckerPort;
import com.ubidict.backend.draftdocument.infra.port.TermSnapshot;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckExecutionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DraftDocumentCheckEventListener {

    private final DraftDocumentCheckExecutionService executionService;
    private final DocumentQueryPort documentQueryPort;
    private final DictionaryTermQueryPort dictionaryTermQueryPort;
    private final TermCheckerPort termCheckerPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(DraftDocumentCheckRequestedEvent event) {
        if (!executionService.start(event.checkJobId())) {
            return;
        }
        try {
            DocumentSnapshot document = documentQueryPort
                    .read(event.documentId())
                    .orElseThrow(() -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DOCUMENT_NOT_FOUND));
            if (!dictionaryTermQueryPort.hasActiveDictionary(document.workspaceId())) {
                throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DICTIONARY_NOT_FOUND);
            }
            List<TermSnapshot> activeTerms = dictionaryTermQueryPort.readActiveTerms(document.workspaceId());
            executionService.complete(event.checkJobId(), document, termCheckerPort.check(document, activeTerms));
        } catch (Exception exception) {
            log.error(
                    "[DraftDocumentCheckEventListener.onRequested] Failed to execute draft document check. checkJobId={}",
                    event.checkJobId(),
                    exception);
            executionService.fail(event.checkJobId(), failureReason(exception));
        }
    }

    private String failureReason(Exception exception) {
        return exception instanceof BusinessException businessException
                ? businessException.errorCode().message()
                : "문서 대조 작업 처리에 실패했습니다.";
    }
}
