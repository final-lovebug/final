package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.event.CandidateTermDecidedEvent;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryCreatedEvent;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryReviewRequestedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDictionaryEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishCreated(DraftDictionary draftDictionary) {
        eventPublisher.publish(new DraftDictionaryCreatedEvent(
                draftDictionary.getId(),
                draftDictionary.getWorkspaceId(),
                draftDictionary.getDictionaryId(),
                draftDictionary.getCreatedBy(),
                OffsetDateTime.now()));
    }

    public void publishReviewRequested(DraftDictionary draftDictionary, Long requesterId) {
        eventPublisher.publish(new DraftDictionaryReviewRequestedEvent(
                draftDictionary.getId(),
                draftDictionary.getWorkspaceId(),
                draftDictionary.getDictionaryId(),
                requesterId,
                OffsetDateTime.now()));
    }

    public void publishCandidateDecided(CandidateTerm candidateTerm) {
        eventPublisher.publish(new CandidateTermDecidedEvent(
                candidateTerm.getDraftDictionaryId(),
                candidateTerm.getId(),
                candidateTerm.getStatus(),
                candidateTerm.getHandledBy(),
                OffsetDateTime.now()));
    }
}
