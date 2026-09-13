package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCheckCompletedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobCompletionEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishCompleted(CheckJob checkJob) {
        eventPublisher.publish(new DraftDocumentCheckCompletedEvent(
                checkJob.getId(), checkJob.getDocumentId(), checkJob.getDraftDocumentId(), OffsetDateTime.now()));
    }
}
