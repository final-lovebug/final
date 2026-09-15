package com.ubidict.backend.dictionary.implement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.exception.TermErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TermFormValidatorTest {

    private static final String DEFINITION = "정의";

    private final TermFormValidator termFormValidator = new TermFormValidator();

    @DisplayName("표준어가 서로 다르면 통과한다.")
    @Test
    void validateUnique() {
        // given
        List<NewTerm> newTerms = List.of(newTerm("회원"), newTerm("문서"), newTerm("사전집"));

        // when & then
        assertThatCode(() -> termFormValidator.validateUnique(newTerms)).doesNotThrowAnyException();
    }

    @DisplayName("같은 표준어가 둘이면 등재할 수 없다.")
    @Test
    void validateUnique_preferredFormIsDuplicated() {
        // given
        List<NewTerm> newTerms = List.of(newTerm("회원"), newTerm("회원"));

        // when & then
        assertThatThrownBy(() -> termFormValidator.validateUnique(newTerms))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_DUPLICATE_PREFERRED_FORM);
    }

    @DisplayName("앞뒤 공백만 다른 표준어는 같은 것으로 본다.")
    @Test
    void validateUnique_preferredFormDiffersOnlyByWhitespace() {
        // given
        List<NewTerm> newTerms = List.of(newTerm("회원"), newTerm("  회원  "));

        // when & then
        assertThatThrownBy(() -> termFormValidator.validateUnique(newTerms))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(TermErrorCode.TERM_DUPLICATE_PREFERRED_FORM);
    }

    @DisplayName("대소문자가 다른 표준어는 다른 것으로 본다.")
    @Test
    void validateUnique_preferredFormIsCaseSensitive() {
        // given
        List<NewTerm> newTerms = List.of(newTerm("Login"), newTerm("login"));

        // when & then
        assertThatCode(() -> termFormValidator.validateUnique(newTerms)).doesNotThrowAnyException();
    }

    /**
     * 비어 있는 표준어는 중복이 아니라 값 자체가 잘못된 것이므로 Term이 제 이유로 실패해야 한다.
     */
    @DisplayName("비어 있는 표준어가 여럿이어도 중복으로 보지 않는다.")
    @Test
    void validateUnique_preferredFormIsBlank() {
        // given
        List<NewTerm> newTerms = List.of(newTerm("  "), newTerm(""));

        // when & then
        assertThatCode(() -> termFormValidator.validateUnique(newTerms)).doesNotThrowAnyException();
    }

    private NewTerm newTerm(String preferredForm) {
        return new NewTerm(preferredForm, null, DEFINITION);
    }
}
