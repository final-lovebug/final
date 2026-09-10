package com.ubidict.backend.document.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 권한 부족은 워크스페이스 권한이 모자란 것이므로 WorkspaceErrorCode를 그대로 쓴다. 문서 전용 권한 코드를 새로 만들지 않는다.
 */
@RequiredArgsConstructor
public enum DocumentErrorCode implements ErrorCode {
    /**
     * 다른 워크스페이스의 문서 식별자에도 이 코드를 쓴다. 남의 워크스페이스에 그 문서가 있다는 사실을 드러내지 않는다.
     */
    DOCUMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."),
    DOCUMENT_INVALID_TITLE(HttpStatus.BAD_REQUEST, "문서 제목은 1자 이상 200자 이하여야 합니다."),
    DOCUMENT_INVALID_CONTENT(HttpStatus.BAD_REQUEST, "문서 본문은 1자 이상 10,000자 이하여야 합니다."),
    DOCUMENT_INVALID_VERSION(HttpStatus.BAD_REQUEST, "문서 버전 정보가 올바르지 않습니다."),
    DOCUMENT_VERSION_NOT_FOUND(HttpStatus.NOT_FOUND, "문서 버전을 찾을 수 없습니다."),
    DOCUMENT_LABEL_LIMIT_EXCEEDED(HttpStatus.BAD_REQUEST, "문서에 붙일 수 있는 라벨은 최대 5개입니다.");

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
