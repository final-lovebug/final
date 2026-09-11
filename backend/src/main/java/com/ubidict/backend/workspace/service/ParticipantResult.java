package com.ubidict.backend.workspace.service;

import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import java.time.OffsetDateTime;

public record ParticipantResult(
        Long participantId, Long workspaceId, Long memberId, Permission permission, OffsetDateTime joinedAt) {
    public static ParticipantResult from(Participant p) {
        return new ParticipantResult(
                p.getId(), p.getWorkspaceId(), p.getMemberId(), p.getPermission(), p.getJoinedAt());
    }
}
