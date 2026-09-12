package com.ubidict.backend.reviewrequest.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewerRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @DisplayName("리뷰 요청에 지정된 회원의 존재 여부와 리뷰어 수를 조회한다.")
    @Test
    void existsByReviewRequestIdAndMemberId() {
        // given
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰 요청", null, 1L, 1L));
        reviewerRepository.save(Reviewer.create(request.getId(), 2L, 1L));

        // when & then
        assertThat(reviewerRepository.existsByReviewRequestIdAndMemberId(request.getId(), 2L))
                .isTrue();
        assertThat(reviewerRepository.countByReviewRequestId(request.getId())).isEqualTo(1);
    }
}
