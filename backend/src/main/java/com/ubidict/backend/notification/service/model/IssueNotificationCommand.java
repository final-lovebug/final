package com.ubidict.backend.notification.service.model;

import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.notification.implement.NotificationContent;
import java.util.List;

/**
 * 한 사건을 여러 수신자에게 알리라는 요청.
 *
 * <p>사건마다 달라지는 것은 <b>수신자·문구·중복 방지 키·행위자</b> 넷이고, 그 넷을 정하는 것이 알림 도메인의 비즈니스 판단이다. 그래서 이 커맨드는
 * 서비스가 채우고 {@link com.ubidict.backend.notification.implement.NotificationIssuer}가 받는다.
 *
 * @param targetId 눌렀을 때 이동할 대상. 현재 5종이 모두 리뷰 요청이라 {@code reviewRequestId}가 들어온다
 * @param dedupeKey 이벤트 내용에서 결정론적으로 파생한 키. 같은 사건이 재수신되면 같은 값이라 알림이 두 번 생기지 않는다
 * @param actorId 알림을 유발한 행위자. 이벤트에 담기지 않았으면 null이며 그때는 {@link #withoutActor}를 쓴다
 */
public record IssueNotificationCommand(
        Long workspaceId,
        Long targetId,
        NotificationType type,
        List<Long> recipients,
        NotificationContent content,
        String dedupeKey,
        Long actorId) {

    public IssueNotificationCommand {
        recipients = recipients == null ? List.of() : List.copyOf(recipients);
    }

    /**
     * 행위자를 <b>담지 않은</b> 이벤트용 — 변경요청·반영완료·취소 셋이 그렇다.
     *
     * <p>「시스템이 만들었다」는 뜻이 아니라 <b>이벤트 페이로드에 행위자가 없다</b>는 뜻이다. 호출부가 맨 {@code null}을 적지 않게 한다.
     */
    public static IssueNotificationCommand withoutActor(
            Long workspaceId,
            Long targetId,
            NotificationType type,
            List<Long> recipients,
            NotificationContent content,
            String dedupeKey) {
        return new IssueNotificationCommand(workspaceId, targetId, type, recipients, content, dedupeKey, null);
    }
}
