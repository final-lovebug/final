package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.registry.otlp.OtlpMeterRegistry;
import io.opentelemetry.exporter.otlp.http.logs.OtlpHttpLogRecordExporter;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.metrics.autoconfigure.MetricsAutoConfiguration;
import org.springframework.boot.micrometer.metrics.autoconfigure.export.otlp.OtlpMetricsExportAutoConfiguration;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.opentelemetry.autoconfigure.logging.otlp.OtlpLoggingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * {@code application-prod.yml}이 쓰는 OTLP 프로퍼티 키가 실제로 익스포터를 만들어 내는지 본다.
 *
 * <p><b>키 오타 방어가 목적이다.</b> Spring Boot 3.x 표기({@code management.otlp.tracing.*} 등)는
 * deprecation level이 {@code error}라 쓰면 기동이 깨져 바로 드러나지만, <b>4.x 키의 오타는 조용히
 * 무시된다</b> — 텔레메트리가 안 나가는 것을 배포 후에야 알게 된다. 세 신호가 네임스페이스마저
 * 다르므로(트레이스·로그는 {@code management.opentelemetry.*}, 메트릭만
 * {@code management.otlp.metrics.*}) 사람이 눈으로 맞추기 어렵다.
 *
 * <p>컨테이너를 띄우지 않는다 — 익스포터 빈이 <b>만들어지는지</b>만 보면 되고 실제로 어디로
 * 보내는지는 보지 않는다.
 */
class OtlpPropertyBindingTest {

    private static final String ENDPOINT = "https://otlp-gateway.example.test/otlp";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MetricsAutoConfiguration.class,
                    OtlpTracingAutoConfiguration.class,
                    OtlpLoggingAutoConfiguration.class,
                    OtlpMetricsExportAutoConfiguration.class));

    @DisplayName("prod 가 쓰는 키를 주면 세 신호의 익스포터가 모두 만들어진다.")
    @Test
    void exportersAreCreated() {
        contextRunner
                .withPropertyValues(enabled(true))
                .withPropertyValues(endpointsAndHeaders())
                .run(context -> {
                    assertThat(context).hasSingleBean(OtlpHttpSpanExporter.class);
                    assertThat(context).hasSingleBean(OtlpHttpLogRecordExporter.class);
                    assertThat(context).hasSingleBean(OtlpMeterRegistry.class);
                });
    }

    @DisplayName("enabled 를 끄면 엔드포인트가 있어도 익스포터가 만들어지지 않는다.")
    @Test
    void exportersAreDisabled() {
        contextRunner
                .withPropertyValues(enabled(false))
                .withPropertyValues(endpointsAndHeaders())
                .run(context -> {
                    assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
                    assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
                    assertThat(context).doesNotHaveBean(OtlpMeterRegistry.class);
                });
    }

    /**
     * 트레이스·로그는 엔드포인트가 없으면 {@code *ConnectionDetails} 빈이 없어 저절로 꺼지지만,
     * <b>메트릭은 다르다</b> — 엔드포인트가 없어도 Micrometer의 {@code OtlpConfig} 기본값
     * {@code http://localhost:4318/v1/metrics}로 붙으려 한다. 그래서 끄는 수단이 {@code enabled}뿐이다.
     */
    @DisplayName("엔드포인트가 없으면 트레이스·로그는 저절로 꺼지지만 메트릭은 그렇지 않다.")
    @Test
    void metricsDoNotSwitchOffWithoutEndpoint() {
        contextRunner.withPropertyValues(enabled(true)).run(context -> {
            assertThat(context).doesNotHaveBean(OtlpHttpSpanExporter.class);
            assertThat(context).doesNotHaveBean(OtlpHttpLogRecordExporter.class);
            assertThat(context).hasSingleBean(OtlpMeterRegistry.class);
        });
    }

    private String[] enabled(boolean value) {
        return new String[] {
            "management.tracing.export.otlp.enabled=" + value,
            "management.logging.export.otlp.enabled=" + value,
            "management.otlp.metrics.export.enabled=" + value
        };
    }

    private String[] endpointsAndHeaders() {
        return new String[] {
            "management.opentelemetry.tracing.export.otlp.endpoint=" + ENDPOINT + "/v1/traces",
            "management.opentelemetry.tracing.export.otlp.transport=http",
            "management.opentelemetry.tracing.export.otlp.compression=gzip",
            "management.opentelemetry.tracing.export.otlp.headers.Authorization=Basic dGVzdA==",
            "management.opentelemetry.logging.export.otlp.endpoint=" + ENDPOINT + "/v1/logs",
            "management.opentelemetry.logging.export.otlp.transport=http",
            "management.opentelemetry.logging.export.otlp.compression=gzip",
            "management.opentelemetry.logging.export.otlp.headers.Authorization=Basic dGVzdA==",
            "management.otlp.metrics.export.url=" + ENDPOINT + "/v1/metrics",
            "management.otlp.metrics.export.aggregation-temporality=cumulative",
            "management.otlp.metrics.export.step=60s",
            "management.otlp.metrics.export.headers.Authorization=Basic dGVzdA=="
        };
    }
}
