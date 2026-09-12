package com.ubidict.backend.dictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermIdTest {

    @DisplayName("값이 있으면 용어 식별자를 만들 수 있다.")
    @Test
    void create() {
        // when
        TermId termId = new TermId(1L);

        // then
        assertThat(termId.value()).isEqualTo(1L);
    }

    @DisplayName("값이 null이면 용어 식별자를 만들 수 없다.")
    @Test
    void create_valueIsNull() {
        // when & then
        assertThatThrownBy(() -> new TermId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("값이 같으면 같은 용어 식별자다.")
    @Test
    void equals_sameValue() {
        // when & then
        assertThat(new TermId(1L)).isEqualTo(new TermId(1L));
    }
}
