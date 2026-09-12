package com.ubidict.backend.draftdictionary.domain;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class CandidateTermTest {
    @Test
    void createStartsPending() {
        assertThat(CandidateTerm.create(1L, "term", null, null, List.of(), 1, List.of())
                        .getStatus())
                .isEqualTo(CandidateTermStatus.PENDING);
    }

    @Test
    void blankFormRejected() {
        assertThatThrownBy(() -> CandidateTerm.create(1L, " ", null, null, List.of(), 1, List.of()))
                .isInstanceOf(RuntimeException.class);
    }
}
