package com.ubidict.backend.notification.presentation;

import com.ubidict.backend.common.presentation.PageResponse;
import com.ubidict.backend.notification.presentation.dto.MarkAllReadResponse;
import com.ubidict.backend.notification.presentation.dto.NotificationResponse;
import com.ubidict.backend.notification.presentation.dto.UnreadCountResponse;
import com.ubidict.backend.notification.service.NotificationService;
import com.ubidict.backend.notification.service.model.NotificationSearchQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<PageResponse<NotificationResponse>> search(
            @PathVariable Long workspaceId,
            @AuthenticationPrincipal Long memberId,
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
            @PathVariable Long workspaceId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(UnreadCountResponse.of(notificationService.countUnread(workspaceId, memberId)));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable Long workspaceId, @PathVariable Long notificationId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(
                NotificationResponse.from(notificationService.markRead(workspaceId, notificationId, memberId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<MarkAllReadResponse> markAllRead(
            @PathVariable Long workspaceId, @AuthenticationPrincipal Long memberId) {
        return ResponseEntity.ok(MarkAllReadResponse.of(notificationService.markAllRead(workspaceId, memberId)));
    }
}
