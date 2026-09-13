package com.ubidict.backend.draftdictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.draftdictionary.service.model.ExamineProgressResult;
import com.ubidict.backend.member.infra.security.JwtProvider;
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
@WebMvcTest(DraftDictionaryController.class)
class DraftDictionaryControllerTest {

    private static final Long MEMBER_ID = 2L;
    private static final Long DRAFT_DICTIONARY_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DraftDictionaryService draftDictionaryService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("없는 사전 초안을 조회하면 404와 도메인 오류 코드를 응답한다.")
    @Test
    void read_notFound() {
        given(draftDictionaryService.read(anyLong(), anyLong()))
                .willThrow(new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_FOUND));

        RestAssuredMockMvc.given()
                .when()
                .get("/api/draft-dictionaries/{draftDictionaryId}?memberId={memberId}", DRAFT_DICTIONARY_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("DRAFT_DICTIONARY_NOT_FOUND"));
    }

    @DisplayName("교정을 완료하면 EXAMINED 상태를 응답한다.")
    @Test
    void completeExamine() {
        given(draftDictionaryService.completeExamine(any(CompleteExamineCommand.class)))
                .willReturn(draftDictionaryResult(DraftDictionaryStatus.EXAMINED));

        RestAssuredMockMvc.given()
                .when()
                .post(
                        "/api/draft-dictionaries/{draftDictionaryId}/examine-completion?memberId={memberId}",
                        DRAFT_DICTIONARY_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("EXAMINED"));
    }

    @DisplayName("초안의 상태별 교정 진행률을 응답한다.")
    @Test
    void readExamineProgress() {
        given(draftDictionaryService.readExamineProgress(DRAFT_DICTIONARY_ID, MEMBER_ID))
                .willReturn(new ExamineProgressResult(7, 1, 1, 1, 1, 1, 1));

        RestAssuredMockMvc.given()
                .when()
                .get(
                        "/api/draft-dictionaries/{draftDictionaryId}/examine-progress?memberId={memberId}",
                        DRAFT_DICTIONARY_ID,
                        MEMBER_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("total", equalTo(7))
                .body("pending", equalTo(1))
                .body("kept", equalTo(1))
                .body("approved", equalTo(1))
                .body("merged", equalTo(1))
                .body("rejected", equalTo(1))
                .body("onHold", equalTo(1));
    }

    private static DraftDictionaryResult draftDictionaryResult() {
        return draftDictionaryResult(DraftDictionaryStatus.EXAMINING);
    }

    private static DraftDictionaryResult draftDictionaryResult(DraftDictionaryStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new DraftDictionaryResult(DRAFT_DICTIONARY_ID, 1L, null, List.of(20L, 30L), status, MEMBER_ID, now, now);
    }
}
