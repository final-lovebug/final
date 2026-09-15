package com.ubidict.backend.member.infra.security;

import static org.hamcrest.Matchers.equalTo;

import com.ubidict.backend.member.domain.MemberRole;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWT 인증/인가 필터 체인을 end-to-end로 검증한다. 실제 {@link SecurityConfig}·
 * {@link JwtAuthenticationFilter}·{@link JwtAuthenticationEntryPoint}·
 * {@link JwtAccessDeniedHandler}·{@link JwtProvider}를 그대로 사용한다.
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
// prod 와 같은 「닫힘」 상태를 고정한다. 프로파일 기본값에 기대면 application-dev.yml 이
// permit-all: true 로 덮어 이 테스트가 아무것도 지키지 못한다.
@TestPropertySource(properties = "app.security.actuator.permit-all=false")
@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("/api/auth/** 는 토큰 없이도 호출할 수 있다.")
    @Test
    void permitAll() {
        RestAssuredMockMvc.given().when().get("/api/auth/ping").then().statusCode(HttpStatus.OK.value());
    }

    @DisplayName("/api/internal/** 는 토큰 없이도 호출할 수 있다.")
    @Test
    void permitAll_internal() {
        RestAssuredMockMvc.given().when().get("/api/internal/ping").then().statusCode(HttpStatus.OK.value());
    }

    @DisplayName("/actuator/health 는 토큰 없이도 호출할 수 있다.")
    @Test
    void permitAll_actuatorHealth() {
        RestAssuredMockMvc.given().when().get("/actuator/health").then().statusCode(HttpStatus.OK.value());
    }

    @DisplayName("/actuator/health/readiness 는 토큰 없이도 호출할 수 있다.")
    @Test
    void permitAll_actuatorHealthProbe() {
        RestAssuredMockMvc.given()
                .when()
                .get("/actuator/health/readiness")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("actuator 가 닫혀 있으면 토큰 없이 /actuator/prometheus 를 호출할 수 없다.")
    @Test
    void actuatorPrometheus_withoutToken() {
        RestAssuredMockMvc.given()
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("AUTH_TOKEN_MISSING"));
    }

    @DisplayName("actuator 가 닫혀 있으면 REGULAR 권한으로도 /actuator/prometheus 를 호출할 수 없다.")
    @Test
    void actuatorPrometheus_regularToken() {
        // given
        String accessToken = jwtProvider.issueAccessToken(1L, MemberRole.REGULAR);

        // when & then
        RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/actuator/prometheus")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @DisplayName("토큰 없이 보호된 API를 호출하면 401 AUTH_TOKEN_MISSING을 응답한다.")
    @Test
    void protectedEndpoint_withoutToken() {
        RestAssuredMockMvc.given()
                .when()
                .get("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("AUTH_TOKEN_MISSING"));
    }

    @DisplayName("Bearer 형식이 아닌 토큰으로 호출하면 401 AUTH_TOKEN_INVALID를 응답한다.")
    @Test
    void protectedEndpoint_invalidToken() {
        RestAssuredMockMvc.given()
                .header("Authorization", "Basic garbage")
                .when()
                .get("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("AUTH_TOKEN_INVALID"));
    }

    @DisplayName("만료된 토큰으로 호출하면 401 AUTH_TOKEN_EXPIRED를 응답한다.")
    @Test
    void protectedEndpoint_expiredToken() {
        // given
        SecretKey key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        Instant expiredAt = Instant.now().minus(Duration.ofMinutes(1));
        String expiredToken = Jwts.builder()
                .subject("1")
                .claim("role", "REGULAR")
                .issuedAt(Date.from(expiredAt.minus(Duration.ofMinutes(30))))
                .expiration(Date.from(expiredAt))
                .signWith(key)
                .compact();

        // when & then
        RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + expiredToken)
                .when()
                .get("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("AUTH_TOKEN_EXPIRED"));
    }

    @DisplayName("유효한 토큰으로 호출하면 200을 응답한다.")
    @Test
    void protectedEndpoint_validToken() {
        // given
        String accessToken = jwtProvider.issueAccessToken(1L, MemberRole.REGULAR);

        // when & then
        RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("REGULAR 권한으로 관리자 전용 API를 호출하면 403 AUTH_FORBIDDEN을 응답한다.")
    @Test
    void adminEndpoint_forbidden() {
        // given
        String accessToken = jwtProvider.issueAccessToken(1L, MemberRole.REGULAR);

        // when & then
        RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/admin/ping")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("AUTH_FORBIDDEN"));
    }

    @DisplayName("ADMIN 권한으로 관리자 전용 API를 호출하면 200을 응답한다.")
    @Test
    void adminEndpoint_ok() {
        // given
        String accessToken = jwtProvider.issueAccessToken(1L, MemberRole.ADMIN);

        // when & then
        RestAssuredMockMvc.given()
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/admin/ping")
                .then()
                .statusCode(HttpStatus.OK.value());
    }

    @DisplayName("허용된 origin에서 preflight 요청을 보내면 CORS 헤더를 포함해 200을 응답한다.")
    @Test
    void corsPreflight_allowedOrigin() {
        RestAssuredMockMvc.given()
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.OK.value())
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:5173"))
                .header("Access-Control-Allow-Credentials", equalTo("true"));
    }

    @DisplayName("허용되지 않은 origin에서 preflight 요청을 보내면 403을 응답한다.")
    @Test
    void corsPreflight_disallowedOrigin() {
        RestAssuredMockMvc.given()
                .header("Origin", "http://evil.example.com")
                .header("Access-Control-Request-Method", "GET")
                .when()
                .options("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value());
    }

    @DisplayName("허용된 origin에서 인증된 실제 요청을 보내면 Access-Control-Allow-Origin 헤더를 포함해 응답한다.")
    @Test
    void corsActualRequest_allowedOrigin() {
        // given
        String accessToken = jwtProvider.issueAccessToken(1L, MemberRole.REGULAR);

        // when & then
        RestAssuredMockMvc.given()
                .header("Origin", "http://localhost:5173")
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get("/api/protected/ping")
                .then()
                .statusCode(HttpStatus.OK.value())
                .header("Access-Control-Allow-Origin", equalTo("http://localhost:5173"));
    }

    @RequestMapping
    @RestController
    static class TestController {

        @GetMapping("/api/auth/ping")
        String authPing() {
            return "pong";
        }

        @GetMapping("/api/protected/ping")
        String protectedPing() {
            return "pong";
        }

        /** AI 워커 콜백 자리. 인증 주체 없이 필터 체인을 통과해야 한다(D-69). */
        @GetMapping("/api/internal/ping")
        String internalPing() {
            return "pong";
        }

        @GetMapping("/api/admin/ping")
        String adminPing() {
            return "pong";
        }

        /**
         * actuator 엔드포인트 자리. 이 슬라이스에는 actuator 자동 구성이 없으므로 대역을 둔다 —
         * 여기서 검증하는 것은 엔드포인트의 내용이 아니라 <b>필터 체인이 이 경로를 어떻게 판단하는가</b>다.
         */
        @GetMapping({"/actuator/health", "/actuator/health/readiness"})
        String actuatorHealth() {
            return "UP";
        }

        @GetMapping("/actuator/prometheus")
        String actuatorPrometheus() {
            return "# metrics";
        }
    }
}
