package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.impl.NoOpLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * 설정 파일에 적힌 OTLP 프로퍼티가 <b>최종적으로 어떤 값이 되는지</b> 본다.
 *
 * <p>{@link OtlpPropertyBindingTest}가 「이 키를 주면 익스포터가 생긴다」를 보는 반면 여기서는
 * 「{@code application-prod.yml}이 실제로 그 키에 무엇을 넣는가」를 본다. 둘이 붙어야 prod 배선이
 * 증명된다 — 키가 맞아도 값이 틀리면(엔드포인트 경로를 두 번 붙인다든지) 배포 후에야 드러난다.
 *
 * <p>Parameter Store 값은 흉내만 낸다. 실제 SSM에서 여기까지 이어지는지는
 * {@code OtlpParameterStoreChainTest}가 LocalStack으로 본다.
 */
class ObservabilityPropertiesTest {

    /**
     * 인프라가 등록해 둔 값의 형태(2026-09-15 확인) — <b>경로가 없고 끝 슬래시도 없다.</b> 신호별
     * 경로는 애플리케이션이 붙인다. 이 전제가 깨지면 세 신호 모두 잘못된 URL로 나간다.
     */
    private static final String ENDPOINT = "https://otlp-gateway-prod-ap-southeast-1.grafana.net/otlp";

    private static final String CREDENTIALS = "1234567:glc_secret";

    /** Boot 3.x 표기. 4.x에서 deprecation level이 {@code error}라 하나라도 남으면 기동이 깨진다. */
    private static final List<String> DEPRECATED_KEY_PREFIXES = List.of(
            "management.otlp.tracing",
            "management.otlp.logging",
            "management.tracing.opentelemetry.export",
            "management.tracing.enabled",
            "management.metrics.export");

    private static final List<String> CONFIGURATION_FILES =
            List.of("application.yml", "application-local.yml", "application-dev.yml", "application-prod.yml");

    @DisplayName("prod 의 트레이스 엔드포인트가 파라미터 값 뒤에 /v1/traces 를 붙여 만들어진다.")
    @Test
    void tracingEndpointIsComposed() {
        StandardEnvironment environment = prodEnvironment(Map.of());

        assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.endpoint"))
                .isEqualTo(ENDPOINT + "/v1/traces");
    }

    @DisplayName("prod 의 로그 엔드포인트가 파라미터 값 뒤에 /v1/logs 를 붙여 만들어진다.")
    @Test
    void loggingEndpointIsComposed() {
        StandardEnvironment environment = prodEnvironment(Map.of());

        assertThat(environment.getProperty("management.opentelemetry.logging.export.otlp.endpoint"))
                .isEqualTo(ENDPOINT + "/v1/logs");
    }

    @DisplayName("prod 의 메트릭 주소가 파라미터 값 뒤에 /v1/metrics 를 붙여 만들어진다.")
    @Test
    void metricsUrlIsComposed() {
        StandardEnvironment environment = prodEnvironment(Map.of());

        assertThat(environment.getProperty("management.otlp.metrics.export.url"))
                .isEqualTo(ENDPOINT + "/v1/metrics");
    }

    @DisplayName("세 신호 모두 같은 Basic 자격증명을 Authorization 헤더로 싣는다.")
    @Test
    void allSignalsCarryTheSameBasicAuthHeader() {
        StandardEnvironment environment = prodEnvironment(Map.of());
        String expected = "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes(StandardCharsets.UTF_8));

        assertThat(List.of(
                        "management.opentelemetry.tracing.export.otlp.headers.Authorization",
                        "management.opentelemetry.logging.export.otlp.headers.Authorization",
                        "management.otlp.metrics.export.headers.Authorization"))
                .allSatisfy(key -> assertThat(environment.getProperty(key)).isEqualTo(expected));
    }

    @DisplayName("킬 스위치 파라미터가 없으면 세 신호가 모두 켜진다.")
    @Test
    void exportIsOnWithoutKillSwitch() {
        StandardEnvironment environment = prodEnvironment(Map.of());

        assertThat(enabledFlags(environment)).containsExactly("true", "true", "true");
    }

    @DisplayName("킬 스위치 파라미터 하나로 세 신호가 한꺼번에 꺼진다.")
    @Test
    void killSwitchDisablesAllThreeSignals() {
        StandardEnvironment environment = prodEnvironment(Map.of("otel.enabled", "false"));

        assertThat(enabledFlags(environment)).containsExactly("false", "false", "false");
    }

    @DisplayName("자격증명이 없어도 Authorization 이 빈 값으로 해석돼 기동을 막지 않는다.")
    @Test
    void missingCredentialsDoNotBreakStartup() {
        // given — 관측은 기동 조건이 아니다(D-98). 플레이스홀더 해석 실패로 죽으면 안 된다.
        StandardEnvironment environment = new StandardEnvironment();
        loadYaml(environment, "application.yml");
        loadYaml(environment, "application-prod.yml");
        environment
                .getPropertySources()
                .addFirst(new MapPropertySource("parameterStore", Map.of("otel.endpoint", ENDPOINT)));

        // when & then
        assertThat(environment.getProperty("management.opentelemetry.tracing.export.otlp.headers.Authorization"))
                .isEmpty();
    }

    @DisplayName("설정 파일 어디에도 Boot 3.x 표기의 낡은 키가 없다.")
    @Test
    void configurationFilesDoNotUseDeprecatedKeys() {
        // given — 이 키들은 deprecation level 이 error 라 기동을 깨뜨린다. prod 프로파일은 CI 에서
        // 뜨지 않으므로 누가 되살려 놓아도 배포 전까지 드러나지 않는다.
        assertThat(CONFIGURATION_FILES).allSatisfy(file -> {
            StandardEnvironment environment = new StandardEnvironment();
            loadYaml(environment, file);

            // isNotEmpty 가 없으면 파일을 못 읽었을 때 allSatisfy 가 공허하게 통과한다.
            assertThat(propertyNames(environment, file)).isNotEmpty().allSatisfy(name -> assertThat(
                            DEPRECATED_KEY_PREFIXES)
                    .withFailMessage("%s 에 Boot 3.x 표기의 낡은 키 %s 가 있다", file, name)
                    .noneMatch(name::startsWith));
        });
    }

    private List<String> enabledFlags(StandardEnvironment environment) {
        return List.of(
                environment.getProperty("management.tracing.export.otlp.enabled"),
                environment.getProperty("management.logging.export.otlp.enabled"),
                environment.getProperty("management.otlp.metrics.export.enabled"));
    }

    /** 실제 기동과 같은 순서로 쌓는다 — 공통 파일 위에 prod 가 얹히고, 그 위에 Parameter Store 가 온다. */
    private StandardEnvironment prodEnvironment(Map<String, Object> extraParameters) {
        StandardEnvironment environment = new StandardEnvironment();
        loadYaml(environment, "application.yml");
        loadYaml(environment, "application-prod.yml");

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("otel.endpoint", ENDPOINT);
        parameters.put(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY, CREDENTIALS);
        parameters.putAll(extraParameters);
        environment.getPropertySources().addFirst(new MapPropertySource("parameterStore", parameters));

        new OtlpAuthHeaderEnvironmentPostProcessor(supplier -> new NoOpLog()).postProcessEnvironment(environment, null);
        return environment;
    }

    /** 나중에 부른 파일이 앞선 파일을 이기도록 addFirst 로 쌓는다. */
    private void loadYaml(StandardEnvironment environment, String file) {
        try {
            List<PropertySource<?>> sources = new YamlPropertySourceLoader().load(file, new ClassPathResource(file));
            sources.forEach(source -> environment.getPropertySources().addFirst(source));
        } catch (IOException e) {
            throw new IllegalStateException(file + " 를 읽지 못했다", e);
        }
    }

    private List<String> propertyNames(StandardEnvironment environment, String file) {
        List<String> names = new ArrayList<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (source.getName().startsWith(file) && source instanceof EnumerablePropertySource<?> enumerable) {
                names.addAll(Arrays.asList(enumerable.getPropertyNames()));
            }
        }
        return names;
    }
}
