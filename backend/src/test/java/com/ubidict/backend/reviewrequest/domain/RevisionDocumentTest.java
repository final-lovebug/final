package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RevisionDocumentTest {
    @Test
    void create() {
        var r = RevisionDocument.create(1L, 2L, 3, 4L, "body", 5L);
        assertThat(r.getReexamineRound()).isZero();
    }

    @Test
    void create_proposedBodyIsBlank() {
        assertThatThrownBy(() -> RevisionDocument.create(1L, 2L, 3, 4L, " ", 5L))
                .isInstanceOf(RuntimeException.class);
    }
}
