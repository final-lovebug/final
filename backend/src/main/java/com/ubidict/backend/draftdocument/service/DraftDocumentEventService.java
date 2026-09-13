package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.implement.DraftDocumentReader;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentEventService {

    private final DraftDocumentReader draftDocumentReader;

    @Transactional
    public void markReviewRequested(Long draftDocumentId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        draftDocument.markReviewRequested();
        log.info(
                "[DraftDocumentEventService.markReviewRequested] Draft document review requested. draftDocumentId={}",
                draftDocumentId);
    }

    @Transactional
    public void reopen(Long draftDocumentId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        draftDocument.reopen();
        log.info("[DraftDocumentEventService.reopen] Draft document reopened. draftDocumentId={}", draftDocumentId);
    }

    @Transactional
    public void markRevised(Long draftDocumentId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        draftDocument.markRevised();
        log.info("[DraftDocumentEventService.markRevised] Draft document revised. draftDocumentId={}", draftDocumentId);
    }
}
