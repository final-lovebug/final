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
}
