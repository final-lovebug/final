package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import org.junit.jupiter.api.Test;

class SuggestionTermOwnershipValidatorTest {
    @Test
    void validate_mismatched() {
        var t = SuggestionTerm.create(1L, new TextRange(0, 1), "a", "b", 1L);
        assertThatThrownBy(() -> new SuggestionTermOwnershipValidator().validate(t, 2L))
                .isInstanceOf(RuntimeException.class);
    }
}
