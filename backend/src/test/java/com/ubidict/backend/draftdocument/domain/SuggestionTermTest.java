package com.ubidict.backend.draftdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.domain.TextRange;
import org.junit.jupiter.api.Test;

class SuggestionTermTest {
    @Test
    void create() {
        assertThat(SuggestionTerm.create(1L, new TextRange(0, 1), "a", "b", 2L).getStatus())
                .isEqualTo(SuggestionTermStatus.PENDING);
    }
}
