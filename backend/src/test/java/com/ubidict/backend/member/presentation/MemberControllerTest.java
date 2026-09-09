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
import com.ubidict.backend.member.presentation.dto.CreateMemberRequest;
import com.ubidict.backend.member.presentation.dto.UpdateMemberRequest;
import com.ubidict.backend.member.service.MemberService;
import com.ubidict.backend.member.service.model.MemberResult;
import com.ubidict.backend.member.service.model.UpdateMemberCommand;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SecurityConfig가 아직 없어(로그인 도메인 몫) addFilters=false로 Security 필터 체인을 우회한다.
 */
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(MemberController.class)
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
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

    @DisplayName("id로 회원을 조회하면 200과 회원 정보를 응답한다.")
    @Test
    void getById() {
        // given
        given(memberService.getById(1L))
                .willReturn(
                        new MemberResult(1L, "member@example.com", "member1", MemberStatus.ACTIVE, MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/members/1")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("memberId", equalTo(1))
                .body("displayName", equalTo("member1"));
    }

    @DisplayName("존재하지 않는 회원을 조회하면 404를 응답한다.")
    @Test
    void getById_notFound() {
        // given
        given(memberService.getById(1L)).willThrow(new BusinessException(MemberErrorCode.MEMBER_NOT_FOUND));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/members/1")
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("MEMBER_NOT_FOUND"));
    }

    @DisplayName("표시 이름을 수정하면 200과 수정된 회원 정보를 응답한다.")
    @Test
    void update() {
        // given
        given(memberService.update(eq(new UpdateMemberCommand(1L, "새이름"))))
                .willReturn(new MemberResult(1L, "member@example.com", "새이름", MemberStatus.ACTIVE, MemberRole.REGULAR));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new UpdateMemberRequest("새이름"))
                .when()
                .patch("/api/members/1")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("displayName", equalTo("새이름"));
    }

    @DisplayName("표시 이름이 비어 있으면 400을 응답한다.")
    @Test
    void update_blankDisplayName() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(new UpdateMemberRequest(" "))
                .when()
                .patch("/api/members/1")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("회원을 탈퇴시키면 204 No Content를 응답한다.")
    @Test
    void withdraw() {
        // when & then
        RestAssuredMockMvc.given().when().delete("/api/members/1").then().statusCode(HttpStatus.NO_CONTENT.value());

        verify(memberService).withdraw(1L);
    }
}
