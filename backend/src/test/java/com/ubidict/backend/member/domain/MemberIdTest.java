package com.ubidict.backend.member.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberIdTest {

    @DisplayName("값이 같은 MemberId는 서로 같다.")
    @Test
    void equals() {
        // given
        MemberId id1 = new MemberId(1L);
        MemberId id2 = new MemberId(1L);

        // when & then
        assertThat(id1).isEqualTo(id2);
    }

    @DisplayName("value가 null이면 MemberId를 생성할 수 없다.")
    @Test
    void constructor_valueIsNull() {
        // given & when & then
        assertThatThrownBy(() -> new MemberId(null)).isInstanceOf(IllegalArgumentException.class);
    }
}
