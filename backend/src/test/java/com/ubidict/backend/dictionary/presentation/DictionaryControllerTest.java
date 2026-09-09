package com.ubidict.backend.dictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.presentation.dto.ReviseDictionaryRequest;
import com.ubidict.backend.dictionary.presentation.dto.TermRequest;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import com.ubidict.backend.dictionary.service.model.TermResult;
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
@WebMvcTest(DictionaryController.class)
class DictionaryControllerTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final String BASE_PATH = "/api/workspaces/{workspaceId}/dictionary";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DictionaryService dictionaryService;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("사전집 버전을 반영하면 201 Created를 응답한다.")
    @Test
    void revise() {
        // given
        given(dictionaryService.revise(any())).willReturn(dictionaryResult(2, DictionaryStatus.ACTIVE));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .queryParam("memberId", MEMBER_ID)
                .body(new ReviseDictionaryRequest(List.of(new TermRequest("회원", "Member", "가입한 주체"))))
                .when()
                .post(BASE_PATH + "/versions", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("versionNo", equalTo(2))
                .body("status", equalTo("ACTIVE"))
                .body("terms", hasSize(1))
                .body("terms[0].preferredForm", equalTo("회원"));
    }

    @DisplayName("현재 확정본을 조회하면 200 OK와 용어를 응답한다.")
    @Test
    void readActive() {
        // given
        given(dictionaryService.readActive(anyLong(), anyLong()))
                .willReturn(dictionaryResult(1, DictionaryStatus.ACTIVE));

        // when & then
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .when()
                .get(BASE_PATH, WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("dictionaryId", equalTo(100))
                .body("versionNo", equalTo(1))
                .body("terms[0].definition", equalTo("가입한 주체"));
    }

    @DisplayName("버전 이력을 조회하면 200 OK와 용어 수를 응답한다.")
    @Test
    void readVersions() {
        // given
        given(dictionaryService.readVersions(anyLong(), anyLong()))
                .willReturn(List.of(
                        new DictionaryVersionResult(
                                101L, 2, DictionaryStatus.ACTIVE, OffsetDateTime.now(), MEMBER_ID, 2L),
                        new DictionaryVersionResult(
                                100L, 1, DictionaryStatus.ARCHIVED, OffsetDateTime.now(), MEMBER_ID, 1L)));

        // when & then
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .when()
                .get(BASE_PATH + "/versions", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("", hasSize(2))
                .body("[0].versionNo", equalTo(2))
                .body("[0].termCount", equalTo(2))
                .body("[1].status", equalTo("ARCHIVED"));
    }

    @DisplayName("특정 버전을 조회하면 200 OK를 응답한다.")
    @Test
    void readVersion() {
        // given
        given(dictionaryService.readVersion(anyLong(), anyInt(), anyLong()))
                .willReturn(dictionaryResult(1, DictionaryStatus.ARCHIVED));

        // when & then
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .when()
                .get(BASE_PATH + "/versions/{versionNo}", WORKSPACE_ID, 1)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("status", equalTo("ARCHIVED"));
    }

    @DisplayName("용어가 비어 있으면 400 Bad Request를 응답한다.")
    @Test
    void revise_termsAreEmpty() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .queryParam("memberId", MEMBER_ID)
                .body(new ReviseDictionaryRequest(List.of()))
                .when()
                .post(BASE_PATH + "/versions", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("표준어가 비어 있으면 400 Bad Request를 응답한다.")
    @Test
    void revise_preferredFormIsBlank() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .queryParam("memberId", MEMBER_ID)
                .body(new ReviseDictionaryRequest(List.of(new TermRequest("  ", null, "가입한 주체"))))
                .when()
                .post(BASE_PATH + "/versions", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("memberId가 없으면 400 Bad Request를 응답한다.")
    @Test
    void readActive_memberIdIsMissing() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get(BASE_PATH, WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("사전집이 없으면 404 Not Found를 응답한다.")
    @Test
    void readActive_dictionaryDoesNotExist() {
        // given
        willThrow(new BusinessException(DictionaryErrorCode.DICTIONARY_NOT_FOUND))
                .given(dictionaryService)
                .readActive(anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .when()
                .get(BASE_PATH, WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("DICTIONARY_NOT_FOUND"));
    }

    private DictionaryResult dictionaryResult(int versionNo, DictionaryStatus status) {
        return new DictionaryResult(
                100L,
                WORKSPACE_ID,
                versionNo,
                status,
                OffsetDateTime.now(),
                MEMBER_ID,
                List.of(new TermResult(1000L, "회원", "Member", "가입한 주체")));
    }
}
