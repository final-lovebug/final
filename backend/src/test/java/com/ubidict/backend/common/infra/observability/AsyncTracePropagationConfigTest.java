package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.context.ContextRegistry;
import java.util.concurrent.Future;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * {@code @Async}가 쓰는 {@code applicationTaskExecutor}가 실제로 호출 스레드의 컨텍스트를 실어
 * 나르는지 본다.
 *
 * <p>추적 컨텍스트 대신 <b>테스트가 직접 등록한 {@link ThreadLocal}</b>로 확인한다. 검증하려는
 * 것은 「추적이 되는가」가 아니라 <b>「실행기가 작업을 데코레이터로 감싸는가」</b>이고, 그것이
 * 이 설정이 책임지는 전부이기 때문이다. 실제 추적 컨텍스트도 같은 경로로 넘어간다.
 */
class AsyncTracePropagationConfigTest {

    private static final String ACCESSOR_KEY = "asyncTracePropagationConfigTest";
    private static final ThreadLocal<String> CARRIED = new ThreadLocal<>();

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
            .withUserConfiguration(AsyncTracePropagationConfig.class);

    @DisplayName("비동기 실행기에 넘긴 작업이 호출 스레드의 컨텍스트를 그대로 본다.")
    @Test
    void contextIsPropagated() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(ACCESSOR_KEY, CARRIED);
        try {
            contextRunner.run(context -> {
                // given
                AsyncTaskExecutor executor = context.getBean(AsyncTaskExecutor.class);
                CARRIED.set("carried-across-threads");

                // when
                Future<String> result = executor.submit(CARRIED::get);

                // then
                assertThat(result.get()).isEqualTo("carried-across-threads");
            });
        } finally {
            CARRIED.remove();
            ContextRegistry.getInstance().removeThreadLocalAccessor(ACCESSOR_KEY);
        }
    }

    @DisplayName("데코레이터가 없으면 컨텍스트가 넘어가지 않는다.")
    @Test
    void contextIsLostWithoutDecorator() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(ACCESSOR_KEY, CARRIED);
        try {
            new ApplicationContextRunner()
                    .withConfiguration(AutoConfigurations.of(TaskExecutionAutoConfiguration.class))
                    .run(context -> {
                        // given
                        AsyncTaskExecutor executor = context.getBean(AsyncTaskExecutor.class);
                        CARRIED.set("carried-across-threads");

                        // when
                        Future<String> result = executor.submit(CARRIED::get);

                        // then — 이 테스트가 깨지면 위 테스트가 무엇도 증명하지 못한다는 뜻이다
                        assertThat(result.get()).isNull();
                    });
        } finally {
            CARRIED.remove();
            ContextRegistry.getInstance().removeThreadLocalAccessor(ACCESSOR_KEY);
        }
    }
}
