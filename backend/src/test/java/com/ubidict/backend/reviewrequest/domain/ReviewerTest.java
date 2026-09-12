package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ReviewerTest {
    @Test
    void create() {
        Reviewer r = Reviewer.create(1L, 2L, 3L);
        assertThat(r.getReviewRequestId()).isEqualTo(1L);
        assertThat(r.getMemberId()).isEqualTo(2L);
        assertThat(r.getAssignedAt()).isNotNull();
    }

    @Test
    void create_memberIdIsNull() {
        assertThatThrownBy(() -> Reviewer.create(1L, null, 3L)).isInstanceOf(RuntimeException.class);
    }
}
