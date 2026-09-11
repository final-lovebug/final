package com.ubidict.backend.draftdocument.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.service.DraftDocumentService;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import com.ubidict.backend.member.infra.security.JwtProvider;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
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
@WebMvcTest(DraftDocumentController.class)
class DraftDocumentControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long DRAFT_DOCUMENT_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DraftDocumentService draftDocumentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("문서 초안을 생성하면 201과 상세를 응답한다.")
    @Test
    void create() {
        // given
        given(draftDocumentService.create(any(CreateDraftDocumentCommand.class)))
                .willReturn(draftDocumentResult("회원은 결제할 수 있다."));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"documentId": 10, "baseVersionNo": 1, "draftBody": "회원은 결제할 수 있다."}
                        """)
                .when()
                .post("/api/draft-documents?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("draftDocumentId", equalTo(DRAFT_DOCUMENT_ID.intValue()))
                .body("status", equalTo("EXAMINING"));
    }

    @DisplayName("원본 문서 식별자가 없으면 400과 공통 검증 실패 코드를 응답한다.")
    @Test
    void create_documentIdIsNull() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"baseVersionNo": 1, "draftBody": "회원은 결제할 수 있다."}
                        """)
                .when()
                .post("/api/draft-documents?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("문서 초안을 조회하면 200과 상세를 응답한다.")
    @Test
    void read() {
        // given
        given(draftDocumentService.read(DRAFT_DOCUMENT_ID, MEMBER_ID)).willReturn(draftDocumentResult("회원은 결제할 수 있다."));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/draft-documents/{draftDocumentId}?memberId={memberId}", DRAFT_DOCUMENT_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("documentId", equalTo(10))
                .body("draftBody", equalTo("회원은 결제할 수 있다."));
    }

    @DisplayName("존재하지 않는 문서 초안을 조회하면 404와 도메인 오류 코드를 응답한다.")
    @Test
    void read_notFound() {
        // given
        given(draftDocumentService.read(anyLong(), anyLong()))
                .willThrow(new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/draft-documents/{draftDocumentId}?memberId={memberId}", DRAFT_DOCUMENT_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("DRAFT_DOCUMENT_NOT_FOUND"));
    }

    @DisplayName("문서 초안 본문을 수정하면 200과 수정된 상세를 응답한다.")
    @Test
    void updateBody() {
        // given
        given(draftDocumentService.updateBody(any(UpdateDraftBodyCommand.class)))
                .willReturn(draftDocumentResult("수정 본문"));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"draftBody": "수정 본문"}
                        """)
                .when()
                .patch("/api/draft-documents/{draftDocumentId}?memberId={memberId}", DRAFT_DOCUMENT_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("draftBody", equalTo("수정 본문"));
    }

    @DisplayName("문서 초안을 삭제하면 204를 응답한다.")
    @Test
    void delete() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .delete("/api/draft-documents/{draftDocumentId}?memberId={memberId}", DRAFT_DOCUMENT_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NO_CONTENT.value());
    }

    private static DraftDocumentResult draftDocumentResult(String draftBody) {
        OffsetDateTime now = OffsetDateTime.now();

        return new DraftDocumentResult(
                DRAFT_DOCUMENT_ID, 10L, 1, draftBody, DraftDocumentStatus.EXAMINING, MEMBER_ID, MEMBER_ID, now, now);
    }
}
