package com.ubidict.backend.dictionary.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.exception.TermErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermTest {

    private static final Long DICTIONARY_ID = 1L;
    private static final Long CREATED_BY = 10L;
    private static final String DEFINITION = "서비스에 가입해 인증받는 주체";

    @DisplayName("용어를 만들면 표준어·영문명·정의가 설정된다.")
    @Test
    void create() {
        // when
        Term term = Term.create(DICTIONARY_ID, "회원", "Member", DEFINITION, CREATED_BY);

        // then
        assertThat(term.getDictionaryId()).isEqualTo(DICTIONARY_ID);
        assertThat(term.getPreferredForm()).isEqualTo("회원");
        assertThat(term.getEnglishName()).isEqualTo("Member");
        assertThat(term.getDefinition()).isEqualTo(DEFINITION);
        assertThat(term.getCreatedBy()).isEqualTo(CREATED_BY);
    }

    @DisplayName("표준어의 앞뒤 공백은 제거된다.")
    @Test
    void create_preferredFormIsTrimmed() {
        // when
        Term term = Term.create(DICTIONARY_ID, "  회원  ", null, DEFINITION, CREATED_BY);

        // then
        assertThat(term.getPreferredForm()).isEqualTo("회원");
    }

    @DisplayName("영문명은 없어도 용어를 만들 수 있다.")
    @Test
    void create_englishNameIsAbsent() {
        // when
        Term term = Term.create(DICTIONARY_ID, "회원", "  ", DEFINITION, CREATED_BY);

        // then
        assertThat(term.getEnglishName()).isNull();
    }

    @DisplayName("표준어가 100자라면 용어를 만들 수 있다.")
    @Test
    void create_preferredFormIsMaxLength() {
        // given
        String preferredForm = "가".repeat(100);

        // when
        Term term = Term.create(DICTIONARY_ID, preferredForm, null, DEFINITION, CREATED_BY);

        // then
        assertThat(term.getPreferredForm()).isEqualTo(preferredForm);
    }

    @DisplayName("표준어가 비어 있으면 용어를 만들 수 없다.")
    @Test
    void create_preferredFormIsBlank() {
        // when & then
        assertThatThrownBy(() -> Term.create(DICTIONARY_ID, "   ", null, DEFINITION, CREATED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_INVALID_PREFERRED_FORM);
    }

    @DisplayName("표준어가 100자를 넘으면 용어를 만들 수 없다.")
    @Test
    void create_preferredFormIsTooLong() {
        // given
        String preferredForm = "가".repeat(101);

        // when & then
        assertThatThrownBy(() -> Term.create(DICTIONARY_ID, preferredForm, null, DEFINITION, CREATED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_INVALID_PREFERRED_FORM);
    }

    @DisplayName("영문명이 100자를 넘으면 용어를 만들 수 없다.")
    @Test
    void create_englishNameIsTooLong() {
        // given
        String englishName = "a".repeat(101);

        // when & then
        assertThatThrownBy(() -> Term.create(DICTIONARY_ID, "회원", englishName, DEFINITION, CREATED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_INVALID_ENGLISH_NAME);
    }

    /**
     * 대조가 LLM 문맥 판단이라 정의가 유일한 판단 근거다. 비워 둘 수 없다.
     */
    @DisplayName("정의가 비어 있으면 용어를 만들 수 없다.")
    @Test
    void create_definitionIsBlank() {
        // when & then
        assertThatThrownBy(() -> Term.create(DICTIONARY_ID, "회원", "Member", "   ", CREATED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_INVALID_DEFINITION);
    }

    @DisplayName("표준어 비교는 대소문자를 구분한다.")
    @Test
    void normalize_isCaseSensitive() {
        // when & then
        assertThat(Term.normalize(" Login ")).isEqualTo("Login").isNotEqualTo("login");
    }
}
