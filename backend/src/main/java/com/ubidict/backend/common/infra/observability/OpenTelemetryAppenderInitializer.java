package com.ubidict.backend.common.infra.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;

/**
 * {@code logback-spring.xml}의 {@code OTEL} appender에 {@link OpenTelemetry} 인스턴스를 꽂는다.
 *
 * <p>Logback은 Spring 컨텍스트보다 먼저 뜨므로 appender가 스스로 {@link OpenTelemetry}를 얻을 수
 * 없다. Spring Boot는 OTLP 로그 export(SDK {@code LoggerProvider})까지는 자동 구성하지만 appender
 * 설치는 해 주지 않으므로, 컨텍스트가 준비된 뒤 이 빈이 대신 꽂는다. 설치 전까지 쌓인 로그는
 * appender가 버퍼에 들고 있다가 이 시점에 흘려보낸다.
 *
 * <p>전송이 꺼져 있어도 무해하다 — 그때 주입되는 {@link OpenTelemetry}는 아무 데도 내보내지 않는
 * 구현이고, 로그는 {@code CONSOLE} appender로 그대로 나간다.
 */
@RequiredArgsConstructor
@Configuration
class OpenTelemetryAppenderInitializer implements InitializingBean {

    private final OpenTelemetry openTelemetry;

    @Override
    public void afterPropertiesSet() {
        OpenTelemetryAppender.install(openTelemetry);
    }
}
