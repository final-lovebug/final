package com.ubidict.backend.notification.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.exception.NotificationErrorCode;
import com.ubidict.backend.notification.infra.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationReader {

    private final NotificationRepository notificationRepository;

    /**
     * 수신자 본인의 알림만 돌려준다. 남의 알림은 <b>403이 아니라 404</b>다 — 코드가 갈리면 그 자체로 존재가 드러난다.
     */
    public Notification readOwned(Long notificationId, Long memberId) {
        Notification notification = notificationRepository
                .findByIdAndDeletedAtIsNull(notificationId)
                .orElseThrow(() -> new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));

        if (!notification.isRecipient(memberId)) {
            throw new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
        }

        return notification;
    }

    public PageResult<Notification> read(Long memberId, Long workspaceId, boolean unreadOnly, Pageable pageable) {
        Page<Notification> page = unreadOnly
                ? notificationRepository.findAllByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
                        memberId, workspaceId, pageable)
                : notificationRepository.findAllByRecipientIdAndWorkspaceIdAndDeletedAtIsNull(
                        memberId, workspaceId, pageable);

        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public long countUnread(Long memberId, Long workspaceId) {
        return notificationRepository.countByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
                memberId, workspaceId);
    }
}
