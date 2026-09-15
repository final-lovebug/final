package com.ubidict.backend.common.infra.observability;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.support.IntegrationTestSupport;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.localstack.LocalStackContainer;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.ParameterType;

/**
 * Parameter Store에 든 값이 OTLP 헤더가 되기까지의 사슬을 <b>실제 SSM으로</b> 확인한다.
 *
 * <p>사슬에 고리가 둘 있고 둘 다 지금까지 추론이었다.
 *
 * <ul>
 *   <li>{@code /lovebug/otel/auth}가 {@code otel.auth} 프로퍼티가 되는가 — 경로를 프로퍼티 이름으로
 *       바꾸는 규칙은 awspring 안에 있고 우리가 시험해 본 적이 없다.
 *   <li>{@link OtlpAuthHeaderEnvironmentPostProcessor}가 <b>Parameter Store가 올라온 뒤에</b> 도는가 —
 *       {@code OtlpAuthHeaderEnvironmentPostProcessorTest}는 순서 <i>숫자</i>만 비교한다. 순서가
 *       뒤집히면 예외 없이 <b>조용히 아무것도 하지 않는다.</b>
 * </ul>
 *
 * <p>LocalStack 컨테이너는 공유 설정({@code TestcontainersConfiguration})의 것을 그대로 쓴다. 검증
 * 대상은 그 위에 따로 띄우는 최소 컨텍스트이며, {@code prod} 프로파일은 쓰지 않는다 — 그 프로파일은
 * RDS·JWT·OAuth 파라미터까지 전부 요구하고, 여기서 볼 것은 관측 사슬뿐이다.
 */
class OtlpParameterStoreChainTest extends IntegrationTestSupport {

    private static final String CREDENTIALS = "1234567:glc_secret";
    private static final String ENDPOINT = "https://otlp-gateway-prod-ap-southeast-1.grafana.net/otlp";

    @Autowired
    private LocalStackContainer localStack;

    @DisplayName("Parameter Store 의 /lovebug/otel/ 값이 프로퍼티가 되고 Basic 헤더까지 파생된다.")
    @Test
    void parameterStoreValuesBecomeOtlpProperties() {
        // given
        putParameter("/lovebug/otel/auth", CREDENTIALS, ParameterType.SECURE_STRING);
        putParameter("/lovebug/otel/endpoint", ENDPOINT, ParameterType.STRING);

        // when
        try (ConfigurableApplicationContext context = bootWithParameterStore()) {
            // then — 경로가 프로퍼티 이름으로 바뀐다
            assertThat(context.getEnvironment().getProperty("otel.endpoint")).isEqualTo(ENDPOINT);
            assertThat(context.getEnvironment().getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_PROPERTY))
                    .isEqualTo(CREDENTIALS);

            // then — EPP 가 그 뒤에 돌아 헤더를 파생시킨다
            assertThat(context.getEnvironment()
                            .getProperty(OtlpAuthHeaderEnvironmentPostProcessor.AUTH_HEADER_PROPERTY))
                    .isEqualTo("Basic "
                            + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes(StandardCharsets.UTF_8)));
        }
    }

    private ConfigurableApplicationContext bootWithParameterStore() {
        return new SpringApplicationBuilder(BareConfiguration.class)
                .web(WebApplicationType.NONE)
                .bannerMode(Banner.Mode.OFF)
                .properties(
                        // 애플리케이션 설정 파일을 읽지 않는다 — 여기서 보려는 것은 관측 사슬뿐이다.
                        "spring.config.name=otlp-parameter-store-chain-test",
                        "spring.config.import=aws-parameterstore:/lovebug/",
                        "spring.cloud.aws.parameterstore.enabled=true",
                        "spring.cloud.aws.endpoint=" + localStack.getEndpoint(),
                        "spring.cloud.aws.region.static=" + localStack.getRegion(),
                        "spring.cloud.aws.credentials.access-key=" + localStack.getAccessKey(),
                        "spring.cloud.aws.credentials.secret-key=" + localStack.getSecretKey())
                .run();
    }

    private void putParameter(String name, String value, ParameterType type) {
        try (SsmClient ssm = SsmClient.builder()
                .endpointOverride(localStack.getEndpoint())
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())))
                .build()) {
            ssm.putParameter(
                    request -> request.name(name).value(value).type(type).overwrite(true));
        }
    }

    /**
     * 따로 띄우는 컨텍스트의 진입점.
     *
     * <p><b>{@code @Configuration}을 붙이지 않는다.</b> 붙이면 {@code @SpringBootTest}가 이 중첩
     * 클래스를 「기본 설정 클래스」로 감지해 {@code BackendApplication} 대신 써 버린다 — 공유
     * 컨텍스트가 통째로 어긋나 테스트가 기동조차 못 한다. {@code SpringApplication}은 애너테이션
     * 없는 클래스도 진입점으로 받는다.
     */
    static class BareConfiguration {}
}
