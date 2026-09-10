package com.ubidict.backend.document.exception;

import com.ubidict.backend.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * DocumentErrorCode와 나누는 이유는 EXCEPTION.md의 {DOMAIN}_{REASON} 규칙 때문이다. 한 enum에 두 접두사를 섞으면 enum 이름과 코드가 어긋난다.
 *
 * <p>문서당 라벨 개수 상한은 문서 쪽 제약이므로 DOCUMENT_LABEL_LIMIT_EXCEEDED로 DocumentErrorCode에 둔다.
 */
@RequiredArgsConstructor
public enum LabelErrorCode implements ErrorCode {
    LABEL_INVALID_NAME(HttpStatus.BAD_REQUEST, "라벨 이름은 1자 이상 20자 이하여야 합니다.");

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
