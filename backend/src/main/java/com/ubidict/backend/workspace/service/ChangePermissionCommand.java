package com.ubidict.backend.workspace.service;
import com.ubidict.backend.workspace.domain.Permission;
public record ChangePermissionCommand(Long workspaceId, Long memberId, Permission permission, Long actorId) {}
