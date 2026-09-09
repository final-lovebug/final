package com.ubidict.backend.workspace.service;

public record RenameWorkspaceCommand(Long workspaceId, String name, Long memberId) {}
