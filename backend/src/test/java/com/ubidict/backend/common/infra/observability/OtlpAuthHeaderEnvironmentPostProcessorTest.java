package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.apache.commons.logging.impl.NoOpLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.mock.env.MockEnvironment;

/**
 * {@link OtlpAuthHeaderEnvironmentPostProcessor}가 Parameter Store의 원문 자격증명을 Grafana Cloud가
 * 받는 Basic 헤더로 바꾸는지 본다. Spring 컨텍스트를 띄우지 않는다.
 */
class OtlpAuthHeaderEnvironmentPostProcessorTest {

    private final OtlpAuthHeaderEnvironmentPostProcessor postProcessor =
            new OtlpAuthHeaderEnvironmentPostProcessor(supplier -> new NoOpLog());

    @DisplayName("instanceId:token 원문을 Basic 자격증명으로 바꿔 둔다.")
    @Test
    void derivesBasicHeader() {
        // given
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY, "1234567:glc_secret");

        // when
        postProcessor.postProcessEnvironment(environment, null);

        // then
        assertThat(environment.getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_HEADER_PROPERTY))
                .isEqualTo("Basic " + encode("1234567:glc_secret"));
    }

    @DisplayName("자격증명에 섞인 공백과 개행을 걷어낸 뒤 인코딩한다.")
    @Test
    void trimsCredentials() {
        // given — SSM 값 끝의 개행이 그대로 인코딩되면 게이트웨이가 401 로 돌려준다
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY, "  1234567:glc_secret\n");

        // when
        postProcessor.postProcessEnvironment(environment, null);

        // then
        assertThat(environment.getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_HEADER_PROPERTY))
                .isEqualTo("Basic " + encode("1234567:glc_secret"));
    }

    @DisplayName("자격증명이 없으면 아무것도 더하지 않는다.")
    @Test
    void doesNothingWithoutCredentials() {
        // given — local·dev·test 는 Parameter Store 를 읽지 않아 이 경로로 지나간다
        MockEnvironment environment = new MockEnvironment();

        // when
        postProcessor.postProcessEnvironment(environment, null);

        // then
        assertThat(environment
                        .getPropertySources()
                        .contains(OtlpAuthHeaderEnvironmentPostProcessor.PROPERTY_SOURCE_NAME))
                .isFalse();
        assertThat(environment.getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_HEADER_PROPERTY))
                .isNull();
    }

    @DisplayName("자격증명이 빈 문자열이어도 아무것도 더하지 않는다.")
    @Test
    void doesNothingWithBlankCredentials() {
        // given
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY, "   ");

        // when
        postProcessor.postProcessEnvironment(environment, null);

        // then
        assertThat(environment.getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_HEADER_PROPERTY))
                .isNull();
    }

    @DisplayName("이미 파생해 둔 프로퍼티 소스가 있으면 다시 만들지 않는다.")
    @Test
    void doesNotDeriveTwice() {
        // given — 부모·자식 컨텍스트에서 두 번 도는 경우가 있다
        MockEnvironment environment = new MockEnvironment();
        environment.setProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY, "1234567:glc_secret");
        postProcessor.postProcessEnvironment(environment, null);

        // when
        postProcessor.postProcessEnvironment(environment, null);

        // then
        assertThat(environment.getPropertySources().stream()
                        .filter(source ->
                                OtlpAuthHeaderEnvironmentPostProcessor.PROPERTY_SOURCE_NAME.equals(source.getName()))
                        .count())
                .isEqualTo(1);
    }

    @DisplayName("Parameter Store 가 올라온 뒤에 돌도록 순서가 잡혀 있다.")
    @Test
    void runsAfterConfigData() {
        // 먼저 돌면 otel.auth 를 읽을 수 없어 조용히 아무것도 하지 않는다 — 실패가 드러나지 않는
        // 종류의 버그라 순서를 테스트로 못 박는다.
        assertThat(postProcessor.getOrder()).isGreaterThan(ConfigDataEnvironmentPostProcessor.ORDER);
    }

    @DisplayName("META-INF/spring.factories 로 실제 등록돼 기동 시 돈다.")
    @Test
    void isRegisteredViaSpringFactories() {
        // given — 등록 경로 오타는 예외 없이 조용히 무시돼 배포 후에야 드러난다
        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(BareConfiguration.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .properties(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY + "=1234567:glc_secret")
                .run()) {
            // when & then
            assertThat(context.getEnvironment()
                            .getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_HEADER_PROPERTY))
                    .isEqualTo("Basic " + encode("1234567:glc_secret"));
        }
    }

    private String encode(String value) {
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 자동 구성 없이 컨텍스트만 띄우기 위한 진입점.
     *
     * <p>{@code @Configuration}을 붙이지 않는다 — 붙이면 나중에 이 클래스에 {@code @SpringBootTest}가
     * 더해졌을 때 중첩 설정 클래스로 감지돼 애플리케이션 설정을 대체해 버린다.
     */
    static class BareConfiguration {}
}
