package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.service.model.CancelReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.CreateReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.reviewrequest.service.model.UpdateReviewRequestCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewRequestServiceTest extends IntegrationTestSupport {

    private static final Long REQUESTER_ID = 1L;
    private static final Long OTHER_MEMBER_ID = 2L;

    @Autowired
    private ReviewRequestService reviewRequestService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private Long workspaceId;

    @BeforeEach
    void createWorkspace() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
        joinAs(workspaceId, REQUESTER_ID, Permission.OWNER);
        joinAs(workspaceId, OTHER_MEMBER_ID, Permission.REGULAR);
    }

    @DisplayName("리뷰 요청을 생성하면 요청 내용과 리뷰대기 상태가 저장된다.")
    @Test
    void create() {
        // when
        ReviewRequestResult result = createReviewRequest();

        // then
        assertThat(result.reviewRequestId()).isNotNull();
        assertThat(result.workspaceId()).isEqualTo(workspaceId);
        assertThat(result.status()).isEqualTo(ReviewRequestStatus.PENDING_REVIEW);
    }

    @DisplayName("리뷰 요청의 제목과 설명을 수정한다.")
    @Test
    void update() {
        // given
        ReviewRequestResult created = createReviewRequest();

        // when
        ReviewRequestResult result = reviewRequestService.update(new UpdateReviewRequestCommand(
                created.reviewRequestId(), "정산 문서 리뷰", "정산 문서의 개정안을 검토합니다.", REQUESTER_ID));

        // then
        assertThat(result.title()).isEqualTo("정산 문서 리뷰");
        assertThat(result.description()).isEqualTo("정산 문서의 개정안을 검토합니다.");
    }

    @DisplayName("요청자가 리뷰 요청을 취소하면 취소 상태가 된다.")
    @Test
    void cancel() {
        // given
        ReviewRequestResult created = createReviewRequest();

        // when
        ReviewRequestResult result =
                reviewRequestService.cancel(new CancelReviewRequestCommand(created.reviewRequestId(), REQUESTER_ID));

        // then
        assertThat(result.status()).isEqualTo(ReviewRequestStatus.CANCELED);
    }

    @DisplayName("요청자가 아닌 참여자가 리뷰 요청을 취소하면 예외가 발생한다.")
    @Test
    void cancel_notRequester() {
        // given
        ReviewRequestResult created = createReviewRequest();

        // when & then
        assertThatThrownBy(() -> reviewRequestService.cancel(
                        new CancelReviewRequestCommand(created.reviewRequestId(), OTHER_MEMBER_ID)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REQUESTER);
    }

    private ReviewRequestResult createReviewRequest() {
        return reviewRequestService.create(new CreateReviewRequestCommand(
                workspaceId, ReviewRequestType.DOCUMENT, "결제 문서 리뷰", "결제 문서의 개정안을 검토합니다.", REQUESTER_ID));
    }

    private void joinAs(Long workspaceId, Long memberId, Permission permission) {
        Participant participant = ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(memberId)
                .permission(permission)
                .build();
        participantRepository.save(participant);
    }
}
