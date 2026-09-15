package com.ubidict.backend.notification.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum NotificationErrorCode implements ErrorCode {
    /**
     * 존재하지만 내 것이 아닌 알림에도 이 코드를 쓴다.
     *
     * <p>「내 알림이 아님」에 별도 코드를 두면 코드가 갈리는 것 자체로 그 알림의 존재가 드러난다. 워크스페이스가 비참여자에게 404를 주는 것과 같은 이유다.
     */
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "알림을 찾을 수 없습니다."),
    NOTIFICATION_RECIPIENT_REQUIRED(HttpStatus.BAD_REQUEST, "알림 수신자가 필요합니다."),
    NOTIFICATION_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "알림 문구가 올바르지 않습니다."),
    NOTIFICATION_INVALID_DEDUPE_KEY(HttpStatus.BAD_REQUEST, "알림 중복 방지 키가 올바르지 않습니다.");

    private final HttpStatus status;
    private final String message;

    @Override
    public HttpStatus status() {
        return status;
    }

    @Override
    public String message() {
        return message;
    }
}
