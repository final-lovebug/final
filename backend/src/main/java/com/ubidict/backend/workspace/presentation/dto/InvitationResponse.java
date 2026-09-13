package com.ubidict.backend.workspace.presentation.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.model.InvitationResult;
import java.time.OffsetDateTime;

public record InvitationResponse(
        Long invitationId,
        Long workspaceId,
        String inviteeEmail,
        @JsonInclude(JsonInclude.Include.NON_NULL) String token,
        Permission permission,
        InvitationStatus status,
        OffsetDateTime expiresAt) {

    public static InvitationResponse issued(InvitationResult result) {
        return from(result, result.token());
    }

    public static InvitationResponse listed(InvitationResult result) {
        return from(result, null);
    }

    private static InvitationResponse from(InvitationResult result, String token) {
        return new InvitationResponse(
                result.invitationId(),
                result.workspaceId(),
                result.inviteeEmail(),
                token,
                result.permission(),
                result.status(),
                result.expiresAt());
    }
}
