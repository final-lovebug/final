package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantUpdater {
    private final ParticipantRepository participantRepository;

    public Participant changePermission(Participant participant, Permission permission) {
        participant.changePermission(permission);
        return participantRepository.save(participant);
    }

    public void transferOwnership(Participant owner, Participant target) {
        owner.demoteToAdmin();
        target.promoteToOwner();
        participantRepository.save(owner);
        participantRepository.save(target);
    }
}
