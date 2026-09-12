package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.IssueInvitationCommand;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record IssueInvitationRequest(
        @Email @Size(max = 320) String inviteeEmail,
        @NotNull Permission permission) {

    public IssueInvitationCommand toCommand(Long workspaceId, Long memberId) {
        return new IssueInvitationCommand(workspaceId, inviteeEmail, permission, memberId);
    }
}
