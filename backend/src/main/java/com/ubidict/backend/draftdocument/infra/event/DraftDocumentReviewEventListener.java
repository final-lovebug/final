package com.ubidict.backend.draftdocument.infra.event;

import com.ubidict.backend.draftdocument.service.DraftDocumentEventService;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DraftDocumentReviewEventListener {

    private final DraftDocumentEventService draftDocumentEventService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(ReviewRequestCreatedEvent event) {
        if (event.type() == ReviewRequestType.DOCUMENT) {
            draftDocumentEventService.markReviewRequested(event.targetDraftId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCanceled(ReviewRequestCanceledEvent event) {
        if (event.type() == ReviewRequestType.DOCUMENT) {
            draftDocumentEventService.reopen(event.targetDraftId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRevised(ReviewRequestRevisedEvent event) {
        if (event.type() == ReviewRequestType.DOCUMENT) {
            draftDocumentEventService.markRevised(event.targetDraftId());
        }
    }
}
