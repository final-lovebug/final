package com.ubidict.backend.notification.service;

import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.notification.implement.NotificationDedupeKeyFactory;
import com.ubidict.backend.notification.implement.NotificationIssuer;
import com.ubidict.backend.notification.implement.NotificationMessageFactory;
import com.ubidict.backend.notification.implement.NotificationRecipientResolver;
import com.ubidict.backend.notification.implement.ReviewRequestSnapshotReader;
import com.ubidict.backend.notification.service.model.IssueNotificationCommand;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 도메인 이벤트를 알림으로 바꾸는 <b>기술 무관 공용 핸들러</b>.
 *
 * <p>{@code docs/ARCHITECTURE.md}: 「어댑터는 수신 애노테이션만 담당하고 처리 로직은 공용 핸들러에 위임한다. 인메모리와 외부 큐가 같은 핸들러를
 * 호출해야 비즈니스 로직이 한 벌로 유지된다.」 인메모리 리스너와 SQS 리스너가 모두 이 클래스를 부른다.
 *
 * <p>다섯 메서드가 모두 같은 흐름이다 — <b>대상을 읽고 · 수신자를 정하고 · 문구를 만들고 · 중복 방지 키를 만들고 · 발행한다.</b> 주입된 다섯 개가 그
 * 다섯 단계와 1:1로 대응한다. 「설정이 없으면 기본값을 깐다」「음소거된 유형은 건너뛴다」「이미 만든 알림은 다시 만들지 않는다」는 전부
 * {@link NotificationIssuer} 아래에 있다.
 *
 * <p><b>여기 남은 분기는 하나뿐이다</b> — {@link ReviewSubmittedEvent}의 판정 구분. 그것은 이벤트 페이로드에 달린 판단이라 흐름에 보여야 하고,
 * 음소거·수신자 없음·중복은 알림 정책에 달린 판단이라 아래로 내렸다.
 *
 * <p><b>전부 멱등이다.</b> 같은 이벤트가 두 번 도착해도 알림은 한 번만 생긴다 — {@code dedupeKey}가 결정론적이고
 * {@code (recipientId, dedupeKey)}에 유니크 제약이 걸려 있다({@code NFR-NTF-002}).
 *
 * <p><b>이벤트 순서에 의존하지 않는다.</b> 각 메서드가 필요한 상태를 다시 조회하므로 도착 순서가 뒤바뀌어도 결과가 달라지지 않는다.
 *
 * <p>트랜잭션 경계는 이 클래스의 public 메서드뿐이다 — {@code ARCHITECTURE.md} 「implement에는 원칙적으로 트랜잭션을 선언하지 않는다」.
 */
@Service
@RequiredArgsConstructor
public class NotificationEventHandler {

    private final ReviewRequestSnapshotReader snapshotReader;
    private final NotificationRecipientResolver recipientResolver;
    private final NotificationMessageFactory messageFactory;
    private final NotificationDedupeKeyFactory dedupeKeyFactory;
    private final NotificationIssuer notificationIssuer;

    /**
     * 리뷰 요청 도착 — 지정된 리뷰어에게. 지정된 리뷰어가 없으면 아무도 받지 않는다.
     */
    @Transactional
    public void handle(ReviewRequestCreatedEvent event) {
        snapshotReader
                .read(event.reviewRequestId())
                .ifPresent(request -> notificationIssuer.issue(new IssueNotificationCommand(
                        request.workspaceId(),
                        event.reviewRequestId(),
                        NotificationType.REVIEW_REQUEST_RECEIVED,
                        recipientResolver.forReviewRequestReceived(event.reviewRequestId(), request.requesterId()),
                        messageFactory.reviewRequestReceived(request.title(), request.type()),
                        dedupeKeyFactory.reviewRequestCreated(event.reviewRequestId()),
                        request.requesterId())));
    }

    /**
     * 승인 — 요청자에게.
     *
     * <p>변경요청 판정은 {@link ReviewRequestChangesRequestedEvent}가 따로 알린다. 여기서는 승인만 처리한다 — 둘 다 만들면 한 번의
     * 변경요청에 알림이 두 건 간다.
     */
    @Transactional
    public void handle(ReviewSubmittedEvent event) {
        if (event.verdict() != ReviewVerdict.APPROVED) {
            return;
        }

        snapshotReader
                .read(event.reviewRequestId())
                .ifPresent(request -> notificationIssuer.issue(new IssueNotificationCommand(
                        request.workspaceId(),
                        event.reviewRequestId(),
                        NotificationType.APPROVED,
                        recipientResolver.forRequester(request.requesterId(), event.memberId()),
                        messageFactory.approved(request.title()),
                        dedupeKeyFactory.reviewSubmitted(
                                event.reviewRequestId(), event.memberId(), event.targetRound()),
                        event.memberId())));
    }

    /**
     * 변경요청 — 요청자에게. 재교정 후 다시 제출해야 하는 사람이 그 사람이다.
     */
    @Transactional
    public void handle(ReviewRequestChangesRequestedEvent event) {
        snapshotReader
                .read(event.reviewRequestId())
                .ifPresent(request -> notificationIssuer.issue(IssueNotificationCommand.withoutActor(
                        request.workspaceId(),
                        event.reviewRequestId(),
                        NotificationType.CHANGES_REQUESTED,
                        recipientResolver.forRequester(request.requesterId()),
                        messageFactory.changesRequested(request.title()),
                        dedupeKeyFactory.changesRequested(event.reviewRequestId(), event.occurredAt()))));
    }

    /**
     * 반영완료 — 워크스페이스 참여자 전원에게. 새 문서·사전집 버전이 생긴 사건이라 요청 관계자를 넘어 전체가 알아야 의미가 있다.
     */
    @Transactional
    public void handle(ReviewRequestRevisedEvent event) {
        snapshotReader
                .read(event.reviewRequestId())
                .ifPresent(request -> notificationIssuer.issue(IssueNotificationCommand.withoutActor(
                        request.workspaceId(),
                        event.reviewRequestId(),
                        NotificationType.REVISED,
                        recipientResolver.forWorkspace(request.workspaceId()),
                        messageFactory.revised(request.title(), request.type(), event.resultVersionNo()),
                        dedupeKeyFactory.revised(event.reviewRequestId(), event.resultVersionNo()))));
    }

    /**
     * 취소 — 요청자에게. 요청자 본인이 취소하면 수신자에서 빠져 알림이 생기지 않는다.
     */
    @Transactional
    public void handle(ReviewRequestCanceledEvent event) {
        snapshotReader
                .read(event.reviewRequestId())
                .ifPresent(request -> notificationIssuer.issue(IssueNotificationCommand.withoutActor(
                        request.workspaceId(),
                        event.reviewRequestId(),
                        NotificationType.CANCELED,
                        recipientResolver.forRequester(request.requesterId()),
                        messageFactory.canceled(request.title()),
                        dedupeKeyFactory.canceled(event.reviewRequestId()))));
    }
}
