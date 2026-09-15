package com.ubidict.backend.common.infra.observability;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.core.task.support.ContextPropagatingTaskDecorator;

/**
 * {@code @Async} 경계 너머로 추적 컨텍스트를 잇는다.
 *
 * <p>이것이 없으면 {@code @TransactionalEventListener(AFTER_COMMIT)} + {@code @Async}로 도는 이벤트
 * 리스너가 전부 새 trace로 시작한다 — 알림 발행, 초안 상태 전이, AI 작업 발행이 모두 그 경로라
 * 요청을 따라가다 커밋 직후에 끊긴다. 로그의 {@code traceId}도 거기서부터 달라진다.
 *
 * <p>Spring Boot는 {@code TaskDecorator} 빈이 하나 있으면 {@code applicationTaskExecutor} 빌더에
 * 그대로 물려 준다. {@code spring.task.execution.propagate-context} 프로퍼티로 같은 일을 할 수
 * 있게 된 것은 4.1부터라 여기서는 빈으로 등록한다.
 *
 * <p><b>이 {@code @Bean}을 {@code AsyncEventConfig}에 함께 두지 않는다.</b> 그쪽은
 * {@code AsyncConfigurer} 구현체라, 같은 클래스에 이 빈을 두면 {@code applicationTaskExecutor}를
 * 만드는 도중 자기 자신을 다시 요구하게 되어 {@code Illegal factory instance}로 컨텍스트 전체가
 * 기동에 실패한다.
 */
@Configuration
class AsyncTracePropagationConfig {

    @Bean
    TaskDecorator contextPropagatingTaskDecorator() {
        return new ContextPropagatingTaskDecorator();
    }
}
