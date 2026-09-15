package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.exporter.otlp.http.trace.OtlpHttpSpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.micrometer.tracing.opentelemetry.autoconfigure.otlp.OtlpTracingAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * 설정한 {@code Authorization} 헤더가 <b>실제로 와이어까지 나가는지</b> 본다.
 *
 * <p>여기까지 오는 길에 가정이 세 개 있었다 — 프로퍼티 키가 맞고({@link OtlpPropertyBindingTest}),
 * prod 설정이 그 키에 올바른 값을 넣고({@link ObservabilityPropertiesTest}), <b>Boot가 그 헤더 맵을
 * 익스포터에 제대로 물려 준다</b>는 것. 앞의 둘은 검증했지만 마지막 하나는 믿고 있을 뿐이었다.
 * 헤더가 빠지면 Grafana Cloud가 401로 돌려주는데 익스포터 로거를 {@code ERROR}로 눌러 둔 탓에
 * <b>조용히 실패한다</b> — 배포 후에 「텔레메트리가 안 온다」로만 드러난다.
 *
 * <p>가짜 수신기는 JDK에 들어 있는 {@link HttpServer}다. 새 의존성을 더하지 않으려고 골랐고,
 * 여기서 볼 것은 요청의 겉모습뿐이라 OTLP 응답 본문을 흉내 낼 필요가 없다.
 */
class OtlpExportOverHttpTest {

    private static final String CREDENTIALS = "1234567:glc_secret";

    private HttpServer receiver;
    private final AtomicReference<CapturedRequest> captured = new AtomicReference<>();

    @BeforeEach
    void startReceiver() throws IOException {
        receiver = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        receiver.createContext("/", this::capture);
        receiver.start();
    }

    @AfterEach
    void stopReceiver() {
        receiver.stop(0);
    }

    @DisplayName("스팬을 내보내면 설정한 경로로 Basic 자격증명과 gzip 본문이 실려 나간다.")
    @Test
    void exportCarriesAuthorizationHeader() {
        String expectedHeader = "Basic "
                + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes(java.nio.charset.StandardCharsets.UTF_8));

        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(OtlpTracingAutoConfiguration.class))
                .withPropertyValues(
                        "management.tracing.export.otlp.enabled=true",
                        "management.opentelemetry.tracing.export.otlp.endpoint=" + endpoint(),
                        "management.opentelemetry.tracing.export.otlp.transport=http",
                        "management.opentelemetry.tracing.export.otlp.compression=gzip",
                        "management.opentelemetry.tracing.export.otlp.headers.Authorization=" + expectedHeader)
                .run(context -> {
                    // given
                    OtlpHttpSpanExporter exporter = context.getBean(OtlpHttpSpanExporter.class);
                    try (SdkTracerProvider tracerProvider = SdkTracerProvider.builder()
                            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
                            .build()) {
                        Tracer tracer = tracerProvider.get("observability-test");

                        // when
                        tracer.spanBuilder("export-check").startSpan().end();
                        tracerProvider.forceFlush().join(10, TimeUnit.SECONDS);
                    }

                    // then
                    CapturedRequest request = captured.get();
                    assertThat(request).as("수신기에 요청이 오지 않았다").isNotNull();
                    assertThat(request.path()).isEqualTo("/v1/traces");
                    assertThat(request.authorization()).isEqualTo(expectedHeader);
                    assertThat(request.contentEncoding()).isEqualTo("gzip");
                });
    }

    private String endpoint() {
        return "http://" + receiver.getAddress().getHostString() + ":"
                + receiver.getAddress().getPort() + "/v1/traces";
    }

    private void capture(HttpExchange exchange) throws IOException {
        captured.compareAndSet(
                null,
                new CapturedRequest(
                        exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Content-Encoding")));
        exchange.getRequestBody().readAllBytes();
        // 본문 없는 200 이면 익스포터는 성공으로 본다. OTLP 응답 메시지를 흉내 낼 필요가 없다.
        exchange.sendResponseHeaders(200, -1);
        exchange.close();
    }

    private record CapturedRequest(String path, String authorization, String contentEncoding) {}
}
