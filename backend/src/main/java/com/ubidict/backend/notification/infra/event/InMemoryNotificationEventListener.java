package com.ubidict.backend.notification.infra.event;

import com.ubidict.backend.notification.service.NotificationEventHandler;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 인메모리 수신 어댑터. <b>수신 애노테이션만 담당하고 처리는 전부 공용 핸들러에 위임한다</b> — {@code docs/ARCHITECTURE.md}의 규약이다.
 * SQS 어댑터가 같은 핸들러를 부르므로 로직이 한 벌로 유지된다.
 *
 * <p>{@code AFTER_COMMIT}인 이유 — 리뷰 반영 트랜잭션이 롤백되면 알림도 없어야 한다. {@code @Async}인 이유 — 알림 생성이 실패해도 본 작업을
 * 되돌리지 않는다. 그 대가로 여기서 던진 예외는 호출자에게 전파되지 않고 {@code AsyncEventConfig}의 핸들러가 ERROR 로그로만 남긴다.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryNotificationEventListener {

    private final NotificationEventHandler handler;

    @Async
    @TransactionalEventListener
    public void on(ReviewRequestCreatedEvent event) {
        handler.handle(event);
    }

    @Async
    @TransactionalEventListener
    public void on(ReviewSubmittedEvent event) {
        handler.handle(event);
    }

    @Async
    @TransactionalEventListener
    public void on(ReviewRequestChangesRequestedEvent event) {
        handler.handle(event);
    }

    @Async
    @TransactionalEventListener
    public void on(ReviewRequestRevisedEvent event) {
        handler.handle(event);
    }

    @Async
    @TransactionalEventListener
    public void on(ReviewRequestCanceledEvent event) {
        handler.handle(event);
    }
}
