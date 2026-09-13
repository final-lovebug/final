package com.ubidict.backend.workspace.service.model;

public record RemoveParticipantCommand(Long workspaceId, Long participantId, Long actorId) {}
