package com.ubidict.backend.workspace.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum InvitationErrorCode implements ErrorCode {
    INVITATION_NOT_FOUND(HttpStatus.NOT_FOUND, "초대를 찾을 수 없습니다."),
    INVITATION_NOT_ACCEPTABLE(HttpStatus.CONFLICT, "만료되거나 이미 처리된 초대입니다."),
    INVITATION_ALREADY_PARTICIPANT(HttpStatus.CONFLICT, "이미 참여 중인 회원입니다."),
    INVITATION_DUPLICATE_PENDING(HttpStatus.CONFLICT, "같은 대상에 대기 중인 초대가 이미 있습니다."),
    INVITATION_OWNER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "초대로는 소유자 권한을 부여할 수 없습니다.");

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
