package com.ubidict.backend.member.domain;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 회원을 찾을 수 없습니다."),
    MEMBER_DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    MEMBER_DUPLICATE_SOCIAL_ACCOUNT(HttpStatus.CONFLICT, "이미 다른 소셜 계정으로 가입된 이메일입니다."),
    MEMBER_LOGIN_NOT_ALLOWED(HttpStatus.FORBIDDEN, "로그인할 수 없는 회원 상태입니다.");

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
