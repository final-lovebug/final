package com.ubidict.backend.dictionary.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.presentation.dto.ReviseDictionaryRequest;
import com.ubidict.backend.dictionary.presentation.dto.TermRequest;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import com.ubidict.backend.dictionary.service.model.TermResult;
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
@WebMvcTest(DictionaryController.class)
class DictionaryControllerTest {

    @Test
    @DisplayName("현재 확정본의 용어 목록을 페이지 구조로 응답한다.")
    void readActive_termsArePaged() {
        given(dictionaryService.readActive(anyLong(), anyLong(), any(DictionarySearchQuery.class)))
                .willReturn(dictionaryResult(1, DictionaryStatus.ACTIVE));
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .queryParam("page", 0)
                .queryParam("size", 20)
                .when()
                .get(BASE_PATH, WORKSPACE_ID)
                .then()
                .statusCode(200)
                .body("terms.content", hasSize(1))
                .body("terms.content[0]", not(hasKey("definition")));
    }

    @Test
    @DisplayName("허용되지 않은 정렬 필드는 400으로 응답한다.")
    void readActive_sortIsNotWhitelisted() {
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .queryParam("sort", "definition,asc")
                .when()
                .get(BASE_PATH, WORKSPACE_ID)
                .then()
                .statusCode(400);
    }

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 10L;
    private static final String BASE_PATH = "/api/workspaces/{workspaceId}/dictionary";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DictionaryService dictionaryService;

    /**
     * addFilters=false로 Security 필터 체인은 우회하지만 SecurityConfig가 이 슬라이스에 함께
     * 로드되므로, JwtAuthenticationFilter가 요구하는 JwtProvider를 mock으로 채워 컨텍스트를 띄운다.
     */
    @MockitoBean
    private JwtProvider jwtProvider;

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
                .body("terms.content", hasSize(1))
                .body("terms.content[0].preferredForm", equalTo("회원"));
    }

    @DisplayName("현재 확정본을 조회하면 200 OK와 용어를 응답한다.")
    @Test
    void readActive() {
        // given
        given(dictionaryService.readActive(anyLong(), anyLong(), any(DictionarySearchQuery.class)))
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
                .body("terms.content[0].preferredForm", equalTo("회원"));
    }

    @DisplayName("버전 이력을 조회하면 200 OK와 용어 수를 응답한다.")
    @Test
    void readVersions() {
        // given
        given(dictionaryService.readVersions(anyLong(), anyLong(), anyInt(), anyInt()))
                .willReturn(new PageResult<>(
                        List.of(
                                new DictionaryVersionResult(
                                        101L, 2, DictionaryStatus.ACTIVE, OffsetDateTime.now(), MEMBER_ID, 2L),
                                new DictionaryVersionResult(
                                        100L, 1, DictionaryStatus.ARCHIVED, OffsetDateTime.now(), MEMBER_ID, 1L)),
                        0,
                        20,
                        2));

        // when & then
        RestAssuredMockMvc.given()
                .queryParam("memberId", MEMBER_ID)
                .when()
                .get(BASE_PATH + "/versions", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content", hasSize(2))
                .body("content[0].versionNo", equalTo(2))
                .body("content[0].termCount", equalTo(2))
                .body("content[1].status", equalTo("ARCHIVED"));
    }

    @DisplayName("특정 버전을 조회하면 200 OK를 응답한다.")
    @Test
    void readVersion() {
        // given
        given(dictionaryService.readVersion(anyLong(), anyInt(), anyLong(), any(DictionarySearchQuery.class)))
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
                .readActive(anyLong(), anyLong(), any(DictionarySearchQuery.class));

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
                new PageResult<>(List.of(new TermResult(1000L, "회원", "Member", null)), 0, 20, 1));
    }
}
