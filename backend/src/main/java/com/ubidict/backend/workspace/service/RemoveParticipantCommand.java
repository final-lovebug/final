package com.ubidict.backend.workspace.service;

public record RemoveParticipantCommand(Long workspaceId, Long participantId, Long actorId) {}
