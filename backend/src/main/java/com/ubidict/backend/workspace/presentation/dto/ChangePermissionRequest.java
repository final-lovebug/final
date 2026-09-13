package com.ubidict.backend.workspace.presentation.dto;

import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.model.ChangePermissionCommand;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ChangePermissionRequest(@NotNull Permission permission) {

    public ChangePermissionCommand toCommand(Long workspaceId, Long participantId, Long memberId) {
        return new ChangePermissionCommand(workspaceId, participantId, permission, memberId);
    }

    @AssertTrue(message = "권한 변경으로 OWNER를 부여할 수 없습니다.")
    public boolean isPermissionChangeAllowed() {
        return permission == null || permission != Permission.OWNER;
    }
}
