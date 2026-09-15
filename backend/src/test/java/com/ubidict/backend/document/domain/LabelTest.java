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

    /**
     * D-94. 표시명은 최초 입력 표기를 그대로 남긴다 — 접는 것은 비교 키뿐이다.
     */
    @DisplayName("라벨 이름의 대소문자는 표시명에 그대로 남는다.")
    @Test
    void create_nameKeepsLetterCase() {
        // when
        Label label = Label.create(WORKSPACE_ID, "Design", MEMBER_ID);

        // then
        assertThat(label.getName()).isEqualTo("Design");
    }

    @DisplayName("대소문자만 다른 이름은 같은 라벨로 본다.")
    @Test
    void matchKey_ignoresLetterCase() {
        // when & then
        assertThat(Label.matchKey("Design")).isEqualTo(Label.matchKey("design"));
        assertThat(Label.matchKey("API")).isEqualTo(Label.matchKey("api"));
    }

    /**
     * label.name의 collation(utf8mb4_0900_ai_ci)이 유니코드 정규화를 거쳐 비교하므로 비교 키도 그 기준에 맞춘다.
     * macOS에서 복사한 한글이 조합형(NFD)으로 들어오는 경우가 실제로 있다.
     */
    @DisplayName("조합형과 완성형 한글은 같은 라벨로 본다.")
    @Test
    void matchKey_normalizesHangul() {
        // given
        String composed = "설계";
        String decomposed = java.text.Normalizer.normalize(composed, java.text.Normalizer.Form.NFD);

        // when & then
        assertThat(decomposed).isNotEqualTo(composed);
        assertThat(Label.matchKey(decomposed)).isEqualTo(Label.matchKey(composed));
    }

    @DisplayName("비교 키도 앞뒤 공백을 무시한다.")
    @Test
    void matchKey_ignoresSurroundingWhitespace() {
        // when & then
        assertThat(Label.matchKey(" Design ")).isEqualTo(Label.matchKey("design"));
    }

    @DisplayName("공백만 다른 이름은 같은 라벨로 본다.")
    @Test
    void normalizeName_ignoresSurroundingWhitespace() {
        // when & then
        assertThat(Label.normalizeName(" 설계 ")).isEqualTo(Label.normalizeName("설계"));
    }
}
