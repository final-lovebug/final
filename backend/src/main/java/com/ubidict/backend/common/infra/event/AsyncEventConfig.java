package com.ubidict.backend.common.infra.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * 비동기 이벤트 처리 설정.
 *
 * <p>이벤트 리스너는 발행 트랜잭션과 분리하기 위해 {@code @TransactionalEventListener(AFTER_COMMIT)}와 {@code @Async}를 함께 사용한다.
 */
@Slf4j
@EnableAsync
@Configuration
class AsyncEventConfig implements AsyncConfigurer {

    /**
     * 반환값이 없는 {@code @Async} 메서드에서 던진 예외는 호출자에게 전파되지 않고 사라진다.
     *
     * <p>외부 메시지 큐라면 재시도 후 DLQ로 갔을 실패가 로컬에서는 아무 흔적 없이 지나가므로, 최소한 ERROR 로그로 남겨 두 환경의 차이를 줄인다.
     */
    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (exception, method, params) -> log.error(
                "[AsyncEventConfig.getAsyncUncaughtExceptionHandler] Failed to handle event. handler={}.{}",
                method.getDeclaringClass().getSimpleName(),
                method.getName(),
                exception);
    }
}
