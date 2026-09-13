package com.ubidict.backend.notification.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.implement.NotificationReader;
import com.ubidict.backend.notification.implement.NotificationUpdater;
import com.ubidict.backend.notification.service.model.NotificationResult;
import com.ubidict.backend.notification.service.model.NotificationSearchQuery;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 알림 조회와 읽음 처리.
 *
 * <p><b>생성 메서드가 없다.</b> 알림은 HTTP가 아니라 도메인 이벤트로만 만들어진다 — {@link NotificationEventHandler}의 몫이다.
 *
 * <p>참여 검증은 {@code WorkspaceAccessValidator}를 직접 주입해 한다(D-19 예외). 비참여 워크스페이스는 404다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationReader notificationReader;
    private final NotificationUpdater notificationUpdater;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional(readOnly = true)
    public PageResult<NotificationResult> search(NotificationSearchQuery query) {
        workspaceAccessValidator.validateParticipant(query.workspaceId(), query.memberId());

        return notificationReader
                .read(query.memberId(), query.workspaceId(), query.unreadOnly(), query.toPageable())
                .map(NotificationResult::from);
    }

    @Transactional(readOnly = true)
    public long countUnread(Long workspaceId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        return notificationReader.countUnread(memberId, workspaceId);
    }

    /**
     * 읽음 처리. 이미 읽었으면 아무것도 바뀌지 않고 그대로 돌려준다 — 멱등이다.
     */
    @Transactional
    public NotificationResult markRead(Long workspaceId, Long notificationId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        Notification notification = notificationReader.readOwned(notificationId, memberId);
        notificationUpdater.markRead(notification);

        return NotificationResult.from(notification);
    }

    /**
     * @return 실제로 읽음으로 바뀐 개수
     */
    @Transactional
    public int markAllRead(Long workspaceId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        int updated = notificationUpdater.markAllRead(memberId, workspaceId);

        log.info(
                "[NotificationService.markAllRead] Notifications marked read. workspaceId={}, memberId={}, updated={}",
                workspaceId,
                memberId,
                updated);

        return updated;
    }
}
