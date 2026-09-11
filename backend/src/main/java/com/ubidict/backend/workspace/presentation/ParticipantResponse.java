package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.ParticipantResult;
import java.time.OffsetDateTime;

public record ParticipantResponse(
        Long participantId, Long workspaceId, Long memberId, Permission permission, OffsetDateTime joinedAt) {
    public static ParticipantResponse from(ParticipantResult r) {
        return new ParticipantResponse(r.participantId(), r.workspaceId(), r.memberId(), r.permission(), r.joinedAt());
    }
}
