package com.ubidict.backend.document.presentation;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.document.service.model.CreateDocumentCommand;
import com.ubidict.backend.document.service.model.DocumentResult;
import com.ubidict.backend.document.service.model.DocumentSummaryResult;
import com.ubidict.backend.document.service.model.DocumentVersionResult;
import com.ubidict.backend.document.service.model.DocumentVersionSummaryResult;
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
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long WORKSPACE_ID = 10L;
    private static final Long DOCUMENT_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("문서를 생성하면 201과 상세를 응답한다.")
    @Test
    void create() {
        // given
        given(documentService.create(any(CreateDocumentCommand.class))).willReturn(documentResult());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "결제 도메인 설계", "content": "회원은 결제할 수 있다.", "labels": ["설계"]}
                        """)
                .when()
                .post("/api/workspaces/{workspaceId}/documents?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("documentId", equalTo(DOCUMENT_ID.intValue()))
                .body("currentVersionNo", equalTo(1))
                .body("outdated", equalTo(false))
                .body("labels", contains("설계"));
    }

    @DisplayName("제목이 비어 있으면 400과 공통 검증 실패 코드를 응답한다.")
    @Test
    void create_titleIsBlank() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "  ", "content": "본문"}
                        """)
                .when()
                .post("/api/workspaces/{workspaceId}/documents?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("본문이 10,001자면 400을 응답한다.")
    @Test
    void create_contentIsTooLong() {
        // given
        String tooLong = "가".repeat(10_001);

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"title\": \"제목\", \"content\": \"" + tooLong + "\"}")
                .when()
                .post("/api/workspaces/{workspaceId}/documents?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("라벨이 6개면 400을 응답한다.")
    @Test
    void create_labelLimitIsExceeded() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "제목", "content": "본문",
                         "labels": ["하나", "둘", "셋", "넷", "다섯", "여섯"]}
                        """)
                .when()
                .post("/api/workspaces/{workspaceId}/documents?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("memberId가 없으면 400을 응답한다.")
    @Test
    void create_memberIdIsMissing() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "제목", "content": "본문"}
                        """)
                .when()
                .post("/api/workspaces/{workspaceId}/documents", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    /**
     * 목록에 10,000자 × N을 실을 이유가 없다. 응답 형태로 못 박는다.
     */
    @DisplayName("문서 목록은 200을 응답하고 본문을 담지 않는다.")
    @Test
    void readAll() {
        // given
        given(documentService.readAll(eq(WORKSPACE_ID), eq(MEMBER_ID), eq(null)))
                .willReturn(List.of(summaryResult()));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/documents?memberId={memberId}", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(1))
                .body("[0].title", equalTo("결제 도메인 설계"))
                .body("[0].content", nullValue());
    }

    @DisplayName("라벨 필터를 넘기면 서비스로 전달된다.")
    @Test
    void readAll_filterByLabel() {
        // given
        given(documentService.readAll(eq(WORKSPACE_ID), eq(MEMBER_ID), eq("설계")))
                .willReturn(List.of(summaryResult()));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/documents?memberId={memberId}&label=설계", WORKSPACE_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(1));
    }

    @DisplayName("문서 상세는 200과 본문을 응답한다.")
    @Test
    void read() {
        // given
        given(documentService.read(WORKSPACE_ID, DOCUMENT_ID, MEMBER_ID)).willReturn(documentResult());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}",
                        WORKSPACE_ID,
                        DOCUMENT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", equalTo("회원은 결제할 수 있다."));
    }

    @DisplayName("없는 문서를 조회하면 404와 도메인 에러 코드를 응답한다.")
    @Test
    void read_documentIsAbsent() {
        // given
        willThrow(new BusinessException(DocumentErrorCode.DOCUMENT_NOT_FOUND))
                .given(documentService)
                .read(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}",
                        WORKSPACE_ID,
                        DOCUMENT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("DOCUMENT_NOT_FOUND"));
    }

    @DisplayName("문서를 수정하면 204를 응답한다.")
    @Test
    void update() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"title": "정산 도메인 설계", "labels": ["정산"]}
                        """)
                .when()
                .patch(
                        "/api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}",
                        WORKSPACE_ID,
                        DOCUMENT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("문서를 삭제하면 204를 응답한다.")
    @Test
    void delete() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete(
                        "/api/workspaces/{workspaceId}/documents/{documentId}?memberId={memberId}",
                        WORKSPACE_ID,
                        DOCUMENT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    @DisplayName("버전 이력은 200을 응답하고 본문을 담지 않는다.")
    @Test
    void readVersions() {
        // given
        given(documentService.readVersions(WORKSPACE_ID, DOCUMENT_ID, MEMBER_ID))
                .willReturn(List.of(new DocumentVersionSummaryResult(1, OffsetDateTime.now(), null, MEMBER_ID)));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/workspaces/{workspaceId}/documents/{documentId}/versions?memberId={memberId}",
                        WORKSPACE_ID,
                        DOCUMENT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("[0].versionNo", equalTo(1))
                .body("[0].body", nullValue());
    }

    @DisplayName("특정 버전 조회는 200과 그 시점 본문을 응답한다.")
    @Test
    void readVersion() {
        // given
        given(documentService.readVersion(WORKSPACE_ID, DOCUMENT_ID, 1, MEMBER_ID))
                .willReturn(new DocumentVersionResult(1, "첫 본문", OffsetDateTime.now(), null, MEMBER_ID));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/workspaces/{workspaceId}/documents/{documentId}/versions/1?memberId={memberId}",
                        WORKSPACE_ID,
                        DOCUMENT_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("body", equalTo("첫 본문"));
    }

    private static DocumentResult documentResult() {
        return new DocumentResult(
                DOCUMENT_ID,
                WORKSPACE_ID,
                "결제 도메인 설계",
                "회원은 결제할 수 있다.",
                1,
                false,
                null,
                List.of("설계"),
                MEMBER_ID,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }

    private static DocumentSummaryResult summaryResult() {
        return new DocumentSummaryResult(
                DOCUMENT_ID,
                "결제 도메인 설계",
                1,
                false,
                List.of("설계"),
                MEMBER_ID,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }
}
