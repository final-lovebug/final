package com.ubidict.backend.dictionary.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 권한 부족은 워크스페이스 권한이 모자란 것이므로 WorkspaceErrorCode를 그대로 쓴다. 사전집 전용 권한 코드를 새로 만들지 않는다.
 */
@RequiredArgsConstructor
public enum DictionaryErrorCode implements ErrorCode {
    /**
     * 워크스페이스에 사전집이 없는 기간은 정상이다. 첫 반영 전까지가 그 상태이며 오류 상황이 아니다.
     */
    DICTIONARY_NOT_FOUND(HttpStatus.NOT_FOUND, "사전집을 찾을 수 없습니다."),
    DICTIONARY_EMPTY_TERMS(HttpStatus.BAD_REQUEST, "용어가 없는 사전집 버전은 만들 수 없습니다."),
    DICTIONARY_INVALID_VERSION(HttpStatus.BAD_REQUEST, "사전집 버전 정보가 올바르지 않습니다."),
    DICTIONARY_VERSION_CONFLICT(HttpStatus.CONFLICT, "사전집 버전이 현재 버전과 일치하지 않습니다.");

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
