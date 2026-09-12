package com.ubidict.backend.document.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.LabelErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LabelTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 7L;

    @DisplayName("라벨 이름의 앞뒤 공백을 제거한다.")
    @Test
    void create_nameIsTrimmed() {
        // when
        Label label = Label.create(WORKSPACE_ID, "  설계  ", MEMBER_ID);

        // then
        assertThat(label.getName()).isEqualTo("설계");
    }

    @DisplayName("라벨 이름이 비어 있으면 예외가 발생한다.")
    @Test
    void create_nameIsBlank() {
        // when & then
        assertThatThrownBy(() -> Label.create(WORKSPACE_ID, "   ", MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(LabelErrorCode.LABEL_INVALID_NAME);
    }

    @DisplayName("라벨 이름이 20자를 넘으면 예외가 발생한다.")
    @Test
    void create_nameIsTooLong() {
        // given
        String tooLong = "가".repeat(Label.NAME_MAX_LENGTH + 1);

        // when & then
        assertThatThrownBy(() -> Label.create(WORKSPACE_ID, tooLong, MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(LabelErrorCode.LABEL_INVALID_NAME);
    }

    @DisplayName("대소문자가 다르면 다른 라벨로 본다.")
    @Test
    void normalizeName_isCaseSensitive() {
        // when
        String upper = Label.normalizeName("Design");
        String lower = Label.normalizeName("design");

        // then
        assertThat(upper).isNotEqualTo(lower);
    }

    @DisplayName("공백만 다른 이름은 같은 라벨로 본다.")
    @Test
    void normalizeName_ignoresSurroundingWhitespace() {
        // when & then
        assertThat(Label.normalizeName(" 설계 ")).isEqualTo(Label.normalizeName("설계"));
    }
}
