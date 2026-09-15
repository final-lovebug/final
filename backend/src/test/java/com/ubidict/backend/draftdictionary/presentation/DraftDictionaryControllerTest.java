package com.ubidict.backend.draftdictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.support.WithLoginMember;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WithLoginMember(2L)
class DraftDictionaryControllerTest extends com.ubidict.backend.support.ControllerTest {

    private static final Long MEMBER_ID = 2L;
    private static final Long DRAFT_DICTIONARY_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("워크스페이스 기준으로 사전 초안 목록을 조회하면 200과 페이지 응답을 반환한다.")
    @Test
    void search() {
        given(draftDictionaryService.search(any()))
                .willReturn(new PageResult<>(List.of(draftDictionaryResult()), 0, 20, 1));

        RestAssuredMockMvc.given()
                .queryParam("workspaceId", 1)
                .when()
                .get("/api/draft-dictionaries")
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content.size()", equalTo(1))
                .body("content[0].draftDictionaryId", equalTo(100))
                .body("totalElements", equalTo(1));
    }

    @DisplayName("없는 사전 초안을 조회하면 404와 도메인 오류 코드를 응답한다.")
    @Test
    void read_notFound() {
        given(draftDictionaryService.read(anyLong(), anyLong()))
                .willThrow(new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_FOUND));

        RestAssuredMockMvc.given()
                .when()
                .get("/api/draft-dictionaries/{draftDictionaryId}", DRAFT_DICTIONARY_ID)
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
                .post("/api/draft-dictionaries/{draftDictionaryId}/examine-completion", DRAFT_DICTIONARY_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("EXAMINED"));
    }

    private static DraftDictionaryResult draftDictionaryResult() {
        return draftDictionaryResult(DraftDictionaryStatus.EXAMINING);
    }

    private static DraftDictionaryResult draftDictionaryResult(DraftDictionaryStatus status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new DraftDictionaryResult(DRAFT_DICTIONARY_ID, 1L, null, List.of(20L, 30L), status, MEMBER_ID, now, now);
    }
}
