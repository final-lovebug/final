package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RevisionDictionaryTest {
    @Test
    void firstRevisionAllowsNullDictionary() {
        var r = RevisionDictionary.create(1L, null, 0, 2L, 3L);
        assertThat(r.getDictionaryId()).isNull();
        assertThat(r.getBaseVersionNo()).isZero();
    }
}
