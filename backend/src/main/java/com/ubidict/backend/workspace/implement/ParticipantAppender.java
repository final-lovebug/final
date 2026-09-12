package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ParticipantAppender {

    private final ParticipantRepository participantRepository;

    public Participant appendOwner(Long workspaceId, Long memberId) {
        return participantRepository.save(Participant.owner(workspaceId, memberId));
    }

    public Participant appendMember(Long workspaceId, Long memberId, Permission permission, Long invitedBy) {
        if (participantRepository
                .findByWorkspaceIdAndMemberId(workspaceId, memberId)
                .isPresent()) {
            participantRepository.restore(
                    workspaceId, memberId, permission, OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS), invitedBy);
            return participantRepository
                    .findByWorkspaceIdAndMemberIdAndDeletedAtIsNull(workspaceId, memberId)
                    .orElseThrow();
        }
        return participantRepository.save(Participant.join(workspaceId, memberId, permission, invitedBy));
    }
}
