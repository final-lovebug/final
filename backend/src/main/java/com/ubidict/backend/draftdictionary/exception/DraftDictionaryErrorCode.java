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
    DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT(HttpStatus.BAD_REQUEST, "유래 문서가 중복되었습니다."),
    DRAFT_DICTIONARY_INVALID_FORM(HttpStatus.BAD_REQUEST, "후보어 표기가 올바르지 않습니다."),
    DRAFT_DICTIONARY_INVALID_OCCURRENCE_COUNT(HttpStatus.BAD_REQUEST, "출현 횟수가 올바르지 않습니다."),
    DRAFT_DICTIONARY_DUPLICATE_CANDIDATE_FORM(HttpStatus.CONFLICT, "이미 등록된 표기의 후보어입니다."),
    DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_FOUND(HttpStatus.NOT_FOUND, "후보어를 찾을 수 없습니다."),
    DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_EXAMINABLE(HttpStatus.CONFLICT, "교정 중인 후보어만 수정할 수 있습니다."),
    DRAFT_DICTIONARY_CANDIDATE_TERM_MISMATCHED(HttpStatus.BAD_REQUEST, "해당 초안의 후보어가 아닙니다.");

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
