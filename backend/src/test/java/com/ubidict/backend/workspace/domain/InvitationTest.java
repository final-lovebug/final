package com.ubidict.backend.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.InvitationErrorCode;
import com.ubidict.backend.workspace.fixture.InvitationFixture;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvitationTest {

    @DisplayName("초대로는 소유자 권한을 부여할 수 없다.")
    @Test
    void issue_ownerPermissionIsRejected() {
        assertThatThrownBy(() -> Invitation.issue(
                        1L,
                        null,
                        "token",
                        Permission.OWNER,
                        OffsetDateTime.now().plusHours(1),
                        2L))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_OWNER_NOT_ALLOWED);
    }

    @DisplayName("만료된 초대는 수락할 수 없고 만료 상태가 된다.")
    @Test
    void accept_expired() {
        Invitation invitation = InvitationFixture.invitation()
                .expiresAt(OffsetDateTime.now().minusSeconds(1))
                .build();

        assertThatThrownBy(() -> invitation.accept(3L, OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }

    @DisplayName("이미 수락한 초대는 다시 수락할 수 없다.")
    @Test
    void accept_alreadyAccepted() {
        Invitation invitation = InvitationFixture.invitation().build();
        invitation.accept(3L, OffsetDateTime.now());

        assertThatThrownBy(() -> invitation.accept(4L, OffsetDateTime.now()))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
    }

    @DisplayName("대기 상태인 초대만 취소할 수 있다.")
    @Test
    void cancel_pendingOnly() {
        Invitation invitation = InvitationFixture.invitation().build();
        invitation.cancel();

        assertThatThrownBy(invitation::cancel)
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(InvitationErrorCode.INVITATION_NOT_ACCEPTABLE);
    }

    @DisplayName("만료 시각과 현재 시각이 같으면 수락할 수 없다.")
    @Test
    void isAcceptable_boundary() {
        Invitation invitation = InvitationFixture.invitation().build();

        assertThat(invitation.isAcceptable(invitation.getExpiresAt())).isFalse();
        assertThat(invitation.getStatus()).isEqualTo(InvitationStatus.EXPIRED);
    }
}
