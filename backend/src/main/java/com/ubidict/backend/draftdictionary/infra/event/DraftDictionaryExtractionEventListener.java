package com.ubidict.backend.draftdictionary.infra.event;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionRequestedEvent;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import com.ubidict.backend.draftdictionary.infra.port.TermExtractorPort;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionExecutionService;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
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
public class DraftDictionaryExtractionEventListener {

    private final DraftDictionaryExtractionExecutionService executionService;
    private final DocumentQueryPort documentQueryPort;
    private final DictionaryTermQueryPort dictionaryTermQueryPort;
    private final TermExtractorPort termExtractorPort;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRequested(DraftDictionaryExtractionRequestedEvent event) {
        var started = executionService.start(event.extractionJobId());
        if (started.isEmpty()) return;

        ExtractionJobResult job = started.get();
        try {
            List<Long> sourceDocumentIds = job.sourceDocumentIds().stream()
                    .filter(documentQueryPort::isExtractable)
                    .toList();
            if (sourceDocumentIds.size() != job.sourceDocumentIds().size()) {
                throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_EXTRACTABLE_DOCUMENT);
            }
            List<TermSnapshot> activeTerms = dictionaryTermQueryPort.readActiveTerms(job.workspaceId());
            List<ExtractedTerm> extractedTerms = termExtractorPort.extract(sourceDocumentIds, activeTerms);
            executionService.complete(event.extractionJobId(), sourceDocumentIds, extractedTerms);
        } catch (Exception exception) {
            log.error(
                    "[DraftDictionaryExtractionEventListener.onRequested] Failed to execute draft dictionary extraction. extractionJobId={}",
                    event.extractionJobId(),
                    exception);
            executionService.fail(event.extractionJobId(), failureReason(exception));
        }
    }

    private String failureReason(Exception exception) {
        return exception instanceof BusinessException businessException
                ? businessException.errorCode().message()
                : "용어 추출 작업 처리에 실패했습니다.";
    }
}
