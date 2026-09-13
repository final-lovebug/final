package com.ubidict.backend.notification.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.notification.presentation.dto.MarkAllReadResponse;
import com.ubidict.backend.notification.presentation.dto.NotificationResponse;
import com.ubidict.backend.notification.presentation.dto.UnreadCountResponse;
import com.ubidict.backend.notification.service.NotificationService;
import com.ubidict.backend.notification.service.model.NotificationSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 알림 조회와 읽음 처리.
 *
 * <p><b>POST가 없다.</b> 알림은 HTTP로 만들어지지 않고 도메인 이벤트로만 생긴다.
 *
 * <p>목록은 언제나 요청자 본인의 것이다 — 수신자를 파라미터로 받지 않는다. 남의 알림에는 403이 아니라 404를 준다(존재를 숨긴다).
 *
 * <p>요청자 memberId를 요청 파라미터로 받는다. 인증 계층이 아직 없어 생긴 임시 방식이며 인증 도입 전까지 운영 배포 대상이 아니다. 알림은 수신자 본인만 보는
 * 자원이라 이 파라미터를 신뢰하는 것이 특히 위험하다.
 *
 * <p>TODO(NFR-USR-001): 인증이 들어오면 memberId 파라미터를 걷어내고 인증 주체에서 해석한다.
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> search(
            @PathVariable Long workspaceId,
            @RequestParam Long memberId,
            @RequestParam(required = false) Boolean unreadOnly,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sort) {
        NotificationSearchQuery query = NotificationSearchQuery.of(workspaceId, memberId, unreadOnly, page, size, sort);

        return ResponseEntity.ok(
                PageResponse.from(notificationService.search(query).map(NotificationResponse::from)));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadCountResponse> unreadCount(
            @PathVariable Long workspaceId, @RequestParam Long memberId) {
        return ResponseEntity.ok(UnreadCountResponse.of(notificationService.countUnread(workspaceId, memberId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long workspaceId, @PathVariable Long notificationId, @RequestParam Long memberId) {
        return ResponseEntity.ok(
                NotificationResponse.from(notificationService.markRead(workspaceId, notificationId, memberId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllRead(
            @PathVariable Long workspaceId, @RequestParam Long memberId) {
        return ResponseEntity.ok(MarkAllReadResponse.of(notificationService.markAllRead(workspaceId, memberId)));
    }
}
