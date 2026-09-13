package com.ubidict.backend.notification.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewerRepository;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 발행부가 붙었을 때 알림이 실제로 흐르는지 증명한다.
 *
 * <p>리뷰 도메인의 발행부(`RR-4d`)가 아직 없으므로 이 테스트가 {@link EventPublisher}로 직접 발행해 그 자리를 대신한다. <b>발행부가 들어오는
 * 순간 배선 없이 동작한다는 것이 여기서 확인된다</b> — 핸들러를 직접 부르는 것이 아니라 포트 → 인메모리 어댑터 →
 * {@code @TransactionalEventListener(AFTER_COMMIT)} → {@code @Async} 경로를 그대로 탄다.
 *
 * <p>테스트에 {@code @Transactional}을 붙이지 않는 이유가 여기서 분명하다 — 붙이면 커밋이 일어나지 않아 리스너가 영원히 실행되지 않는다.
 */
@TestPropertySource(properties = {"app.crossdomain.review-request.mode=real", "app.crossdomain.workspace.mode=real"})
class NotificationEventWiringTest extends IntegrationTestSupport {

    private static final Long REQUESTER_ID = 100L;
    private static final Long REVIEWER_ID = 200L;

    @Autowired
    private EventPublisher eventPublisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private ReviewerRepository reviewerRepository;

    @DisplayName("커밋된 뒤 이벤트가 비동기로 전달되어 알림이 만들어진다.")
    @Test
    void publishesThroughAfterCommitListener() {
        // given
        Long workspaceId =
                workspaceRepository.save(Workspace.create("결제팀", REQUESTER_ID)).getId();
        ReviewRequest reviewRequest = reviewRequestRepository.save(ReviewRequest.create(
                workspaceId, ReviewRequestType.DOCUMENT, "결제 문서 개정 반영", null, REQUESTER_ID, REQUESTER_ID));
        reviewerRepository.save(Reviewer.create(reviewRequest.getId(), REVIEWER_ID, REQUESTER_ID));

        // when — 트랜잭션 안에서 발행하고 커밋한다
        transactionTemplate.executeWithoutResult(status -> eventPublisher.publish(new ReviewRequestCreatedEvent(
                reviewRequest.getId(),
                workspaceId,
                ReviewRequestType.DOCUMENT,
                10L,
                REQUESTER_ID,
                OffsetDateTime.now())));

        // then
        await(() -> notificationRepository.count() == 1);
        assertThat(notificationRepository.findAll().getFirst().getType())
                .isEqualTo(NotificationType.REVIEW_REQUEST_RECEIVED);
        assertThat(notificationRepository.findAll().getFirst().getRecipientId()).isEqualTo(REVIEWER_ID);
    }

    /**
     * {@code @Async}라 발행 직후에는 아직 행이 없다. Awaitility 의존성을 더하지 않고 짧게 폴링한다.
     */
    private static void await(Callable<Boolean> condition) {
        Duration timeout = Duration.ofSeconds(5);
        long deadline = System.nanoTime() + timeout.toNanos();
        try {
            while (System.nanoTime() < deadline) {
                if (Boolean.TRUE.equals(condition.call())) {
                    return;
                }
                Thread.sleep(50);
            }
            throw new AssertionError("비동기 리스너가 " + timeout.toSeconds() + "초 안에 알림을 만들지 않았다.");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError("대기 중 인터럽트됐다.", exception);
        } catch (Exception exception) {
            throw new AssertionError("조건 평가에 실패했다.", exception);
        }
    }
}
