package com.ubidict.backend.workspace.service.model;

import com.ubidict.backend.workspace.domain.Permission;

public record ChangePermissionCommand(Long workspaceId, Long participantId, Permission permission, Long actorId) {}
