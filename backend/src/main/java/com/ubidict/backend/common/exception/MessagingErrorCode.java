package com.ubidict.backend.common.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * 메시징 경계에서 나는 실패. 큐에 실을 메시지를 만들거나 되돌리지 못한 경우다.
 *
 * <p><b>HTTP 상태는 사실상 쓰이지 않는다.</b> 이 실패는 요청-응답이 아니라 이벤트 처리 경로에서 나므로
 * {@code GlobalExceptionHandler}까지 가지 않는다. 그래도 {@link ErrorCode} 계약이 상태를 요구하고, 성격상 클라이언트 잘못이 아니라
 * 서버 쪽 문제이므로 500을 쓴다.
 *
 * <p>수신 측에서 이 예외가 나가면 메시지가 확인(ack)되지 않아 SQS가 재시도하고, 끝내 실패하면 DLQ로 간다. 망가진 메시지를 조용히 삼키지 않기 위해 일부러
 * 던진다({@code NFR-MSG-004}).
 */
@RequiredArgsConstructor
public enum MessagingErrorCode implements ErrorCode {
    MESSAGING_EVENT_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트를 메시지로 만들지 못했습니다."),
    MESSAGING_EVENT_DESERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "메시지를 이벤트로 되돌리지 못했습니다."),
    MESSAGING_EVENT_HANDLER_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트를 처리할 수 없습니다."),
    MESSAGING_LLM_REQUEST_PUBLISH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "AI 작업 요청을 발행하지 못했습니다.");

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
