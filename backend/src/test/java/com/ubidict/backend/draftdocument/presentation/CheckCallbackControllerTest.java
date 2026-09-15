package com.ubidict.backend.draftdocument.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.CheckSuggestion;
import com.ubidict.backend.draftdocument.service.DraftDocumentCheckCallbackService;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AI 워커 콜백의 HTTP 계약.
 *
 * <p><b>{@code @WithLoginMember}가 없다.</b> 이 엔드포인트는 인증 주체 없이 동작해야 하므로 principal을 주입하지 않는 것 자체가 검증이다
 * (D-69).
 */
class CheckCallbackControllerTest extends com.ubidict.backend.support.ControllerTest {

    @Autowired
    private DraftDocumentCheckCallbackService callbackService;

    private static final String REQUEST_ID = "0d5c6f6e-0000-4000-8000-000000000001";

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("대조 결과 콜백을 받으면 결과를 반영하고 204를 응답한다.")
    @Test
    void complete() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(resultBody())
                .post("/api/internal/llm/checks/40/result")
                .then()
                .statusCode(204);

        verify(callbackService)
                .complete(
                        eq(40L),
                        eq(REQUEST_ID),
                        eq(3),
                        eq(List.of(new CheckSuggestion(new TextRange(10, 12), "유저", "이용자"))));
    }

    @DisplayName("실패 콜백을 받으면 작업을 실패로 기록하고 204를 응답한다.")
    @Test
    void fail() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"requestId":"%s","reason":"모델 호출이 시간 안에 끝나지 않았습니다.","code":"LLM_TIMEOUT"}
                        """.formatted(REQUEST_ID))
                .post("/api/internal/llm/checks/40/failure")
                .then()
                .statusCode(204);

        verify(callbackService).fail(40L, REQUEST_ID, "모델 호출이 시간 안에 끝나지 않았습니다.", "LLM_TIMEOUT");
    }

    @DisplayName("상관 식별자가 없는 콜백은 400으로 거절한다.")
    @Test
    void complete_requestIdMissing() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"documentVersionNo\":3,\"suggestions\":[]}")
                .post("/api/internal/llm/checks/40/result")
                .then()
                .statusCode(400)
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("제안 용어가 빈 콜백은 400으로 거절한다.")
    @Test
    void complete_blankSuggestionTerm() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"requestId":"%s","documentVersionNo":3,
                         "suggestions":[{"anchor":{"startOffset":10,"endOffset":12},
                                         "originTerm":"유저","suggestionTerm":" "}]}
                        """.formatted(REQUEST_ID))
                .post("/api/internal/llm/checks/40/result")
                .then()
                .statusCode(400);
    }

    @DisplayName("상관 식별자가 맞지 않으면 403으로 거절한다.")
    @Test
    void complete_forbidden() {
        willThrow(new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN))
                .given(callbackService)
                .complete(any(), anyString(), anyInt(), any());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(resultBody())
                .post("/api/internal/llm/checks/40/result")
                .then()
                .statusCode(403)
                .body("code", equalTo("DRAFT_DOCUMENT_CHECK_CALLBACK_FORBIDDEN"));
    }

    @DisplayName("없는 작업에 대한 콜백은 404로 거절한다.")
    @Test
    void complete_notFound() {
        willThrow(new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_NOT_FOUND))
                .given(callbackService)
                .complete(any(), anyString(), anyInt(), any());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(resultBody())
                .post("/api/internal/llm/checks/40/result")
                .then()
                .statusCode(404)
                .body("code", equalTo("DRAFT_DOCUMENT_CHECK_NOT_FOUND"));
    }

    private String resultBody() {
        return """
                {"requestId":"%s","documentVersionNo":3,
                 "suggestions":[{"anchor":{"startOffset":10,"endOffset":12},
                                 "originTerm":"유저","suggestionTerm":"이용자"}]}
                """.formatted(REQUEST_ID);
    }
}
