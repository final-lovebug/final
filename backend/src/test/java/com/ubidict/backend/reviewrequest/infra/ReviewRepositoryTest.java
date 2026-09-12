package com.ubidict.backend.reviewrequest.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.fixture.ReviewFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @DisplayName("리뷰 요청의 리뷰 이력을 회차와 제출 순서로 조회한다.")
    @Test
    void findByReviewRequestId() {
        // given
        ReviewRequest request = saveRequest();
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .targetRound(0)
                .build());
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .memberId(2L)
                .targetRound(1)
                .build());
        em.flush();
        em.clear();

        // when & then
        assertThat(reviewRepository.findByReviewRequestIdAndDeletedAtIsNullOrderBySubmittedAtAscIdAsc(request.getId()))
                .hasSize(2);
        assertThat(reviewRepository.findByReviewRequestIdAndTargetRoundAndDeletedAtIsNullOrderBySubmittedAtAscIdAsc(
                        request.getId(), 1))
                .extracting(Review::getMemberId)
                .containsExactly(2L);
    }

    @DisplayName("판정별 리뷰 이력 수를 조회한다.")
    @Test
    void countByVerdict() {
        // given
        ReviewRequest request = saveRequest();
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .verdict(ReviewVerdict.APPROVED)
                .build());
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .memberId(2L)
                .verdict(ReviewVerdict.CHANGES_REQUESTED)
                .build());

        // when & then
        assertThat(reviewRepository.countByReviewRequestIdAndVerdictAndDeletedAtIsNull(
                        request.getId(), ReviewVerdict.APPROVED))
                .isEqualTo(1);
    }

    @DisplayName("회원별 가장 최근 판정만 조회한다.")
    @Test
    void findLatestByReviewRequestId() {
        // given
        ReviewRequest request = saveRequest();
        OffsetDateTime now = OffsetDateTime.now();
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .memberId(1L)
                .verdict(ReviewVerdict.CHANGES_REQUESTED)
                .submittedAt(now.minusMinutes(1))
                .build());
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .memberId(1L)
                .verdict(ReviewVerdict.APPROVED)
                .submittedAt(now)
                .build());
        reviewRepository.save(ReviewFixture.review()
                .reviewRequestId(request.getId())
                .memberId(2L)
                .verdict(ReviewVerdict.CHANGES_REQUESTED)
                .submittedAt(now)
                .build());
        em.flush();
        em.clear();

        // when
        var latest = reviewRepository.findLatestByReviewRequestId(request.getId());

        // then
        assertThat(latest)
                .extracting(Review::getMemberId, Review::getVerdict)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(1L, ReviewVerdict.APPROVED),
                        org.assertj.core.groups.Tuple.tuple(2L, ReviewVerdict.CHANGES_REQUESTED));
    }

    private ReviewRequest saveRequest() {
        return reviewRequestRepository.save(
                ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰 요청", null, 1L, 1L));
    }
}
