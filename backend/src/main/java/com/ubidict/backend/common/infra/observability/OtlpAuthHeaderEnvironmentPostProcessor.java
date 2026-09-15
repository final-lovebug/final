package com.ubidict.backend.common.infra.observability;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.boot.logging.DeferredLogFactory;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Grafana Cloud OTLP가 요구하는 {@code Authorization: Basic ...} 헤더를 파라미터 값에서 파생한다.
 *
 * <p>Parameter Store의 {@code /lovebug/otel/auth}에는 {@code instanceID:token} <b>원문</b>이 들어
 * 있는데 게이트웨이는 그것을 base64로 인코딩한 Basic 자격증명으로 받는다. YAML 플레이스홀더로는
 * base64를 만들 수 없고, Spring Boot의 {@code Otlp*ConnectionDetails}는 URL만 넘길 뿐 헤더를 넘길
 * API가 없다. 그래서 여기서 {@code otel.auth-header} 프로퍼티를 만들어
 * {@code application-prod.yml}이 세 신호의 {@code headers.Authorization}에서 참조하게 한다.
 *
 * <p>익스포터 빈을 직접 등록하는 길도 있지만 그러면 Boot의 자동 구성이 통째로 물러나
 * 리소스 속성 병합·타임아웃·압축 배선을 전부 다시 써야 한다. 프로퍼티 하나를 더하는 쪽이
 * 세 신호를 같은 방법으로 다룰 수 있는 유일한 길이기도 하다.
 *
 * <p>{@link ConfigDataEnvironmentPostProcessor}<b>보다 뒤에 돌아야 한다</b> —
 * {@code spring.config.import: aws-parameterstore:/lovebug/}가 그 안에서 처리되므로, 먼저 돌면
 * 읽을 값이 아직 없다.
 *
 * <p>{@code otel.auth}가 없으면 아무것도 하지 않는다. {@code local}·{@code dev}·{@code test}는
 * Parameter Store를 읽지 않으므로 그 경로로 지나간다.
 */
public class OtlpAuthHeaderEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String AUTH_PROPERTY = "otel.auth";
    static final String AUTH_HEADER_PROPERTY = "otel.auth-header";
    static final String PROPERTY_SOURCE_NAME = "otlpAuthHeader";

    private final Log log;

    public OtlpAuthHeaderEnvironmentPostProcessor(DeferredLogFactory logFactory) {
        this.log = logFactory.getLog(getClass());
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        if (environment.getPropertySources().contains(PROPERTY_SOURCE_NAME)) {
            return;
        }

        String credentials = environment.getProperty(AUTH_PROPERTY);
        if (!StringUtils.hasText(credentials)) {
            return;
        }

        // SSM 값 끝에 개행이 섞이면 base64가 깨져 게이트웨이가 401로 돌려주는데, OTLP 익스포터
        // 로거를 ERROR로 눌러 둔 탓에 그 실패가 조용히 지나간다. 여기서 미리 다듬는다.
        String trimmed = credentials.trim();
        String header = "Basic " + Base64.getEncoder().encodeToString(trimmed.getBytes(StandardCharsets.UTF_8));

        // addLast 로 두면 OTEL_AUTH_HEADER 환경변수가 이 값을 이긴다(디버깅용 수동 덮어쓰기).
        environment
                .getPropertySources()
                .addLast(new MapPropertySource(PROPERTY_SOURCE_NAME, Map.of(AUTH_HEADER_PROPERTY, header)));

        logDerived(trimmed);
    }

    /**
     * 배포 후 「파라미터를 읽었는가」를 확인할 유일한 단서다. 익스포터가 조용히 실패하기 때문에
     * 이 줄이 없으면 텔레메트리가 안 오는 이유를 구분할 수 없다.
     *
     * <p>토큰은 절대 남기지 않는다. instance id는 계정 식별자일 뿐 자격증명이 아니다.
     */
    private void logDerived(String credentials) {
        int separator = credentials.indexOf(':');
        if (separator <= 0) {
            log.warn("[OtlpAuthHeaderEnvironmentPostProcessor.postProcessEnvironment] "
                    + "Otlp credentials are not in instanceId:token form. Grafana Cloud will reject the export.");
            return;
        }
        log.info("[OtlpAuthHeaderEnvironmentPostProcessor.postProcessEnvironment] "
                + "Otlp authorization header derived. instanceId=" + credentials.substring(0, separator));
    }
}
