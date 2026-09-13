package com.ubidict.backend.workspace.service.model;

public record TransferOwnershipCommand(Long workspaceId, Long participantId, Long actorId) {}
