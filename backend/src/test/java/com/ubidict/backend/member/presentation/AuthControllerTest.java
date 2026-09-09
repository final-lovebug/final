package com.ubidict.backend.member.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.infra.security.OAuthExchangeCodeRedisRepository;
import com.ubidict.backend.member.presentation.dto.OAuthExchangeRequest;
import com.ubidict.backend.member.service.LogoutService;
import com.ubidict.backend.member.service.MemberOAuthLoginService;
import com.ubidict.backend.member.service.TokenReissueService;
import com.ubidict.backend.member.service.model.TokenPairResult;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * addFilters=false로 Security 필터 체인은 우회하지만, SecurityConfig가 이 슬라이스에 함께
 * 로드되므로 JwtAuthenticationFilter·GoogleOAuth2LoginSuccessHandler가 요구하는 빈은 mock으로
 * 채워 컨텍스트를 띄운다. 인증/인가 흐름 자체는 SecurityConfigTest에서 검증한다.
 *
 * <p>RefreshTokenCookieProvider는 실제 구현을 그대로 써서 Set-Cookie 헤더 형식까지 검증한다.
 */
@Import(AuthControllerTest.CookieProviderConfig.class)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AuthController.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberOAuthLoginService memberOAuthLoginService;

    @MockitoBean
    private TokenReissueService tokenReissueService;

    @MockitoBean
    private LogoutService logoutService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("교환 코드로 로그인하면 200과 access token, role을 응답하고 refresh token을 쿠키로 내려준다.")
    @Test
    void exchange() {
        // given
        given(memberOAuthLoginService.loginByExchangeCode("exchange-code"))
                .willReturn(new TokenPairResult("access-token", "refresh-token", MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new OAuthExchangeRequest("exchange-code"))
                .when()
                .post("/api/auth/oauth/google/exchange")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", equalTo("access-token"))
                .body("role", equalTo("REGULAR"))
                .header("Set-Cookie", startsWith(RefreshTokenCookieProvider.COOKIE_NAME + "=refresh-token"));
    }

    @DisplayName("교환 코드가 비어 있으면 400을 응답한다.")
    @Test
    void exchange_blankCode() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new OAuthExchangeRequest(" "))
                .when()
                .post("/api/auth/oauth/google/exchange")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("교환 코드가 만료됐거나 이미 쓰였으면 401을 응답한다.")
    @Test
    void exchange_invalidCode() {
        // given
        given(memberOAuthLoginService.loginByExchangeCode("invalid-code"))
                .willThrow(new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new OAuthExchangeRequest("invalid-code"))
                .when()
                .post("/api/auth/oauth/google/exchange")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("AUTH_TOKEN_INVALID"));
    }

    @DisplayName("refresh token 쿠키로 재발급하면 200과 새 토큰을 응답한다.")
    @Test
    void refresh() {
        // given
        given(tokenReissueService.reissue("old-refresh-token"))
                .willReturn(new TokenPairResult("new-access-token", "new-refresh-token", MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .cookie(RefreshTokenCookieProvider.COOKIE_NAME, "old-refresh-token")
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", equalTo("new-access-token"))
                .header("Set-Cookie", startsWith(RefreshTokenCookieProvider.COOKIE_NAME + "=new-refresh-token"));
    }

    @DisplayName("refresh token 쿠키가 없으면 401 AUTH_TOKEN_MISSING을 응답한다.")
    @Test
    void refresh_withoutCookie() {
        RestAssuredMockMvc.given()
                .when()
                .post("/api/auth/refresh")
                .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value())
                .body("code", equalTo("AUTH_TOKEN_MISSING"));

        verify(tokenReissueService, never()).reissue(any());
    }

    @DisplayName("로그아웃하면 204를 응답하고 refresh token 쿠키를 만료시킨다.")
    @Test
    void logout() {
        // when & then
        RestAssuredMockMvc.given()
                .cookie(RefreshTokenCookieProvider.COOKIE_NAME, "refresh-token")
                .when()
                .post("/api/auth/logout")
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value())
                .header("Set-Cookie", startsWith(RefreshTokenCookieProvider.COOKIE_NAME + "=;"));

        verify(logoutService).logout("refresh-token");
    }

    @DisplayName("쿠키 없이 로그아웃해도 204를 응답한다(멱등).")
    @Test
    void logout_withoutCookie() {
        RestAssuredMockMvc.given().when().post("/api/auth/logout").then().statusCode(HttpStatus.NO_CONTENT.value());

        verify(logoutService, never()).logout(any());
    }

    @TestConfiguration
    static class CookieProviderConfig {

        @Bean
        RefreshTokenCookieProvider refreshTokenCookieProvider() {
            return new RefreshTokenCookieProvider(Duration.ofDays(7), true);
        }
    }
}
