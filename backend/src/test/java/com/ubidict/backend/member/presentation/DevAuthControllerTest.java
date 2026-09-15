package com.ubidict.backend.member.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.exception.MemberErrorCode;
import com.ubidict.backend.member.presentation.dto.DevLoginRequest;
import com.ubidict.backend.member.service.DevLoginService;
import com.ubidict.backend.member.service.model.TokenPairResult;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 로컬 전용 개발 로그인 컨트롤러를 테스트 프로필에서도 명시적으로 가져온다.
 */
@Import(DevAuthControllerTest.DevAuthControllerConfiguration.class)
class DevAuthControllerTest extends com.ubidict.backend.support.ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("존재하는 회원 id로 로그인하면 200과 토큰을 응답하고 refresh token을 쿠키로 내려준다.")
    @Test
    void login() {
        // given
        given(devLoginService.loginAs(1L))
                .willReturn(new TokenPairResult("access-token", "refresh-token", MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new DevLoginRequest(1L))
                .when()
                .post("/api/auth/dev/login")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("accessToken", equalTo("access-token"))
                .body("role", equalTo("REGULAR"))
                .header("Set-Cookie", startsWith(RefreshTokenCookieProvider.COOKIE_NAME + "=refresh-token"));
    }

    @DisplayName("존재하지 않는 회원이면 404를 응답한다.")
    @Test
    void login_memberNotFound() {
        // given
        given(devLoginService.loginAs(999L)).willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new DevLoginRequest(999L))
                .when()
                .post("/api/auth/dev/login")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("MEMBER_NOT_FOUND"));
    }

    @DisplayName("memberId가 없으면 400을 응답한다.")
    @Test
    void login_missingMemberId() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/auth/dev/login")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class DevAuthControllerConfiguration {

        @Bean
        DevAuthController devAuthController(
                DevLoginService devLoginService, RefreshTokenCookieProvider refreshTokenCookieProvider) {
            return new DevAuthController(devLoginService, refreshTokenCookieProvider);
        }
    }
}
