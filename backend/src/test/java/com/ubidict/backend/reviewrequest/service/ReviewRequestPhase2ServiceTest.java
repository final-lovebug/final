package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestSearchQuery;
import com.ubidict.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class ReviewRequestPhase2ServiceTest extends IntegrationTestSupport {

    @Autowired
    private ReviewRequestService reviewRequestService;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @DisplayName("리뷰어 회원 식별자와 상태로 리뷰 요청 목록을 필터링한다.")
    @Test
    void search_filtersByReviewerAndStatus() {
        ReviewRequest expected = saveRequest(ReviewRequestType.DOCUMENT, 10L, 1L);
        ReflectionTestUtils.setField(expected, "status", ReviewRequestStatus.IN_REVIEW);
        reviewRequestRepository.save(expected);
        reviewerRepository.save(Reviewer.create(expected.getId(), 88L, 1L));
        saveRequest(ReviewRequestType.DOCUMENT, 10L, 2L);

        var result = reviewRequestService.search(new ReviewRequestSearchQuery(
                10L, null, ReviewRequestStatus.IN_REVIEW, null, 88L, 0, 20, "createdAt,desc"));

        assertThat(result.content()).extracting("reviewRequestId").containsExactly(expected.getId());
    }

    @DisplayName("워크스페이스 식별자가 없는 목록 요청을 거부한다.")
    @Test
    void search_requiresWorkspaceId() {
        assertThatThrownBy(() -> new ReviewRequestSearchQuery(null, null, null, null, null, 0, 20, null))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_WORKSPACE_ID_REQUIRED));
    }

    private ReviewRequest saveRequest(ReviewRequestType type, Long workspaceId, Long requesterId) {
        return reviewRequestRepository.save(
                ReviewRequest.create(workspaceId, type, "리뷰 요청", null, requesterId, requesterId));
    }
}
