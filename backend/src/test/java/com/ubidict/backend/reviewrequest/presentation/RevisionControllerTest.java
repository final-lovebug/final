package com.ubidict.backend.reviewrequest.presentation;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.service.RevisionService;
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

    @DisplayName("문서 개정안 직접 등록 엔드포인트는 제공하지 않는다.")
    @Test
    void document_isInternalized() {
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
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @DisplayName("사전 개정안 직접 등록 엔드포인트는 제공하지 않는다.")
    @Test
    void dictionary_isInternalized() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "dictionaryId": 10,
                          "baseVersionNo": 1,
                          "draftDictionaryId": 20
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/revision-dictionaries?memberId={memberId}", 1L, 1L)
                .then()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value());
    }
}
