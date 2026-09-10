package com.ubidict.backend.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ParticipantIdTest {

    @DisplayName("값이 있으면 참여자 식별자를 만들 수 있다.")
    @Test
    void create() {
        // when
        ParticipantId participantId = new ParticipantId(1L);

        // then
        assertThat(participantId.value()).isEqualTo(1L);
    }

    @DisplayName("값이 null이면 참여자 식별자를 만들 수 없다.")
    @Test
    void create_valueIsNull() {
        // when & then
        assertThatThrownBy(() -> new ParticipantId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("값이 같으면 같은 참여자 식별자다.")
    @Test
    void equals_sameValue() {
        // when & then
        assertThat(new ParticipantId(1L)).isEqualTo(new ParticipantId(1L));
    }
}
