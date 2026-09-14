package com.ubidict.backend.notification.implement;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.domain.NotificationTargetType;
import com.ubidict.backend.notification.service.model.IssueNotificationCommand;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 정해진 수신자들에게 알림을 발행한다. 「발행한다」에 필요한 일을 전부 한다 — 수신자마다 엔티티를 만들어 저장한다.
 *
 * <p><b>발행하지 않을 수도 있다.</b> 수신자가 없거나 같은 사건으로 이미 만든 알림이 있으면 건너뛴다. 둘 다 흐름이 아니라 발행 정책이라 서비스가 분기하지 않는다.
 *
 * <p><b>MVP1의 전달 수단은 인앱 하나뿐이고, 인앱은 DB에 행이 있는 것이 곧 전달이다.</b> 그래서 저장 외에 보낼 곳이 없다. 메일·슬랙 같은 채널은 MVP2이며
 * ({@code REQ-NTF-005}~{@code 007}) 그때 발송 계층을 다시 들인다.
 *
 * <p>트랜잭션은 선언하지 않는다. 경계는 호출자인 서비스의 public 메서드다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationIssuer {

    private final NotificationAppender notificationAppender;

    /**
     * @return 실제로 만들어진 알림 수. 중복으로 걸러진 것은 세지 않는다
     */
    public int issue(IssueNotificationCommand command) {
        if (command.recipients().isEmpty()) {
            return 0;
        }

        int issued = 0;
        for (Long recipientId : command.recipients()) {
            Optional<Notification> saved = notificationAppender.append(Notification.create(
                    recipientId,
                    command.workspaceId(),
                    command.type(),
                    NotificationTargetType.REVIEW_REQUEST,
                    command.targetId(),
                    command.content().title(),
                    command.content().message(),
                    command.dedupeKey(),
                    command.actorId()));

            if (saved.isPresent()) {
                issued++;
            }
        }

        log.info(
                "[NotificationIssuer.issue] Notifications issued. type={}, targetId={}, recipients={}, issued={}",
                command.type(),
                command.targetId(),
                command.recipients().size(),
                issued);

        return issued;
    }
}
