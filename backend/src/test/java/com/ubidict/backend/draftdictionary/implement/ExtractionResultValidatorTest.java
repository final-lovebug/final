package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExtractionResultValidatorTest {

    private final ExtractionResultValidator validator = new ExtractionResultValidator();

    @DisplayName("추출 결과의 출처 문서가 작업 대상에 없으면 거절한다.")
    @Test
    void validate_unknownSourceDocument() {
        ExtractedTerm term = new ExtractedTerm("결제", "정의", null, List.of(20L), 1, List.of("문맥"));

        assertThatThrownBy(() -> validator.validate(List.of(10L), List.of(term)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_RESULT));
    }

    @DisplayName("추출 결과에 같은 표기의 후보어가 중복되면 거절한다.")
    @Test
    void validate_duplicateForm() {
        ExtractedTerm term = new ExtractedTerm("결제", "정의", null, List.of(10L), 1, List.of("문맥"));

        assertThatThrownBy(() -> validator.validate(List.of(10L), List.of(term, term)))
                .isInstanceOf(BusinessException.class);
    }
}
