package com.ubidict.backend.revisionlog.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import com.ubidict.backend.revisionlog.exception.RevisionLogErrorCode;
import com.ubidict.backend.revisionlog.service.RevisionLogService;
import com.ubidict.backend.revisionlog.service.model.RevisionLogDetailResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogEntryResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogSearchQuery;
import com.ubidict.backend.support.WithLoginMember;
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

@WithLoginMember(2L)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RevisionLogController.class)
class RevisionLogControllerTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long REVISION_LOG_ID = 42L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevisionLogService revisionLogService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("개정 이력 목록을 조회하면 200과 페이징 응답을 내려준다.")
    @Test
    void search() {
        given(revisionLogService.search(any(RevisionLogSearchQuery.class)))
                .willReturn(new PageResult<>(List.of(result()), 0, 20, 1));

        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/revision-logs?targetType=DICTIONARY", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content[0].revisionLogId", equalTo(REVISION_LOG_ID.intValue()))
                .body("content[0].grade", equalTo("NEW_TERMS"))
                .body("totalElements", equalTo(1));
    }

    @DisplayName("개정 이력 상세를 조회하면 변경 항목을 내려준다.")
    @Test
    void read() {
        given(revisionLogService.read(WORKSPACE_ID, REVISION_LOG_ID, 2L))
                .willReturn(new RevisionLogDetailResult(
                        result(),
                        List.of(new RevisionLogEntryResult(RevisionLogChangeType.ADDED, "사용자", "User", null, null))));

        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/revision-logs/{revisionLogId}", WORKSPACE_ID, REVISION_LOG_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("revisionLog.revisionLogId", equalTo(REVISION_LOG_ID.intValue()))
                .body("entries[0].subject", equalTo("사용자"));
    }

    @DisplayName("목록에서 targetType이 없으면 400을 응답한다.")
    @Test
    void search_withoutTargetType() {
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/revision-logs", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("정렬 화이트리스트 밖의 필드로 조회하면 400을 응답한다.")
    @Test
    void search_invalidSort() {
        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/workspaces/{workspaceId}/revision-logs?targetType=DOCUMENT&sort=createdAt,desc",
                        WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("없는 개정 이력을 조회하면 404를 응답한다.")
    @Test
    void read_notFound() {
        willThrow(new BusinessException(RevisionLogErrorCode.REVISION_LOG_NOT_FOUND))
                .given(revisionLogService)
                .read(anyLong(), anyLong(), anyLong());

        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/revision-logs/{revisionLogId}", WORKSPACE_ID, REVISION_LOG_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo(RevisionLogErrorCode.REVISION_LOG_NOT_FOUND.name()));
    }

    private RevisionLogResult result() {
        return new RevisionLogResult(
                REVISION_LOG_ID,
                WORKSPACE_ID,
                RevisionLogTargetType.DICTIONARY,
                10L,
                2,
                1,
                RevisionOrigin.REVIEW_REVISE,
                "용어 1개 추가",
                1,
                0,
                0,
                RevisionLogGrade.NEW_TERMS,
                0,
                null,
                2L,
                OffsetDateTime.parse("2026-09-14T00:00:00Z"));
    }
}
