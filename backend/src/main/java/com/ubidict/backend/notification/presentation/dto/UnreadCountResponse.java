package com.ubidict.backend.notification.presentation.dto;

/**
 * 헤더 벨의 표시용. 목록을 받아 세지 않게 하려고 따로 둔다.
 */
public record UnreadCountResponse(long unreadCount) {

    public static UnreadCountResponse of(long unreadCount) {
        return new UnreadCountResponse(unreadCount);
    }
}
