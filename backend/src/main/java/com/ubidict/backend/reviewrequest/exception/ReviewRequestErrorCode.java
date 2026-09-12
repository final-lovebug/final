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
    REVIEW_REQUEST_NOT_REQUESTER(HttpStatus.FORBIDDEN, "요청자만 수행할 수 있습니다."),
    REVIEW_REQUEST_WORKSPACE_ID_REQUIRED(HttpStatus.BAD_REQUEST, "워크스페이스 식별자는 필수입니다."),
    REVIEW_REQUEST_REVIEWER_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰어를 찾을 수 없습니다."),
    REVIEW_REQUEST_DUPLICATE_REVIEWER(HttpStatus.CONFLICT, "이미 지정된 리뷰어입니다."),
    REVIEW_REQUEST_REVISION_NOT_FOUND(HttpStatus.NOT_FOUND, "개정안을 찾을 수 없습니다."),
    REVIEW_REQUEST_REVISION_ALREADY_EXISTS(HttpStatus.CONFLICT, "해당 회차의 개정안이 이미 있습니다."),
    REVIEW_REQUEST_TYPE_MISMATCHED(HttpStatus.BAD_REQUEST, "요청 유형과 개정안 종류가 맞지 않습니다."),
    REVIEW_REQUEST_NOT_REVIEWABLE_STATUS(HttpStatus.CONFLICT, "리뷰할 수 없는 상태입니다."),
    REVIEW_REQUEST_STALE_TARGET_ROUND(HttpStatus.BAD_REQUEST, "대상 회차가 올바르지 않습니다."),
    REVIEW_REQUEST_REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."),
    REVIEW_REQUEST_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "코멘트를 찾을 수 없습니다."),
    REVIEW_REQUEST_COMMENT_CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "코멘트 내용은 필수입니다."),
    REVIEW_REQUEST_INVALID_COMMENT_PARENT(HttpStatus.BAD_REQUEST, "상위 코멘트가 올바르지 않습니다.");

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
