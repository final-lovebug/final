package com.ubidict.backend.draftdictionary.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum DraftDictionaryErrorCode implements ErrorCode {
    DRAFT_DICTIONARY_NOT_FOUND(HttpStatus.NOT_FOUND, "사전 초안을 찾을 수 없습니다."),
    DRAFT_DICTIONARY_SOURCE_DOCUMENT_REQUIRED(HttpStatus.BAD_REQUEST, "유래 문서는 최소 1건이 필요합니다."),
    DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT(HttpStatus.BAD_REQUEST, "유래 문서가 중복되었습니다.");

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
