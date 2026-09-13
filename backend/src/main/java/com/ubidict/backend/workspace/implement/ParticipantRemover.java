package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.event.ParticipantRemovedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantRemover {

    private final EventPublisher eventPublisher;

    public void remove(Participant participant) {
        participant.delete();
        eventPublisher.publish(new ParticipantRemovedEvent(
                participant.getWorkspaceId(), participant.getMemberId(), OffsetDateTime.now()));
    }
}
