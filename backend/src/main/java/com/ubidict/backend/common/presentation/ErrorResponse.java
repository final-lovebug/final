package com.ubidict.backend.common.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ubidict.backend.common.exception.ErrorCode;
import java.util.List;

public record ErrorResponse(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ValidationError> errors) {

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.code(), errorCode.message(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ValidationError> errors) {
        return new ErrorResponse(errorCode.code(), errorCode.message(), List.copyOf(errors));
    }

    public record ValidationError(String field, String message) {}
}
