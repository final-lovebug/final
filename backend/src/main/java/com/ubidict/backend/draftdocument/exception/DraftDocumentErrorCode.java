package com.ubidict.backend.draftdocument.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum DraftDocumentErrorCode implements ErrorCode {
    DRAFT_DOCUMENT_SUGGESTION_TERM_MISMATCHED(HttpStatus.BAD_REQUEST, "해당 초안의 제안어가 아닙니다."),
    DRAFT_DOCUMENT_INVALID_ANCHOR(HttpStatus.BAD_REQUEST, "제안어 위치가 올바르지 않습니다."),
    DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY(HttpStatus.BAD_REQUEST, "제안어 위치가 본문 범위를 벗어났습니다."),
    DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM(HttpStatus.BAD_REQUEST, "제안 용어가 올바르지 않습니다."),
    DRAFT_DOCUMENT_SUGGESTION_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "제안어를 찾을 수 없습니다."),
    DRAFT_DOCUMENT_REJECT_REASON_REQUIRED(HttpStatus.BAD_REQUEST, "거절 사유는 필수입니다."),
    DRAFT_DOCUMENT_SUGGESTION_TERM_UNHANDLED_EXISTS(HttpStatus.CONFLICT, "처리하지 않은 제안어가 남아 있습니다."),
    DRAFT_DOCUMENT_ALREADY_EXAMINED(HttpStatus.CONFLICT, "이미 교정을 완료한 초안입니다."),
    DRAFT_DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "문서 초안을 찾을 수 없습니다."),
    DRAFT_DOCUMENT_INVALID_BODY(HttpStatus.BAD_REQUEST, "초안 본문은 비어 있을 수 없습니다."),
    DRAFT_DOCUMENT_INVALID_BASE_VERSION(HttpStatus.BAD_REQUEST, "기준 문서 버전이 올바르지 않습니다.");

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
