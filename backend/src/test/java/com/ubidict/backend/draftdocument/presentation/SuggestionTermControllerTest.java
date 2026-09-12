package com.ubidict.backend.draftdocument.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdocument.domain.*;
import com.ubidict.backend.draftdocument.service.SuggestionTermService;
import com.ubidict.backend.draftdocument.service.model.*;
import com.ubidict.backend.member.infra.security.JwtProvider;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(SuggestionTermController.class)
class SuggestionTermControllerTest {
    @Autowired
    MockMvc mvc;

    @MockitoBean
    SuggestionTermService service;

    @MockitoBean
    JwtProvider jwtProvider;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mvc);
    }

    private SuggestionTermResult result() {
        var n = OffsetDateTime.now();
        return new SuggestionTermResult(
                1L, 2L, new TextRange(0, 1), "a", "b", SuggestionTermStatus.PENDING, null, null, 1L, n, n);
    }

    @Test
    void add() {
        given(service.add(any())).willReturn(result());
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"anchor\":{\"startOffset\":0,\"endOffset\":1},\"originTerm\":\"a\",\"suggestionTerm\":\"b\"}")
                .post("/api/draft-documents/2/suggestion-terms?memberId=1")
                .then()
                .statusCode(201)
                .body("id", equalTo(1));
    }

    @Test
    void search_returnsPageFormat() {
        given(service.search(any(SuggestionTermSearchQuery.class)))
                .willReturn(new PageResult<>(java.util.List.of(result()), 0, 20, 1));
        RestAssuredMockMvc.given()
                .get("/api/draft-documents/2/suggestion-terms")
                .then()
                .statusCode(200)
                .body("page", equalTo(0))
                .body("size", equalTo(20))
                .body("totalElements", equalTo(1));
    }

    @Test
    void patch() {
        given(service.edit(any())).willReturn(result());
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"originTerm\":\"x\"}")
                .patch("/api/suggestion-terms/1?memberId=1")
                .then()
                .statusCode(200);
    }

    @Test
    void delete() {
        RestAssuredMockMvc.given().delete("/api/suggestion-terms/1").then().statusCode(204);
    }

    @Test
    void add_blank_returns400() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"anchor\":{\"startOffset\":0,\"endOffset\":1},\"originTerm\":\"\",\"suggestionTerm\":\"b\"}")
                .post("/api/draft-documents/2/suggestion-terms?memberId=1")
                .then()
                .statusCode(400);
    }

    @DisplayName("제안어 시작 위치가 끝 위치보다 뒤면 400과 도메인 오류 코드를 응답한다.")
    @Test
    void add_endBeforeStart() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"anchor\":{\"startOffset\":2,\"endOffset\":1},\"originTerm\":\"a\",\"suggestionTerm\":\"b\"}")
                .post("/api/draft-documents/2/suggestion-terms?memberId=1")
                .then()
                .statusCode(400)
                .body("code", equalTo("DRAFT_DOCUMENT_INVALID_ANCHOR"));
    }

    @Test
    void invalid_sort_returns400() {
        RestAssuredMockMvc.given()
                .get("/api/draft-documents/2/suggestion-terms?sort=body,asc")
                .then()
                .statusCode(400);
    }

    @DisplayName("제안어를 수용하면 200을 응답한다.")
    @Test
    void accept() {
        given(service.accept(any())).willReturn(result());

        RestAssuredMockMvc.given()
                .post("/api/suggestion-terms/1/acceptance?memberId=1")
                .then()
                .statusCode(200)
                .body("id", equalTo(1));
    }

    @DisplayName("제안어를 거절하면 200을 응답한다.")
    @Test
    void reject() {
        given(service.reject(any())).willReturn(result());

        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"rejectReason\":\"고유명사\"}")
                .post("/api/suggestion-terms/1/rejection?memberId=1")
                .then()
                .statusCode(200);
    }

    @DisplayName("거절 사유가 비어 있으면 400을 응답한다.")
    @Test
    void reject_reasonIsBlank() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{\"rejectReason\":\" \"}")
                .post("/api/suggestion-terms/1/rejection?memberId=1")
                .then()
                .statusCode(400);
    }
}
