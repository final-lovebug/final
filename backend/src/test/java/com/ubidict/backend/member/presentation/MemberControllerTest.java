package com.ubidict.backend.member.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.MemberErrorCode;
import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.domain.MemberStatus;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.member.presentation.dto.CreateMemberRequest;
import com.ubidict.backend.member.presentation.dto.UpdateMemberRequest;
import com.ubidict.backend.member.service.MemberService;
import com.ubidict.backend.member.service.model.MemberResult;
import com.ubidict.backend.member.service.model.UpdateMemberCommand;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * addFilters=false로 Security 필터 체인 자체는 우회하지만, SecurityConfig가 이 슬라이스에
 * 함께 로드되므로 JwtAuthenticationFilter가 요구하는 JwtProvider는 mock으로 채워 컨텍스트를
 * 띄운다. 인증/인가 흐름 자체는 SecurityConfigTest에서 검증한다.
 *
 * <p>필터가 우회되므로 {@code /me} 엔드포인트가 쓰는 {@code @AuthenticationPrincipal}을 위해
 * SecurityContext에 인증 정보(회원 id=1L)를 직접 채운다.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    private static final Long MY_MEMBER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(MY_MEMBER_ID, null, List.of()));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @DisplayName("회원을 생성하면 201 Created와 생성된 회원 정보를 응답한다.")
    @Test
    void create() {
        // given
        given(memberService.create(any()))
                .willReturn(
                        new MemberResult(1L, "member@example.com", "member1", MemberStatus.ACTIVE, MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CreateMemberRequest("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .when()
                .post("/api/members")
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .header("Location", equalTo("/api/members/1"))
                .body("memberId", equalTo(1))
                .body("email", equalTo("member@example.com"))
                .body("status", equalTo("ACTIVE"))
                .body("role", equalTo("REGULAR"));
    }

    @DisplayName("이메일 형식이 잘못되면 400과 검증 에러를 응답한다.")
    @Test
    void create_invalidEmail() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CreateMemberRequest("invalid-email", "member1", OAuthProvider.GOOGLE, "google-1"))
                .when()
                .post("/api/members")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("이미 등록된 이메일이면 409를 응답한다.")
    @Test
    void create_duplicateEmail() {
        // given
        given(memberService.create(any())).willThrow(new BusinessException(MemberErrorCode.MEMBER_DUPLICATE_EMAIL));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new CreateMemberRequest("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1"))
                .when()
                .post("/api/members")
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("MEMBER_DUPLICATE_EMAIL"));
    }

    @DisplayName("내 정보를 조회하면 200과 회원 정보를 응답한다.")
    @Test
    void getMyProfile() {
        // given
        given(memberService.getById(MY_MEMBER_ID))
                .willReturn(
                        new MemberResult(1L, "member@example.com", "member1", MemberStatus.ACTIVE, MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/members/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("memberId", equalTo(1))
                .body("displayName", equalTo("member1"));
    }

    @DisplayName("존재하지 않는 회원이면 404를 응답한다.")
    @Test
    void getMyProfile_notFound() {
        // given
        given(memberService.getById(MY_MEMBER_ID)).willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/members/me")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("MEMBER_NOT_FOUND"));
    }

    @DisplayName("내 표시 이름을 수정하면 200과 수정된 회원 정보를 응답한다.")
    @Test
    void updateMyProfile() {
        // given
        given(memberService.update(eq(new UpdateMemberCommand(MY_MEMBER_ID, "새이름"))))
                .willReturn(new MemberResult(1L, "member@example.com", "새이름", MemberStatus.ACTIVE, MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new UpdateMemberRequest("새이름"))
                .when()
                .patch("/api/members/me")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("displayName", equalTo("새이름"));
    }

    @DisplayName("표시 이름이 비어 있으면 400을 응답한다.")
    @Test
    void updateMyProfile_blankDisplayName() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new UpdateMemberRequest(" "))
                .when()
                .patch("/api/members/me")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("내 계정을 탈퇴시키면 204 No Content를 응답한다.")
    @Test
    void withdrawMyself() {
        // when & then
        RestAssuredMockMvc.given().when().delete("/api/members/me").then().statusCode(HttpStatus.NO_CONTENT.value());

        verify(memberService).withdraw(MY_MEMBER_ID);
    }
}
