package com.ubidict.backend.workspace.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.service.ChangePermissionCommand;
import com.ubidict.backend.workspace.service.ParticipantResult;
import com.ubidict.backend.workspace.service.ParticipantService;
import com.ubidict.backend.workspace.service.RemoveParticipantCommand;
import com.ubidict.backend.workspace.service.TransferOwnershipCommand;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(ParticipantController.class)
class ParticipantControllerTest {

    private static final Long WORKSPACE_ID = 10L;
    private static final Long PARTICIPANT_ID = 2L;
    private static final Long MEMBER_ID = 1L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ParticipantService participantService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("워크스페이스 참여자 목록을 배열로 조회한다.")
    @Test
    void readAll() {
        given(participantService.readAll(WORKSPACE_ID, MEMBER_ID))
                .willReturn(List.of(new ParticipantResult(
                        PARTICIPANT_ID, WORKSPACE_ID, MEMBER_ID, Permission.OWNER, OffsetDateTime.now())));

        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/participants?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].participantId", equalTo(PARTICIPANT_ID.intValue()))
                .body("[0].permission", equalTo("OWNER"));
    }

    @DisplayName("소유자가 참여자의 권한을 변경한다.")
    @Test
    void changePermission() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"permission\":\"ADMIN\"}")
                .when()
                .patch(
                        "/api/workspaces/{workspaceId}/participants/{participantId}/permission?memberId={memberId}",
                        WORKSPACE_ID,
                        PARTICIPANT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(204);

        then(participantService)
                .should()
                .changePermission(
                        new ChangePermissionCommand(WORKSPACE_ID, PARTICIPANT_ID, Permission.ADMIN, MEMBER_ID));
    }

    @DisplayName("권한 변경 경로로 소유자 권한을 부여할 수 없다.")
    @Test
    void changePermission_ownerIsRejected() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"permission\":\"OWNER\"}")
                .when()
                .patch(
                        "/api/workspaces/{workspaceId}/participants/{participantId}/permission?memberId={memberId}",
                        WORKSPACE_ID,
                        PARTICIPANT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(400);

        then(participantService).shouldHaveNoInteractions();
    }

    @DisplayName("소유자가 다른 참여자에게 소유권을 이전한다.")
    @Test
    void transferOwnership() {
        RestAssuredMockMvc.given()
                .when()
                .patch(
                        "/api/workspaces/{workspaceId}/participants/{participantId}/ownership?memberId={memberId}",
                        WORKSPACE_ID,
                        PARTICIPANT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(204);

        then(participantService)
                .should()
                .transferOwnership(new TransferOwnershipCommand(WORKSPACE_ID, PARTICIPANT_ID, MEMBER_ID));
    }

    @DisplayName("관리자가 참여자를 내보낸다.")
    @Test
    void remove() {
        RestAssuredMockMvc.given()
                .when()
                .delete(
                        "/api/workspaces/{workspaceId}/participants/{participantId}?memberId={memberId}",
                        WORKSPACE_ID,
                        PARTICIPANT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(204);

        then(participantService).should().remove(new RemoveParticipantCommand(WORKSPACE_ID, PARTICIPANT_ID, MEMBER_ID));
    }
}
