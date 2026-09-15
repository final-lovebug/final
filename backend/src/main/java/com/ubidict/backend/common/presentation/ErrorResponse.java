package com.ubidict.backend.common.presentation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.ubidict.backend.common.exception.ErrorCode;
import java.util.List;
import org.slf4j.MDC;

/**
 * 모든 에러 응답의 표현({@code docs/API.md}·{@code docs/EXCEPTION.md}).
 *
 * <p>{@code traceId}는 그 요청의 추적 식별자다({@code NFR-CMN-003}·{@code D-96}). 이 값으로 응답 →
 * 로그 → 트레이스가 한 ID로 이어지므로 문의·장애 신고에는 이 값을 함께 받는다. <b>예외 핸들러가
 * 챙기지 않는다</b> — 아래 두 팩토리가 이 레코드를 만드는 유일한 통로라 여기서 한 번 읽으면 핸들러
 * 전체에 적용된다.
 *
 * <p>값은 OpenTelemetry 계측이 시작한 trace를 Micrometer Tracing이 MDC에 넣어 둔 것을 그대로 쓴다.
 * 따로 만들지 않는 이유는 ID가 둘 공존하면 로그와 트레이스가 오히려 끊기기 때문이다. 텔레메트리
 * 전송이 꺼져 있어도 이 값은 있다 — 익스포터와 달리 {@code Tracer}는 전송 여부와 무관하게 만들어진다.
 *
 * <p>추적 문맥이 없는 경로(스케줄러, 컨텍스트 밖 호출)에서는 {@code null}이라 응답에서 통째로
 * 빠진다. 클라이언트는 이 필드가 없을 수 있다고 보고 다뤄야 한다.
 */
public record ErrorResponse(
        String code,
        String message,
        @JsonInclude(JsonInclude.Include.NON_EMPTY) List<ValidationError> errors,
        @JsonInclude(JsonInclude.Include.NON_NULL) String traceId) {

    /** Micrometer Tracing이 MDC에 넣는 키. 이 이름은 Boot의 로그 패턴이 쓰는 것과 같다. */
    private static final String TRACE_ID_KEY = "traceId";

    public static ErrorResponse from(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.code(), errorCode.message(), List.of(), currentTraceId());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<ValidationError> errors) {
        return new ErrorResponse(errorCode.code(), errorCode.message(), List.copyOf(errors), currentTraceId());
    }

    private static String currentTraceId() {
        return MDC.get(TRACE_ID_KEY);
    }

    public record ValidationError(String field, String message) {}
}
