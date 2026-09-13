package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.event.DraftDocumentCheckRequestedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobEventPublisher {

    private final EventPublisher eventPublisher;

    public void publishRequested(CheckJob checkJob) {
        eventPublisher.publish(new DraftDocumentCheckRequestedEvent(
                checkJob.getId(), checkJob.getDocumentId(), checkJob.getRequestedBy(), OffsetDateTime.now()));
    }
}
