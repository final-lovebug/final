package com.ubidict.backend.reviewrequest.fixture;

import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import java.time.OffsetDateTime;
import org.springframework.test.util.ReflectionTestUtils;

public class ReviewFixture {

    public static ReviewBuilder review() {
        return new ReviewBuilder();
    }

    public static class ReviewBuilder {

        private Long id;
        private Long reviewRequestId = 1L;
        private Long memberId = 1L;
        private int targetRound;
        private ReviewVerdict verdict = ReviewVerdict.APPROVED;
        private OffsetDateTime submittedAt;

        public ReviewBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public ReviewBuilder reviewRequestId(Long reviewRequestId) {
            this.reviewRequestId = reviewRequestId;
            return this;
        }

        public ReviewBuilder memberId(Long memberId) {
            this.memberId = memberId;
            return this;
        }

        public ReviewBuilder targetRound(int targetRound) {
            this.targetRound = targetRound;
            return this;
        }

        public ReviewBuilder verdict(ReviewVerdict verdict) {
            this.verdict = verdict;
            return this;
        }

        public ReviewBuilder submittedAt(OffsetDateTime submittedAt) {
            this.submittedAt = submittedAt;
            return this;
        }

        public Review build() {
            Review review = Review.submit(reviewRequestId, memberId, targetRound, verdict, memberId);
            if (id != null) {
                ReflectionTestUtils.setField(review, "id", id);
            }
            if (submittedAt != null) {
                ReflectionTestUtils.setField(review, "submittedAt", submittedAt);
            }
            return review;
        }
    }
}
