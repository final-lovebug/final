package com.ubidict.backend.workspace.service.model;

import com.ubidict.backend.workspace.domain.Permission;

public record IssueInvitationCommand(Long workspaceId, String inviteeEmail, Permission permission, Long memberId) {}
