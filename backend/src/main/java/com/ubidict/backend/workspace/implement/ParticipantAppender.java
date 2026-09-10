package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantAppender {

    private final ParticipantRepository participantRepository;

    public Participant appendOwner(Long workspaceId, Long memberId) {
        return participantRepository.save(Participant.owner(workspaceId, memberId));
    }
}
