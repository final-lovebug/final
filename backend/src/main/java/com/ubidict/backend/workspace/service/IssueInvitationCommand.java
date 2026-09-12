package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Permission;

public record IssueInvitationCommand(Long workspaceId, String inviteeEmail, Permission permission, Long memberId) {}
