package com.ubidict.backend.workspace.service.model;

public record RenameWorkspaceCommand(Long workspaceId, String name, Long memberId) {}
