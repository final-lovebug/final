package com.ubidict.backend.workspace.service;
public record TransferOwnershipCommand(Long workspaceId, Long targetMemberId, Long actorId) {}
