package com.ubidict.backend.member.infra.security;

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * {@code app.security.actuator.permit-all=true}(= {@code local}·{@code dev})일 때 actuator가 실제로
 * 열리는지 본다. {@link SecurityConfigTest}는 반대편인 「닫힘」을 고정하므로 이 쪽을 따로 둔다 —
 * 두 값을 한 컨텍스트에서 볼 수 없다.
 *
 * <p>노출({@code management.endpoints.web.exposure.include})과 인가는 별개의 스위치라, 노출만
 * 켜 두면 {@code /actuator/prometheus}가 401이 되어 「눈으로 확인하는 수단」이 동작하지 않는다.
 */
@Import({
    SecurityConfigTest.TestController.class,
    SecurityConfig.class,
    JwtAuthenticationFilter.class,
    JwtAuthenticationEntryPoint.class,
    JwtAccessDeniedHandler.class,
    JwtProvider.class,
    GoogleOAuth2LoginSuccessHandler.class,
    GoogleOAuth2LoginFailureHandler.class
})
@EnableConfigurationProperties({JwtProperties.class, CorsProperties.class, ActuatorSecurityProperties.class})
@TestPropertySource(properties = "app.security.actuator.permit-all=true")
@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
class ActuatorOpenSecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("actuator 가 열려 있으면 토큰 없이 /actuator/prometheus 를 호출할 수 있다.")
    @Test
    void actuatorPrometheus_withoutToken() {
        RestAssuredMockMvc.given().when().get("/actuator/prometheus").then().statusCode(HttpStatus.OK.value());
    }

    @DisplayName("actuator 가 열려 있어도 보호된 API 는 여전히 토큰을 요구한다.")
    @Test
    void protectedEndpoint_stillRequiresToken() {
        RestAssuredMockMvc.given().when().get("/api/protected/ping").then().statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
