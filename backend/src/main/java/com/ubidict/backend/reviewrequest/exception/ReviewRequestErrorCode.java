package com.ubidict.backend.reviewrequest.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum ReviewRequestErrorCode implements ErrorCode {
    REVIEW_REQUEST_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰 요청을 찾을 수 없습니다."),
    REVIEW_REQUEST_TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "제목은 필수입니다."),
    REVIEW_REQUEST_INVALID_TYPE(HttpStatus.BAD_REQUEST, "요청 유형이 올바르지 않습니다."),
    REVIEW_REQUEST_INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "현재 상태에서 수행할 수 없는 작업입니다."),
    REVIEW_REQUEST_NOT_REQUESTER(HttpStatus.FORBIDDEN, "요청자만 수행할 수 있습니다.");

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
