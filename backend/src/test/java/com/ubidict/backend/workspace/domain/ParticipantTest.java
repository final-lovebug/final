package com.ubidict.backend.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 2L;

    @DisplayName("소유자 참여자를 만들면 OWNER 권한을 가진다.")
    @Test
    void owner() {
        // when
        Participant participant = Participant.owner(WORKSPACE_ID, MEMBER_ID);

        // then
        assertThat(participant.getPermission()).isEqualTo(Permission.OWNER);
        assertThat(participant.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(participant.getMemberId()).isEqualTo(MEMBER_ID);
    }

    @DisplayName("소유자 참여자를 만들면 참여 시각이 채워진다.")
    @Test
    void owner_joinedAtIsSet() {
        // when
        Participant participant = Participant.owner(WORKSPACE_ID, MEMBER_ID);

        // then
        assertThat(participant.getJoinedAt()).isNotNull();
    }

    @DisplayName("워크스페이스를 만든 회원이 소유자 참여자의 생성자가 된다.")
    @Test
    void owner_createdByIsMember() {
        // when
        Participant participant = Participant.owner(WORKSPACE_ID, MEMBER_ID);

        // then
        assertThat(participant.getCreatedBy()).isEqualTo(MEMBER_ID);
    }

    @DisplayName("참여자의 권한을 변경할 수 있다.")
    @Test
    void changePermission_changesPermission() {
        Participant participant = Participant.owner(WORKSPACE_ID, MEMBER_ID);
        participant.changePermission(Permission.ADMIN);
        assertThat(participant.getPermission()).isEqualTo(Permission.ADMIN);
    }

    @DisplayName("같은 서열의 참여자는 서로 관리할 수 없다.")
    @Test
    void canBeManagedBy_sameRankIsRejected() {
        Participant admin = Participant.join(WORKSPACE_ID, MEMBER_ID, Permission.ADMIN, 3L);
        Participant owner = Participant.owner(WORKSPACE_ID, MEMBER_ID);

        assertThat(admin.canBeManagedBy(Permission.ADMIN)).isFalse();
        assertThat(owner.canBeManagedBy(Permission.OWNER)).isFalse();
    }

    @DisplayName("소유자는 관리자를 관리할 수 있다.")
    @Test
    void canBeManagedBy_ownerCanManageAdmin() {
        Participant participant = Participant.join(WORKSPACE_ID, MEMBER_ID, Permission.ADMIN, 3L);
        assertThat(participant.canBeManagedBy(Permission.OWNER)).isTrue();
    }

    @DisplayName("소유자를 관리자로 강등할 수 있다.")
    @Test
    void demoteToAdmin() {
        Participant participant = Participant.owner(WORKSPACE_ID, MEMBER_ID);
        participant.demoteToAdmin();
        assertThat(participant.getPermission()).isEqualTo(Permission.ADMIN);
    }

    @DisplayName("참여자를 소유자로 승격할 수 있다.")
    @Test
    void promoteToOwner() {
        Participant participant = Participant.join(WORKSPACE_ID, MEMBER_ID, Permission.ADMIN, 3L);
        participant.promoteToOwner();
        assertThat(participant.getPermission()).isEqualTo(Permission.OWNER);
    }
}
