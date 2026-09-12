package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import com.ubidict.backend.reviewrequest.service.model.AssignReviewerCommand;
import com.ubidict.backend.reviewrequest.service.model.SubmitReviewCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewSubmitServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 1L;
    private static final Long REVIEWER_ID = 2L;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewerService reviewerService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private RevisionDocumentRepository revisionDocumentRepository;

    @DisplayName("리뷰를 제출하면 리뷰 요청이 리뷰중이 된다.")
    @Test
    void submit() {
        // given
        ReviewRequest request = saveRequest(0);

        // when
        reviewService.submit(new SubmitReviewCommand(request.getId(), REVIEWER_ID, 0, ReviewVerdict.APPROVED));

        // then
        assertThat(readRequest(request.getId()).getStatus()).isEqualTo(ReviewRequestStatus.IN_REVIEW);
    }

    @DisplayName("승인 수가 정족수를 채우면 승인 상태가 된다.")
    @Test
    void submit_reachesQuorum() {
        // given
        ReviewRequest request = saveRequest(1);

        // when
        reviewService.submit(new SubmitReviewCommand(request.getId(), REVIEWER_ID, 0, ReviewVerdict.APPROVED));

        // then
        ReviewRequest approved = readRequest(request.getId());
        assertThat(approved.getStatus()).isEqualTo(ReviewRequestStatus.APPROVED);
        assertThat(approved.getApprovedAt()).isNotNull();
    }

    @DisplayName("변경요청 판정을 제출하면 변경요청 상태가 된다.")
    @Test
    void submit_changesRequested() {
        // given
        ReviewRequest request = saveRequest(1);

        // when
        reviewService.submit(new SubmitReviewCommand(request.getId(), REVIEWER_ID, 0, ReviewVerdict.CHANGES_REQUESTED));

        // then
        assertThat(readRequest(request.getId()).getStatus()).isEqualTo(ReviewRequestStatus.CHANGES_REQUESTED);
    }

    @DisplayName("워크스페이스 참여자가 아니면 리뷰를 제출할 수 없다.")
    @Test
    void submit_notParticipant() {
        // given
        ReviewRequest request = saveRequest(1);

        // when & then
        assertThatThrownBy(() ->
                        reviewService.submit(new SubmitReviewCommand(request.getId(), 99L, 0, ReviewVerdict.APPROVED)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    @DisplayName("같은 회원이 판정을 바꿔 다시 제출하면 최신 판정으로 상태를 계산한다.")
    @Test
    void submit_resubmitBySameMember() {
        // given
        ReviewRequest request = saveRequest(1);
        reviewService.submit(new SubmitReviewCommand(request.getId(), REVIEWER_ID, 0, ReviewVerdict.CHANGES_REQUESTED));

        // when
        reviewService.submit(new SubmitReviewCommand(request.getId(), REVIEWER_ID, 0, ReviewVerdict.APPROVED));

        // then
        var progress = reviewService.progress(request.getId(), REVIEWER_ID);
        assertThat(readRequest(request.getId()).getStatus()).isEqualTo(ReviewRequestStatus.APPROVED);
        assertThat(progress.approvedCount()).isEqualTo(1);
        assertThat(progress.changesRequestedCount()).isZero();
        assertThat(progress.reviseEligible()).isTrue();
    }

    @DisplayName("워크스페이스 참여자가 아닌 회원을 리뷰어로 지정하면 예외가 발생한다.")
    @Test
    void assignReviewer_notParticipant() {
        // given
        ReviewRequest request = saveRequest(1);

        // when & then
        assertThatThrownBy(() -> reviewerService.assign(new AssignReviewerCommand(request.getId(), 99L, OWNER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));
    }

    private ReviewRequest saveRequest(int requiredReviewerCount) {
        Workspace workspace = Workspace.create("개발팀", OWNER_ID);
        workspace.changeRuleSet(new RuleSet(requiredReviewerCount, 0));
        workspace = workspaceRepository.save(workspace);
        participantRepository.save(Participant.owner(workspace.getId(), OWNER_ID));
        participantRepository.save(Participant.join(workspace.getId(), REVIEWER_ID, Permission.REGULAR, OWNER_ID));
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(workspace.getId(), ReviewRequestType.DOCUMENT, "리뷰 요청", null, OWNER_ID, OWNER_ID));
        revisionDocumentRepository.save(RevisionDocument.create(request.getId(), 10L, 1, 20L, "개정 본문", OWNER_ID));
        return request;
    }

    private ReviewRequest readRequest(Long reviewRequestId) {
        return reviewRequestRepository.findById(reviewRequestId).orElseThrow();
    }
}
