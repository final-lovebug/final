package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.service.RevisionService;
import com.ubidict.backend.reviewrequest.service.model.RevisionResult;
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

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RevisionController.class)
class RevisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RevisionService revisionService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("문서 개정안을 등록하면 201과 등록 결과를 응답한다.")
    @Test
    void document() {
        // given
        given(revisionService.submitDocument(any())).willReturn(new RevisionResult(3L, 1L, 10L, 1, 20L, "개정 본문", 0));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "documentId": 10,
                          "baseVersionNo": 1,
                          "draftDocumentId": 20,
                          "proposedBody": "개정 본문"
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/revision-documents?memberId={memberId}", 1L, 1L)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("id", equalTo(3))
                .body("proposedBody", equalTo("개정 본문"));
    }

    @DisplayName("문서 개정안 본문이 비어 있으면 400을 응답한다.")
    @Test
    void document_proposedBodyIsBlank() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "documentId": 10,
                          "baseVersionNo": 1,
                          "draftDocumentId": 20,
                          "proposedBody": " "
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/revision-documents?memberId={memberId}", 1L, 1L)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }
}
