package com.ubidict.backend.revisionlog.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@RequiredArgsConstructor
public enum RevisionLogErrorCode implements ErrorCode {
    REVISION_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "개정 이력을 찾을 수 없습니다.");

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
