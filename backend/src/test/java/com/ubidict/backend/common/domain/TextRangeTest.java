package com.ubidict.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TextRangeTest {
    @Test
    void create_endBeforeStart() {
        assertThatThrownBy(() -> new TextRange(2, 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_negativeOffset() {
        assertThatThrownBy(() -> new TextRange(-1, 1)).isInstanceOf(IllegalArgumentException.class);
    }
}
