package com.ubidict.backend.draftdictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.draftdictionary.domain.CandidateTermOrigin;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.service.CandidateTermService;
import com.ubidict.backend.draftdictionary.service.model.BulkDecisionResult;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import com.ubidict.backend.member.infra.security.JwtProvider;
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

@WebMvcTest(CandidateTermController.class)
@AutoConfigureMockMvc(addFilters = false)
class CandidateTermControllerTest {

    private static final Long MEMBER_ID = 2L;
    private static final Long CANDIDATE_TERM_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CandidateTermService candidateTermService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("후보어를 등재 승인하면 판정 결과를 응답한다.")
    @Test
    void approve() {
        given(candidateTermService.decide(any(DecideCandidateTermCommand.class)))
                .willReturn(result(CandidateTermStatus.REGISTRATION_APPROVED, null, null));

        RestAssuredMockMvc.given()
                .when()
                .post(
                        "/api/candidate-terms/{candidateTermId}/registration-approval?memberId={memberId}",
                        CANDIDATE_TERM_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("REGISTRATION_APPROVED"))
                .body("handledBy", equalTo(MEMBER_ID.intValue()));
    }

    @DisplayName("후보어를 동의어로 편입하면 대상 용어 식별자를 응답한다.")
    @Test
    void merge() {
        given(candidateTermService.decide(any(DecideCandidateTermCommand.class)))
                .willReturn(result(CandidateTermStatus.MERGED_AS_SYNONYM, null, 99L));

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"mergeTargetTermId": 99}
                        """)
                .when()
                .post(
                        "/api/candidate-terms/{candidateTermId}/synonym-merge?memberId={memberId}",
                        CANDIDATE_TERM_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("MERGED_AS_SYNONYM"))
                .body("mergeTargetTermId", equalTo(99));
    }

    @DisplayName("거절 사유가 비어 있으면 후보어를 거절할 수 없다.")
    @Test
    void reject_blankReason() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"rejectReason": " "}
                        """)
                .when()
                .post(
                        "/api/candidate-terms/{candidateTermId}/rejection?memberId={memberId}",
                        CANDIDATE_TERM_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("후보어를 보류하면 판정 결과를 응답한다.")
    @Test
    void hold() {
        given(candidateTermService.decide(any(DecideCandidateTermCommand.class)))
                .willReturn(result(CandidateTermStatus.ON_HOLD, null, null));

        RestAssuredMockMvc.given()
                .when()
                .post("/api/candidate-terms/{candidateTermId}/hold?memberId={memberId}", CANDIDATE_TERM_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("ON_HOLD"));
    }

    @DisplayName("후보어 일괄 판정은 성공과 실패 목록을 응답한다.")
    @Test
    void bulkDecide() {
        given(candidateTermService.bulkDecide(any()))
                .willReturn(new BulkDecisionResult(
                        List.of(10L), List.of(new BulkDecisionResult.Failure(11L, "ERROR_CODE", "실패"))));

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"candidateTermIds": [10, 11], "decision": "ON_HOLD"}
                        """)
                .when()
                .post(
                        "/api/draft-dictionaries/{draftDictionaryId}/candidate-terms/bulk-decision?memberId={memberId}",
                        1L,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("succeeded", hasSize(1))
                .body("succeeded[0]", equalTo(10))
                .body("failed", hasSize(1))
                .body("failed[0].candidateTermId", equalTo(11))
                .body("failed[0].code", equalTo("ERROR_CODE"));
    }

    @DisplayName("일괄 판정 대상이 비어 있으면 요청을 거절한다.")
    @Test
    void bulkDecide_emptyCandidateTermIds() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"candidateTermIds": [], "decision": "ON_HOLD"}
                        """)
                .when()
                .post(
                        "/api/draft-dictionaries/{draftDictionaryId}/candidate-terms/bulk-decision?memberId={memberId}",
                        1L,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("지원하지 않는 상태로 일괄 판정하면 요청을 거절한다.")
    @Test
    void bulkDecide_unsupportedDecision() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"candidateTermIds": [10], "decision": "KEPT"}
                        """)
                .when()
                .post(
                        "/api/draft-dictionaries/{draftDictionaryId}/candidate-terms/bulk-decision?memberId={memberId}",
                        1L,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("DRAFT_DICTIONARY_INVALID_DECISION"));
    }

    private static CandidateTermResult result(CandidateTermStatus status, String rejectReason, Long mergeTargetTermId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new CandidateTermResult(
                CANDIDATE_TERM_ID,
                1L,
                CandidateTermOrigin.EXTRACTED,
                null,
                "후보어",
                "후보어 정의",
                null,
                1,
                status,
                MEMBER_ID,
                rejectReason,
                mergeTargetTermId,
                null,
                List.of(100L),
                List.of("문맥"),
                now,
                now);
    }
}
