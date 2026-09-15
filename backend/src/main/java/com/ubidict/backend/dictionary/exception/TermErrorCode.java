package com.ubidict.backend.dictionary.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * DictionaryErrorCode와 나누는 이유는 EXCEPTION.md의 {DOMAIN}_{REASON} 규칙 때문이다. 한 enum에 두 접두사를 섞으면 enum 이름과 코드가 어긋난다.
 */
@RequiredArgsConstructor
public enum TermErrorCode implements ErrorCode {
    TERM_INVALID_PREFERRED_FORM(HttpStatus.BAD_REQUEST, "표준어는 1자 이상 100자 이하여야 합니다."),
    TERM_INVALID_ENGLISH_NAME(HttpStatus.BAD_REQUEST, "영문명은 100자 이하여야 합니다."),
    TERM_INVALID_DEFINITION(HttpStatus.BAD_REQUEST, "정의는 비어 있을 수 없습니다."),
    TERM_DUPLICATE_PREFERRED_FORM(HttpStatus.CONFLICT, "같은 표준어를 두 번 등재할 수 없습니다.");

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
