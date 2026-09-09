package com.ubidict.backend.workspace.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.service.CreateWorkspaceCommand;
import com.ubidict.backend.workspace.service.WorkspaceResult;
import com.ubidict.backend.workspace.service.WorkspaceService;
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
@WebMvcTest(WorkspaceController.class)
class WorkspaceControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long WORKSPACE_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("워크스페이스를 생성하면 201과 식별자를 응답한다.")
    @Test
    void create() {
        // given
        given(workspaceService.create(any(CreateWorkspaceCommand.class)))
                .willReturn(new WorkspaceResult(WORKSPACE_ID, "개발팀", 0, 0, Permission.OWNER, OffsetDateTime.now()));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "개발팀"}
                        """)
                .when()
                .post("/api/workspaces?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("workspaceId", equalTo(WORKSPACE_ID.intValue()));
    }

    @DisplayName("이름이 비어 있으면 400과 공통 검증 실패 코드를 응답한다.")
    @Test
    void create_nameIsBlank() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "  "}
                        """)
                .when()
                .post("/api/workspaces?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("요청자 식별자가 없으면 400을 응답한다.")
    @Test
    void create_memberIdIsMissing() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "개발팀"}
                        """)
                .when()
                .post("/api/workspaces")
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("참여 중인 워크스페이스 목록을 200으로 응답한다.")
    @Test
    void readMine() {
        // given
        given(workspaceService.readMine(MEMBER_ID))
                .willReturn(List.of(
                        new WorkspaceResult(WORKSPACE_ID, "개발팀", 0, 0, Permission.OWNER, OffsetDateTime.now())));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("$", hasSize(1))
                .body("[0].name", equalTo("개발팀"))
                .body("[0].myPermission", equalTo("OWNER"));
    }

    @DisplayName("워크스페이스 상세 응답에는 룰셋이 포함된다.")
    @Test
    void read() {
        // given
        given(workspaceService.read(WORKSPACE_ID, MEMBER_ID))
                .willReturn(new WorkspaceResult(WORKSPACE_ID, "개발팀", 2, 3, Permission.OWNER, OffsetDateTime.now()));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("requiredDocumentReviewerCount", equalTo(2))
                .body("requiredDictionaryReviewerCount", equalTo(3));
    }

    @DisplayName("참여자가 아닌 워크스페이스를 조회하면 404를 응답한다.")
    @Test
    void read_memberIsNotParticipant() {
        // given
        given(workspaceService.read(WORKSPACE_ID, MEMBER_ID))
                .willThrow(new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("WORKSPACE_NOT_FOUND"));
    }

    @DisplayName("워크스페이스 이름을 바꾸면 204를 응답한다.")
    @Test
    void rename() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "플랫폼팀"}
                        """)
                .when()
                .patch("/api/workspaces/{workspaceId}?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("ADMIN 미만이 이름을 바꾸려 하면 403을 응답한다.")
    @Test
    void rename_permissionIsBelowAdmin() {
        // given
        willThrow(new BusinessException(WorkspaceErrorCode.WORKSPACE_ADMIN_REQUIRED))
                .given(workspaceService)
                .rename(any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"name": "플랫폼팀"}
                        """)
                .when()
                .patch("/api/workspaces/{workspaceId}?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("WORKSPACE_ADMIN_REQUIRED"));
    }

    @DisplayName("워크스페이스를 삭제하면 204를 응답한다.")
    @Test
    void delete() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/workspaces/{workspaceId}?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }
}
