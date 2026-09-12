package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.*;

import com.ubidict.backend.draftdictionary.domain.*;
import java.util.*;
import org.junit.jupiter.api.Test;

class CandidateTermOwnershipValidatorTest {
    @Test
    void mismatchedRejected() {
        var t = CandidateTerm.create(1L, "x", null, null, List.of(), 1, List.of());
        assertThatThrownBy(() -> new CandidateTermOwnershipValidator().validate(t, 2L))
                .isInstanceOf(RuntimeException.class);
    }
}
