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
    JwtProvider.class
})
@EnableConfigurationProperties(JwtProperties.class)
@WebMvcTest(controllers = SecurityConfigTest.TestController.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private JwtProperties jwtProperties;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("/api/auth/** 는 토큰 없이도 호출할 수 있다.")
    @Test
    void permitAll() {
        RestAssuredMockMvc.given().when().get("/api/auth/ping").then().statusCode(HttpStatus.OK.value());
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

        @GetMapping("/api/admin/ping")
        String adminPing() {
            return "pong";
        }
    }
}
