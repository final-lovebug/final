package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CheckSuggestionValidatorTest {

    private final CheckSuggestionValidator validator = new CheckSuggestionValidator();

    @DisplayName("대조 결과의 원문과 본문 위치가 일치하면 검증을 통과한다.")
    @Test
    void validate() {
        validator.validate("회원은 결제한다.", List.of(new CheckSuggestion(new TextRange(0, 2), "회원", "사용자")));
    }

    @DisplayName("대조 결과의 원문과 본문 위치가 다르면 작업 결과를 거절한다.")
    @Test
    void validate_originDoesNotMatch() {
        assertThatThrownBy(() ->
                        validator.validate("회원은 결제한다.", List.of(new CheckSuggestion(new TextRange(0, 2), "고객", "사용자"))))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_RESULT));
    }
}
