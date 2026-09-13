package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.LatestReviewAggregator;
import com.ubidict.backend.reviewrequest.implement.ReviewReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviseEligibilityCalculator;
import com.ubidict.backend.reviewrequest.implement.ReviseProcessor;
import com.ubidict.backend.reviewrequest.implement.ReviseWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.infra.port.WorkspacePolicyPort;
import com.ubidict.backend.reviewrequest.service.model.PerformReviseCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviseServiceTest {

    @Mock
    private ReviewRequestReader reviewRequestReader;

    @Mock
    private ReviewReader reviewReader;

    @Mock
    private RevisionDocumentReader revisionDocumentReader;

    @Mock
    private RevisionDictionaryReader revisionDictionaryReader;

    @Mock
    private ReviseProcessor reviseProcessor;

    @Mock
    private ReviseWriter reviseWriter;

    @Mock
    private WorkspacePolicyPort workspacePolicyPort;

    @Mock
    private WorkspaceAccessValidator workspaceAccessValidator;

    private ReviseService reviseService;

    @BeforeEach
    void setUp() {
        reviseService = new ReviseService(
                reviewRequestReader,
                reviewReader,
                revisionDocumentReader,
                revisionDictionaryReader,
                new LatestReviewAggregator(),
                new ReviseEligibilityCalculator(),
                reviseProcessor,
                reviseWriter,
                workspacePolicyPort,
                workspaceAccessValidator);
    }

    @DisplayName("발행하면 반영완료가 되고 반영일시가 기록된다.")
    @Test
    void perform() {
        // given
        ReviewRequest request = request(ReviewRequestStatus.APPROVED);
        RevisionDocument revision = RevisionDocument.create(1L, 20L, 1, 30L, "개정 본문", 1L);
        given(reviewRequestReader.read(1L)).willReturn(request);
        given(reviewReader.readLatest(1L)).willReturn(List.of(Review.submit(1L, 2L, 0, ReviewVerdict.APPROVED, 2L)));
        given(workspacePolicyPort.requiredReviewerCount(10L, ReviewRequestType.DOCUMENT))
                .willReturn(1);
        given(revisionDocumentReader.readCurrent(1L)).willReturn(revision);
        given(reviseProcessor.processDocument(request, revision, 1L)).willReturn(2);
        given(reviseWriter.write(org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        var result = reviseService.perform(new PerformReviseCommand(1L, 1L));

        // then
        assertThat(result.resultVersionNo()).isEqualTo(2);
        assertThat(request.getStatus()).isEqualTo(ReviewRequestStatus.REVISED);
        assertThat(request.getRevisedAt()).isNotNull();
    }

    @DisplayName("발행 조건을 충족하지 못하면 발행할 수 없다.")
    @Test
    void perform_notEligible() {
        // given
        ReviewRequest request = request(ReviewRequestStatus.IN_REVIEW);
        given(reviewRequestReader.read(1L)).willReturn(request);
        given(reviewReader.readLatest(1L)).willReturn(List.of());
        given(workspacePolicyPort.requiredReviewerCount(10L, ReviewRequestType.DOCUMENT))
                .willReturn(1);

        // when & then
        assertThatThrownBy(() -> reviseService.perform(new PerformReviseCommand(1L, 1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_ELIGIBLE_FOR_REVISE));
    }

    @DisplayName("이미 반영된 요청은 다시 발행할 수 없다.")
    @Test
    void perform_alreadyRevised() {
        // given
        ReviewRequest request = request(ReviewRequestStatus.REVISED);
        given(reviewRequestReader.read(1L)).willReturn(request);

        // when & then
        assertThatThrownBy(() -> reviseService.perform(new PerformReviseCommand(1L, 1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_ALREADY_REVISED));
    }

    @DisplayName("정족수가 0이면 승인 없이 발행된다.")
    @Test
    void perform_zeroQuorumWithoutApproval() {
        // given
        ReviewRequest request = request(ReviewRequestStatus.PENDING_REVIEW);
        RevisionDocument revision = RevisionDocument.create(1L, 20L, 1, 30L, "개정 본문", 1L);
        given(reviewRequestReader.read(1L)).willReturn(request);
        given(reviewReader.readLatest(1L)).willReturn(List.of());
        given(workspacePolicyPort.requiredReviewerCount(10L, ReviewRequestType.DOCUMENT))
                .willReturn(0);
        given(revisionDocumentReader.readCurrent(1L)).willReturn(revision);
        given(reviseProcessor.processDocument(request, revision, 1L)).willReturn(2);
        given(reviseWriter.write(org.mockito.ArgumentMatchers.any()))
                .willAnswer(invocation -> invocation.getArgument(0));

        // when
        reviseService.perform(new PerformReviseCommand(1L, 1L));

        // then
        assertThat(request.getStatus()).isEqualTo(ReviewRequestStatus.REVISED);
    }

    private ReviewRequest request(ReviewRequestStatus status) {
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰", null, 1L, 1L);
        ReflectionTestUtils.setField(request, "id", 1L);
        ReflectionTestUtils.setField(request, "status", status);
        return request;
    }
}
