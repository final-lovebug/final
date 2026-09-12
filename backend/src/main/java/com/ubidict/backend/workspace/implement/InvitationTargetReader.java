package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.infra.port.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationTargetReader {

    private final MemberQueryPort memberQueryPort;
    private final ParticipantReader participantReader;

    public boolean isParticipant(Long workspaceId, String inviteeEmail) {
        if (inviteeEmail == null) {
            return false;
        }
        return memberQueryPort
                .findActiveMemberIdByEmail(inviteeEmail)
                .flatMap(memberId -> participantReader.readOptional(workspaceId, memberId))
                .isPresent();
    }
}
