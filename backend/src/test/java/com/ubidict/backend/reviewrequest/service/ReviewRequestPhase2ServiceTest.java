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
import com.ubidict.backend.reviewrequest.service.model.RevisionResult;
import com.ubidict.backend.reviewrequest.service.model.SubmitRevisionCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class ReviewRequestPhase2ServiceTest extends IntegrationTestSupport {

    @Autowired
    private RevisionService revisionService;

    @Autowired
    private ReviewRequestService reviewRequestService;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @DisplayName("문서 개정안을 등록하고 같은 회차의 중복 등록을 거부한다.")
    @Test
    void submitDocument_duplicateRound() {
        ReviewRequest request = saveRequest(ReviewRequestType.DOCUMENT, 10L, 1L);

        RevisionResult result =
                revisionService.submitDocument(new SubmitRevisionCommand(request.getId(), 20L, 1, 30L, "개정 본문", 1L));

        assertThat(result.targetId()).isEqualTo(20L);
        assertThat(result.reexamineRound()).isZero();
        assertThatThrownBy(() -> revisionService.submitDocument(
                        new SubmitRevisionCommand(request.getId(), 20L, 1, 30L, "중복 본문", 1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_REVISION_ALREADY_EXISTS));
    }

    @DisplayName("첫 사전집 개정안은 사전집 식별자 없이 기준 버전 0으로 등록한다.")
    @Test
    void submitDictionary_firstVersion() {
        ReviewRequest request = saveRequest(ReviewRequestType.DICTIONARY, 10L, 1L);

        RevisionResult result =
                revisionService.submitDictionary(new SubmitRevisionCommand(request.getId(), null, 0, 40L, null, 1L));

        assertThat(result.targetId()).isNull();
        assertThat(result.baseVersionNo()).isZero();
        assertThat(revisionService.dictionaries(request.getId(), 0)).hasSize(1);
    }

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
