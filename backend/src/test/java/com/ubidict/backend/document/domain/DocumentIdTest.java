package com.ubidict.backend.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DocumentIdTest {

    @DisplayName("식별자 값이 null이면 예외가 발생한다.")
    @Test
    void create_valueIsNull() {
        // when & then
        assertThatThrownBy(() -> new DocumentId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentVersionId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LabelId(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new DocumentLabelId(null)).isInstanceOf(IllegalArgumentException.class);
    }

    @DisplayName("같은 값을 가진 식별자는 서로 같다.")
    @Test
    void equals_sameValue() {
        // when & then
        assertThat(new DocumentId(1L)).isEqualTo(new DocumentId(1L));
    }
}
