package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewTest {

    @DisplayName("승인 리뷰를 제출한다.")
    @Test
    void submit() {
        // when
        Review review = Review.submit(1L, 2L, 0, ReviewVerdict.APPROVED, 2L);

        // then
        assertThat(review.getReviewRequestId()).isEqualTo(1L);
        assertThat(review.getMemberId()).isEqualTo(2L);
        assertThat(review.getTargetRound()).isZero();
        assertThat(review.isApproval()).isTrue();
        assertThat(review.getSubmittedAt()).isNotNull();
    }

    @DisplayName("대상 회차가 음수이면 리뷰 제출을 거부한다.")
    @Test
    void submit_targetRoundIsNegative() {
        // when & then
        assertThatThrownBy(() -> Review.submit(1L, 2L, -1, ReviewVerdict.APPROVED, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_STALE_TARGET_ROUND));
    }
}
