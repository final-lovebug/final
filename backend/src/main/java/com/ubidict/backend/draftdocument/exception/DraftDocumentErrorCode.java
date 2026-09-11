package com.ubidict.backend.draftdocument.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum DraftDocumentErrorCode implements ErrorCode {
    DRAFT_DOCUMENT_SUGGESTION_TERM_MISMATCHED(
            HttpStatus.BAD_REQUEST, "Suggestion term does not belong to draft document"),
    DRAFT_DOCUMENT_INVALID_ANCHOR(HttpStatus.BAD_REQUEST, "Invalid anchor"),
    DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY(HttpStatus.BAD_REQUEST, "Anchor is outside draft body"),
    DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM(HttpStatus.BAD_REQUEST, "Suggestion term is required"),
    DRAFT_DOCUMENT_SUGGESTION_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "Suggestion term not found"),
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
