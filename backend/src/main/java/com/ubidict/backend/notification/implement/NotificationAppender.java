package com.ubidict.backend.notification.implement;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.infra.NotificationRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationAppender {

    private final NotificationRepository notificationRepository;

    /**
     * 알림을 저장한다. <b>이미 같은 사건으로 만들어진 알림이 있으면 저장하지 않고 빈 Optional을 돌려준다.</b>
     *
     * <p>먼저 조회해 거르고, 그 사이에 끼어든 동시 삽입은 유니크 위반으로 잡는다. 조회만으로는 두 스레드가 동시에 「없다」를 보고 둘 다 넣을 수 있고, 제약만으로는
     * 흔한 재수신마다 예외 스택이 쌓인다. 둘 다 두는 이유다.
     */
    public Optional<Notification> append(Notification notification) {
        if (notificationRepository.existsByRecipientIdAndDedupeKey(
                notification.getRecipientId(), notification.getDedupeKey())) {
            log.debug(
                    "[NotificationAppender.append] Duplicated notification skipped. recipientId={}, dedupeKey={}",
                    notification.getRecipientId(),
                    notification.getDedupeKey());

            return Optional.empty();
        }

        try {
            return Optional.of(notificationRepository.saveAndFlush(notification));
        } catch (DataIntegrityViolationException exception) {
            log.debug(
                    "[NotificationAppender.append] Concurrent duplicate rejected by unique constraint. recipientId={}, dedupeKey={}",
                    notification.getRecipientId(),
                    notification.getDedupeKey());

            return Optional.empty();
        }
    }
}
