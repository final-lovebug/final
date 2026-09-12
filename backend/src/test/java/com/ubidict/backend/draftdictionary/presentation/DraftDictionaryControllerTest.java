package com.ubidict.backend.draftdictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.draftdictionary.service.model.CreateDraftDictionaryCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
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

    @DisplayName("사전 초안을 생성하면 201과 상세를 응답한다.")
    @Test
    void create() {
        given(draftDictionaryService.create(any(CreateDraftDictionaryCommand.class)))
                .willReturn(draftDictionaryResult());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"workspaceId": 1, "dictionaryId": 10, "sourceDocumentIds": [20, 30]}
                        """)
                .when()
                .post("/api/draft-dictionaries?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("draftDictionaryId", equalTo(DRAFT_DICTIONARY_ID.intValue()))
                .body("status", equalTo("EXAMINING"));
    }

    @DisplayName("사전집이 없는 첫 회차에는 dictionaryId 없이 사전 초안을 생성한다.")
    @Test
    void create_dictionaryIdIsNull() {
        given(draftDictionaryService.create(any(CreateDraftDictionaryCommand.class)))
                .willReturn(draftDictionaryResult());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {"workspaceId": 1, "sourceDocumentIds": [20, 30]}
                        """)
                .when()
                .post("/api/draft-dictionaries?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("dictionaryId", nullValue());
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

    private static DraftDictionaryResult draftDictionaryResult() {
        OffsetDateTime now = OffsetDateTime.now();
        return new DraftDictionaryResult(
                DRAFT_DICTIONARY_ID, 1L, null, List.of(20L, 30L), DraftDictionaryStatus.EXAMINING, MEMBER_ID, now, now);
    }
}
