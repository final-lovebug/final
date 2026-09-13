package com.ubidict.backend.draftdictionary.infra.event;

import com.ubidict.backend.draftdictionary.service.DraftDictionaryEventService;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DraftDictionaryReviewEventListener {

    private final DraftDictionaryEventService draftDictionaryEventService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCreated(ReviewRequestCreatedEvent event) {
        if (event.type() == ReviewRequestType.DICTIONARY) {
            draftDictionaryEventService.markReviewRequested(event.targetDraftId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCanceled(ReviewRequestCanceledEvent event) {
        if (event.type() == ReviewRequestType.DICTIONARY) {
            draftDictionaryEventService.reopen(event.targetDraftId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onChangesRequested(ReviewRequestChangesRequestedEvent event) {
        if (event.type() == ReviewRequestType.DICTIONARY) {
            draftDictionaryEventService.reopenForRedecision(event.targetDraftId());
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onRevised(ReviewRequestRevisedEvent event) {
        if (event.type() == ReviewRequestType.DICTIONARY) {
            draftDictionaryEventService.markRevised(event.targetDraftId());
        }
    }
}
