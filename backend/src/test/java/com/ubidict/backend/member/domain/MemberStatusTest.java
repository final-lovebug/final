package com.ubidict.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberStatusTest {

    @DisplayName("ACTIVE 상태의 회원은 로그인할 수 있다.")
    @Test
    void canLogin() {
        // given
        MemberStatus status = MemberStatus.ACTIVE;

        // when & then
        assertThat(status.canLogin()).isTrue();
    }

    @DisplayName("ACTIVE가 아닌 상태의 회원은 로그인할 수 없다.")
    @Test
    void canLogin_notActive() {
        // given & when & then
        assertThat(MemberStatus.PENDING.canLogin()).isFalse();
        assertThat(MemberStatus.SUSPENDED.canLogin()).isFalse();
        assertThat(MemberStatus.WITHDRAWN.canLogin()).isFalse();
    }
}
