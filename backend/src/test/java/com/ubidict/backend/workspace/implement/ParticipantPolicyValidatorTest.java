package com.ubidict.backend.workspace.implement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantPolicyValidatorTest {
    private final ParticipantPolicyValidator validator = new ParticipantPolicyValidator();

    @DisplayName("관리자는 같은 서열의 관리자를 내보낼 수 없다.")
    @Test
    void validateRemovable_adminCannotRemoveAdmin() {
        Participant target = Participant.join(1L, 2L, Permission.ADMIN, 1L);
        assertThatThrownBy(() -> validator.validateRemovable(target, Permission.ADMIN))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_OWNER_REQUIRED);
    }

    @DisplayName("소유자는 내보낼 수 없다.")
    @Test
    void validateRemovable_ownerIsNotRemovable() {
        Participant target = Participant.owner(1L, 2L);
        assertThatThrownBy(() -> validator.validateRemovable(target, Permission.OWNER))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_OWNER_CANNOT_BE_REMOVED);
    }

    @DisplayName("관리자는 일반 참여자를 내보낼 수 있다.")
    @Test
    void validateRemovable_adminCanRemoveRegular() {
        Participant target = Participant.join(1L, 2L, Permission.REGULAR, 1L);
        assertThatCode(() -> validator.validateRemovable(target, Permission.ADMIN))
                .doesNotThrowAnyException();
    }
}
