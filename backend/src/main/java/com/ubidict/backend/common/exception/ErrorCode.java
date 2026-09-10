package com.ubidict.backend.common.exception;

import org.springframework.http.HttpStatus;

public interface ErrorCode {

    HttpStatus status();

    String message();

    /**
     * 구현체를 enum으로 제한해 응답 code와 상수 이름이 항상 일치하도록 강제한다.
     */
    String name();

    default String code() {
        return name();
    }
}
