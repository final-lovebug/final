package com.ubidict.backend.workspace.implement;

import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.infra.InvitationRepository;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class InvitationAppender {

    private static final int EXPIRATION_DAYS = 7;

    private final InvitationRepository invitationRepository;
    private final InvitationTokenGenerator tokenGenerator;

    public Invitation append(Long workspaceId, String inviteeEmail, Permission permission, Long createdBy) {
        Invitation invitation = Invitation.issue(
                workspaceId,
                inviteeEmail,
                tokenGenerator.generate(),
                permission,
                OffsetDateTime.now().plusDays(EXPIRATION_DAYS),
                createdBy);
        return invitationRepository.save(invitation);
    }
}
