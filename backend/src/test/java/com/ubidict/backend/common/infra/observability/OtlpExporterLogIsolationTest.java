package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.ubidict.backend.support.IntegrationTestSupport;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * 익스포터 자신의 로그가 OTLP로 다시 나가지 않는지 본다.
 *
 * <p>나가면 되먹임이 생긴다 — export 실패 로그가 다시 export 대상이 되고, 그 export가 또 실패해
 * 로그를 낳는다. {@code logback-spring.xml}의 {@code additivity="false"}가 그 고리를 끊는 유일한
 * 장치이며, 그 한 줄이 지워져도 아무 테스트도 깨지지 않으므로 여기서 못 박는다.
 *
 * <p><b>레벨로 끊으려 하지 않는다.</b> {@code HttpExporter}는 연결 실패를 {@code SEVERE}로, HTTP
 * 상태코드 실패(Grafana Cloud의 401 등)를 {@code WARNING}으로 찍는다. 로거 레벨을 {@code ERROR}로
 * 올리면 시끄러운 쪽은 그대로 나오고 진단에 필요한 쪽만 가려진다 — 실제로 그렇게 걸려 있었고
 * 로컬 확인에서 드러났다(2026-09-15).
 *
 * <p>{@code logback-spring.xml}은 Spring이 읽는 파일이라 순수 JUnit 테스트에서는 적용되지 않는다.
 * 그래서 공유 컨텍스트 위에서 본다.
 */
class OtlpExporterLogIsolationTest extends IntegrationTestSupport {

    private static final List<String> EXPORTER_LOGGERS =
            List.of("io.opentelemetry.exporter", "io.opentelemetry.sdk.internal", "io.micrometer.registry.otlp");

    @DisplayName("익스포터 로거는 상위로 전파되지 않고 CONSOLE 로만 나간다.")
    @Test
    void exporterLogsDoNotReachTheOtlpAppender() {
        assertThat(EXPORTER_LOGGERS).allSatisfy(name -> {
            Logger logger = loggerContext().getLogger(name);

            assertThat(logger.isAdditive())
                    .withFailMessage("%s 가 상위로 전파된다 — 익스포터 실패 로그가 다시 export 된다", name)
                    .isFalse();
            assertThat(appenderNames(logger))
                    .withFailMessage("%s 의 appender 가 CONSOLE 하나가 아니다", name)
                    .containsExactly("CONSOLE");
        });
    }

    @DisplayName("일반 로그는 CONSOLE 과 OTEL 로 함께 나간다.")
    @Test
    void ordinaryLogsStillReachTheOtlpAppender() {
        // 위 테스트가 「아무 로그도 OTEL 로 안 간다」는 이유로 통과하는 것을 막는다.
        assertThat(appenderNames(loggerContext().getLogger(Logger.ROOT_LOGGER_NAME)))
                .containsExactlyInAnyOrder("CONSOLE", "OTEL");
    }

    private LoggerContext loggerContext() {
        return (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    private List<String> appenderNames(Logger logger) {
        List<String> names = new ArrayList<>();
        for (Iterator<Appender<ILoggingEvent>> it = logger.iteratorForAppenders(); it.hasNext(); ) {
            names.add(it.next().getName());
        }
        return names;
    }
}
