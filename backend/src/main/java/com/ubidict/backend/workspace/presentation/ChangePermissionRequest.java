package com.ubidict.backend.workspace.presentation;

import com.ubidict.backend.workspace.domain.Permission;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;

public record ChangePermissionRequest(@NotNull Permission permission) {

    @AssertTrue(message = "권한 변경으로 OWNER를 부여할 수 없습니다.")
    public boolean isPermissionChangeAllowed() {
        return permission == null || permission != Permission.OWNER;
    }
}
