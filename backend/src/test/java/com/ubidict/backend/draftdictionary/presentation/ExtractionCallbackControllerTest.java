package com.ubidict.backend.draftdictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryExtractionCallbackService;
import com.ubidict.backend.member.infra.security.JwtProvider;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AI 워커 콜백의 HTTP 계약.
 *
 * <p><b>{@code @WithLoginMember}가 없다.</b> 이 엔드포인트는 인증 주체 없이 동작해야 하므로 principal을 주입하지 않는 것 자체가 검증이다
 * (D-69). permitAll 범위는 {@code SecurityConfigTest}가 따로 본다.
 */
class ExtractionCallbackControllerTest extends com.ubidict.backend.support.ControllerTest {

    @Autowired
    private DraftDictionaryExtractionCallbackService callbackService;

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";

    @Autowired
    private MockMvc mockMvc;



    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("추출 결과 콜백을 받으면 결과를 반영하고 204를 응답한다.")
    @Test
    void complete() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(resultBody())
                .post("/api/internal/llm/extractions/30/result")
                .then()
                .statusCode(204);

        verify(callbackService)
                .complete(
                        eq(30L),
                        eq(REQUEST_ID),
                        eq(List.of(10L)),
                        eq(List.of(new ExtractedTerm(
                                "결제", "정의", "Payment", List.of(10L), 2, List.of("문맥"), List.of("결제", "페이먼트")))));
    }

    @DisplayName("실패 콜백을 받으면 작업을 실패로 기록하고 204를 응답한다.")
    @Test
    void fail() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"requestId":"%s","reason":"모델 응답이 스키마를 만족하지 않습니다.","code":"LLM_SCHEMA_VIOLATION"}
                        """.formatted(REQUEST_ID))
                .post("/api/internal/llm/extractions/30/failure")
                .then()
                .statusCode(204);

        verify(callbackService).fail(30L, REQUEST_ID, "모델 응답이 스키마를 만족하지 않습니다.", "LLM_SCHEMA_VIOLATION");
    }

    @DisplayName("상관 식별자가 없는 콜백은 400으로 거절한다.")
    @Test
    void complete_requestIdMissing() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"sourceDocumentIds\":[10],\"terms\":[]}")
                .post("/api/internal/llm/extractions/30/result")
                .then()
                .statusCode(400)
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("출현 횟수가 1 미만인 후보어가 섞인 콜백은 400으로 거절한다.")
    @Test
    void complete_invalidTerm() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"requestId":"%s","sourceDocumentIds":[10],
                         "terms":[{"form":"결제","proposedDefinition":null,"proposedEnglishName":null,
                                   "occurredDocumentIds":[10],"occurrenceCount":0,"contextSnippets":[]}]}
                        """.formatted(REQUEST_ID))
                .post("/api/internal/llm/extractions/30/result")
                .then()
                .statusCode(400);
    }

    @DisplayName("상관 식별자가 맞지 않으면 403으로 거절한다.")
    @Test
    void complete_forbidden() {
        willThrow(new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_CALLBACK_FORBIDDEN))
                .given(callbackService)
                .complete(any(), anyString(), any(), any());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(resultBody())
                .post("/api/internal/llm/extractions/30/result")
                .then()
                .statusCode(403)
                .body("code", equalTo("DRAFT_DICTIONARY_EXTRACTION_CALLBACK_FORBIDDEN"));
    }

    @DisplayName("없는 작업에 대한 콜백은 404로 거절한다.")
    @Test
    void complete_notFound() {
        willThrow(new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_NOT_FOUND))
                .given(callbackService)
                .complete(any(), anyString(), any(), any());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(resultBody())
                .post("/api/internal/llm/extractions/30/result")
                .then()
                .statusCode(404)
                .body("code", equalTo("DRAFT_DICTIONARY_EXTRACTION_NOT_FOUND"));
    }

    private String resultBody() {
        return """
                {"requestId":"%s","sourceDocumentIds":[10],
                 "terms":[{"form":"결제","proposedDefinition":"정의","proposedEnglishName":"Payment",
                           "occurredDocumentIds":[10],"occurrenceCount":2,"contextSnippets":["문맥"],
                           "variantForms":["결제","페이먼트"]}]}
                """.formatted(REQUEST_ID);
    }
}
