package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.*;

import com.ubidict.backend.reviewrequest.implement.RevisionTypeValidator;
import org.junit.jupiter.api.Test;

class RevisionTypeValidatorTest {
    @Test
    void documentTypeMatches() {
        var r = ReviewRequest.create(1L, ReviewRequestType.DOCUMENT, "t", null, 1L, 1L);
        assertThatCode(() -> new RevisionTypeValidator().validate(r, true)).doesNotThrowAnyException();
    }

    @Test
    void mismatched() {
        var r = ReviewRequest.create(1L, ReviewRequestType.DOCUMENT, "t", null, 1L, 1L);
        assertThatThrownBy(() -> new RevisionTypeValidator().validate(r, false)).isInstanceOf(RuntimeException.class);
    }
}
