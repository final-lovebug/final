package com.ubidict.backend.common.presentation;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.common.exception.ErrorCode;
import com.ubidict.backend.common.presentation.ErrorResponse.ValidationError;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.errorCode();
        log.warn(
                "[GlobalExceptionHandler.handleBusinessException] Business exception occurred. code={}, detail={}",
                errorCode.code(),
                e.getMessage());

        return toResponse(errorCode);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        List<ValidationError> errors = e.getBindingResult().getAllErrors().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();
        log.warn("[GlobalExceptionHandler.handleMethodArgumentNotValid] Request validation failed. errors={}", errors);

        return toResponse(CommonErrorCode.COMMON_INVALID_REQUEST, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        List<ValidationError> errors = e.getConstraintViolations().stream()
                .map(GlobalExceptionHandler::toValidationError)
                .toList();
        log.warn("[GlobalExceptionHandler.handleConstraintViolation] Request constraint violated. errors={}", errors);

        return toResponse(CommonErrorCode.COMMON_INVALID_REQUEST, errors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException e) {
        List<ValidationError> errors = e.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream().map(error -> toValidationError(result, error)))
                .toList();
        log.warn(
                "[GlobalExceptionHandler.handleHandlerMethodValidation] Request parameter validation failed. errors={}",
                errors);

        return toResponse(CommonErrorCode.COMMON_INVALID_REQUEST, errors);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException e) {
        log.warn("[GlobalExceptionHandler.handleAuthentication] Authentication failed. detail={}", e.getMessage());

        return toResponse(CommonErrorCode.COMMON_UNAUTHORIZED);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        log.warn("[GlobalExceptionHandler.handleAccessDenied] Access denied. detail={}", e.getMessage());

        return toResponse(CommonErrorCode.COMMON_ACCESS_DENIED);
    }

    @ExceptionHandler({
        HttpMessageNotReadableException.class,
        MissingServletRequestParameterException.class,
        MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResponse> handleMalformedRequest(Exception e) {
        log.warn(
                "[GlobalExceptionHandler.handleMalformedRequest] Malformed request. type={}, detail={}",
                e.getClass().getSimpleName(),
                e.getMessage());

        return toResponse(CommonErrorCode.COMMON_INVALID_REQUEST);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn(
                "[GlobalExceptionHandler.handleMethodNotSupported] Unsupported request method. method={}",
                e.getMethod());

        return toResponse(CommonErrorCode.COMMON_METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(NoResourceFoundException e) {
        log.warn("[GlobalExceptionHandler.handleNoResourceFound] No resource found. path={}", e.getResourcePath());

        return toResponse(CommonErrorCode.COMMON_RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("[GlobalExceptionHandler.handleException] Unexpected exception occurred.", e);

        return toResponse(CommonErrorCode.COMMON_INTERNAL_ERROR);
    }

    private static ValidationError toValidationError(ObjectError error) {
        String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();

        return new ValidationError(field, error.getDefaultMessage());
    }

    private static ValidationError toValidationError(ParameterValidationResult result, MessageSourceResolvable error) {
        String field = error instanceof FieldError fieldError
                ? fieldError.getField()
                : result.getMethodParameter().getParameterName();

        return new ValidationError(field, error.getDefaultMessage());
    }

    private static ValidationError toValidationError(ConstraintViolation<?> violation) {
        return new ValidationError(lastPathNode(violation), violation.getMessage());
    }

    /**
     * 메서드 파라미터 검증 실패의 propertyPath는 "method.parameter" 형태이므로 마지막 노드만 노출한다.
     */
    private static String lastPathNode(ConstraintViolation<?> violation) {
        String propertyPath = violation.getPropertyPath().toString();

        return propertyPath.substring(propertyPath.lastIndexOf('.') + 1);
    }

    private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.from(errorCode));
    }

    private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, List<ValidationError> errors) {
        return ResponseEntity.status(errorCode.status()).body(ErrorResponse.of(errorCode, errors));
    }
}
