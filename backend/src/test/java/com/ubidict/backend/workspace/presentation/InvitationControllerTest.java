package com.ubidict.backend.workspace.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.workspace.domain.InvitationStatus;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.InvitationResult;
import com.ubidict.backend.workspace.service.InvitationService;
import com.ubidict.backend.workspace.service.WorkspaceResult;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(InvitationController.class)
class InvitationControllerTest {

    private static final Long WORKSPACE_ID = 10L;
    private static final Long MEMBER_ID = 1L;
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-09-12T10:00:00+09:00");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InvitationService invitationService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("초대를 발급하면 링크 토큰을 포함해 201로 응답한다.")
    @Test
    void issue() {
        given(invitationService.issue(any())).willReturn(invitationResult());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"inviteeEmail":"invitee@example.com","permission":"REGULAR"}
                        """)
                .when()
                .post("/api/workspaces/{workspaceId}/invitations?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("token", equalTo("invitation-token"));
    }

    @DisplayName("초대 목록에는 링크 토큰을 노출하지 않는다.")
    @Test
    void readAll() {
        given(invitationService.readAll(WORKSPACE_ID, MEMBER_ID, InvitationStatus.PENDING))
                .willReturn(List.of(invitationResult()));

        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/workspaces/{workspaceId}/invitations?memberId={memberId}&status=PENDING",
                        WORKSPACE_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0]", not(hasKey("token")));
    }

    @DisplayName("초대를 취소하면 204로 응답한다.")
    @Test
    void cancel() {
        RestAssuredMockMvc.given()
                .when()
                .delete(
                        "/api/workspaces/{workspaceId}/invitations/{invitationId}?memberId={memberId}",
                        WORKSPACE_ID,
                        20L,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("워크스페이스 하위가 아닌 초대 링크 경로로 수락하면 상세를 201로 응답한다.")
    @Test
    void accept() {
        given(invitationService.accept("invitation-token", MEMBER_ID))
                .willReturn(new WorkspaceResult(WORKSPACE_ID, "개발팀", 0, 0, Permission.REGULAR, NOW));

        RestAssuredMockMvc.given()
                .when()
                .post("/api/invitations/{token}/accept?memberId={memberId}", "invitation-token", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("workspaceId", equalTo(WORKSPACE_ID.intValue()))
                .body("myPermission", equalTo("REGULAR"));
    }

    private static InvitationResult invitationResult() {
        return new InvitationResult(
                20L,
                WORKSPACE_ID,
                "invitee@example.com",
                "invitation-token",
                Permission.REGULAR,
                InvitationStatus.PENDING,
                NOW.plusDays(7));
    }
}
