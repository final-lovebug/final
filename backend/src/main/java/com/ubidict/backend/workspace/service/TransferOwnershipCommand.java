package com.ubidict.backend.workspace.service;

public record TransferOwnershipCommand(Long workspaceId, Long participantId, Long actorId) {}
