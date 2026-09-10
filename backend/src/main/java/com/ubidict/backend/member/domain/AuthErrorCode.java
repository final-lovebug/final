package com.ubidict.backend.member.domain;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {
    AUTH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다."),
    AUTH_TOKEN_HASH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "토큰 처리 중 오류가 발생했습니다.");

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
