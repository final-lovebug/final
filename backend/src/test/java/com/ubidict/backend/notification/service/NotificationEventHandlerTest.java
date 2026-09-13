package com.ubidict.backend.notification.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.notification.infra.NotificationRepository;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * 이벤트 → 알림 변환을 실제 DB 위에서 검증한다.
 *
 * <p>클래스에 {@code @Transactional}을 붙이지 않는다 — {@code IntegrationTestSupport}의 규약이고, 이 도메인에서는 특히 중요하다.
 * 롤백으로 끝나는 테스트 트랜잭션 안에서는 유니크 제약 위반이 실제로 나지 않아 멱등 검증이 무의미해진다.
 */
@TestPropertySource(properties = {"app.crossdomain.review-request.mode=real", "app.crossdomain.workspace.mode=real"})
class NotificationEventHandlerTest extends IntegrationTestSupport {

    private static final Long REQUESTER_ID = 100L;
    private static final Long REVIEWER_ID = 200L;

    @Autowired
    private NotificationEventHandler handler;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Long workspaceId;
    private Long reviewRequestId;

    /**
     * participant가 workspace를 FK로 참조하므로 워크스페이스를 실제로 만든다.
     */
    @BeforeEach
    void setUpReviewRequest() {
        workspaceId =
                workspaceRepository.save(Workspace.create("결제팀", REQUESTER_ID)).getId();
        ReviewRequest reviewRequest = reviewRequestRepository.save(ReviewRequest.create(
                workspaceId, ReviewRequestType.DOCUMENT, "결제 문서 개정 반영", null, REQUESTER_ID, REQUESTER_ID));
        reviewRequestId = reviewRequest.getId();
    }

    @DisplayName("리뷰 요청이 만들어지면 지정된 리뷰어에게 알림이 간다.")
    @Test
    void reviewRequestCreated() {
        // given
        reviewerRepository.save(Reviewer.create(reviewRequestId, REVIEWER_ID, REQUESTER_ID));

        // when
        handler.handle(new ReviewRequestCreatedEvent(
                reviewRequestId, workspaceId, ReviewRequestType.DOCUMENT, 10L, REQUESTER_ID, OffsetDateTime.now()));

        // then
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getRecipientId()).isEqualTo(REVIEWER_ID);
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.REVIEW_REQUEST_RECEIVED);
    }

    @DisplayName("지정된 리뷰어가 없으면 리뷰 요청 도착 알림을 만들지 않는다.")
    @Test
    void reviewRequestCreated_withoutReviewer() {
        // when
        handler.handle(new ReviewRequestCreatedEvent(
                reviewRequestId, workspaceId, ReviewRequestType.DOCUMENT, 10L, REQUESTER_ID, OffsetDateTime.now()));

        // then
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @DisplayName("같은 이벤트를 두 번 처리해도 알림은 한 건만 생긴다.")
    @Test
    void idempotent() {
        // given
        reviewerRepository.save(Reviewer.create(reviewRequestId, REVIEWER_ID, REQUESTER_ID));
        ReviewRequestCreatedEvent event = new ReviewRequestCreatedEvent(
                reviewRequestId, workspaceId, ReviewRequestType.DOCUMENT, 10L, REQUESTER_ID, OffsetDateTime.now());

        // when
        handler.handle(event);
        handler.handle(event);

        // then
        assertThat(notificationRepository.findAll()).hasSize(1);
    }

    @DisplayName("승인 판정이 제출되면 요청자에게 알림이 간다.")
    @Test
    void reviewSubmitted_approved() {
        // when
        handler.handle(new ReviewSubmittedEvent(
                reviewRequestId, 1L, REVIEWER_ID, ReviewVerdict.APPROVED, 0, OffsetDateTime.now()));

        // then
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getRecipientId()).isEqualTo(REQUESTER_ID);
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.APPROVED);
    }

    @DisplayName("변경요청 판정은 승인 알림을 만들지 않는다. 변경요청 이벤트가 따로 알리기 때문이다.")
    @Test
    void reviewSubmitted_changesRequested() {
        // when
        handler.handle(new ReviewSubmittedEvent(
                reviewRequestId, 1L, REVIEWER_ID, ReviewVerdict.CHANGES_REQUESTED, 0, OffsetDateTime.now()));

        // then
        assertThat(notificationRepository.findAll()).isEmpty();
    }

    @DisplayName("변경요청 이벤트는 요청자에게 알림을 만든다.")
    @Test
    void changesRequested() {
        // when
        handler.handle(new ReviewRequestChangesRequestedEvent(
                reviewRequestId, ReviewRequestType.DOCUMENT, 10L, REQUESTER_ID, OffsetDateTime.now()));

        // then
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.CHANGES_REQUESTED);
    }

    @DisplayName("반영완료 알림은 워크스페이스 참여자 전원에게 간다.")
    @Test
    void revised() {
        // given
        participantRepository.save(Participant.owner(workspaceId, REQUESTER_ID));
        participantRepository.save(Participant.join(workspaceId, REVIEWER_ID, Permission.REGULAR, REQUESTER_ID));

        // when
        handler.handle(new ReviewRequestRevisedEvent(
                reviewRequestId, ReviewRequestType.DOCUMENT, 10L, 6, OffsetDateTime.now()));

        // then
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(2);
        assertThat(notifications).allMatch(notification -> notification.getType() == NotificationType.REVISED);
        assertThat(notifications.stream().map(Notification::getRecipientId))
                .containsExactlyInAnyOrder(REQUESTER_ID, REVIEWER_ID);
    }

    @DisplayName("취소 이벤트는 요청자에게 알림을 만든다.")
    @Test
    void canceled() {
        // when
        handler.handle(
                new ReviewRequestCanceledEvent(reviewRequestId, ReviewRequestType.DOCUMENT, 10L, OffsetDateTime.now()));

        // then
        List<Notification> notifications = notificationRepository.findAll();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.getFirst().getType()).isEqualTo(NotificationType.CANCELED);
    }

    @DisplayName("리뷰 요청을 찾지 못하면 알림을 만들지 않고 조용히 끝낸다.")
    @Test
    void missingReviewRequest() {
        // when
        handler.handle(
                new ReviewRequestRevisedEvent(999_999L, ReviewRequestType.DOCUMENT, 10L, 6, OffsetDateTime.now()));

        // then
        assertThat(notificationRepository.findAll()).isEmpty();
    }
}
