package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.implement.ApprovalAuthorityValidator;
import com.ubidict.backend.reviewrequest.implement.LatestReviewAggregator;
import com.ubidict.backend.reviewrequest.implement.ReviewReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestEventPublisher;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestWriter;
import com.ubidict.backend.reviewrequest.implement.ReviseEligibilityCalculator;
import com.ubidict.backend.reviewrequest.implement.ReviseProcessor;
import com.ubidict.backend.reviewrequest.implement.ReviseWriter;
import com.ubidict.backend.reviewrequest.implement.RevisionDictionaryReader;
import com.ubidict.backend.reviewrequest.implement.RevisionDocumentReader;
import com.ubidict.backend.reviewrequest.infra.port.WorkspacePolicyPort;
import com.ubidict.backend.reviewrequest.service.model.PerformReviseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

@ExtendWith(MockitoExtension.class)
class ReviewRequestConcurrencyTest {

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
    private ReviewRequestWriter reviewRequestWriter;

    @Mock
    private WorkspacePolicyPort workspacePolicyPort;

    @Mock
    private ApprovalAuthorityValidator approvalAuthorityValidator;

    @Mock
    private ReviewRequestEventPublisher eventPublisher;

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
                reviewRequestWriter,
                workspacePolicyPort,
                approvalAuthorityValidator,
                eventPublisher);
    }

    @DisplayName("같은 요청을 동시에 발행하면 충돌을 감지한다.")
    @Test
    void revise_concurrentModification() {
        // given
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰", null, 1L, 1L);
        given(reviewRequestReader.read(1L)).willReturn(request);
        given(reviewRequestReader.readForRevision(1L))
                .willThrow(new OptimisticLockingFailureException("stale review request"));

        // when & then
        assertThatThrownBy(() -> reviseService.perform(new PerformReviseCommand(1L, 1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_CONCURRENT_MODIFICATION));
    }
}
