package com.ubidict.backend.workspace.service.model;

import com.ubidict.backend.workspace.domain.Invitation;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.domain.Permission;
import java.time.OffsetDateTime;

public record InvitationResult(
        Long invitationId,
        Long workspaceId,
        String inviteeEmail,
        String token,
        Permission permission,
        InvitationStatus status,
        OffsetDateTime expiresAt) {

    public static InvitationResult from(Invitation invitation) {
        return new InvitationResult(
                invitation.getId(),
                invitation.getWorkspaceId(),
                invitation.getInviteeEmail(),
                invitation.getToken(),
                invitation.getPermission(),
                invitation.getStatus(),
                invitation.getExpiresAt());
    }
}
