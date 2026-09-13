package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.service.DraftReviewRequestService;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
import com.ubidict.backend.support.WithLoginMember;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithLoginMember(7L)
@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(DraftReviewRequestController.class)
class DraftReviewRequestControllerTest {

    private static final Long DRAFT_ID = 100L;
    private static final Long MEMBER_ID = 7L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DraftReviewRequestService draftReviewRequestService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("문서 초안으로 리뷰를 요청하면 201과 리뷰 요청 상세를 응답한다.")
    @Test
    void requestDocumentReview() {
        // given
        given(draftReviewRequestService.requestDocumentReview(any())).willReturn(result(ReviewRequestType.DOCUMENT));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "정산 문서 리뷰", "reviewerMemberIds", List.of(11L)))
                .when()
                .post("/api/draft-documents/{draftDocumentId}/review-request", DRAFT_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("type", equalTo("DOCUMENT"))
                .body("status", equalTo("PENDING_REVIEW"));
    }

    @DisplayName("사전 초안으로 리뷰를 요청하면 201과 리뷰 요청 상세를 응답한다.")
    @Test
    void requestDictionaryReview() {
        // given
        given(draftReviewRequestService.requestDictionaryReview(any()))
                .willReturn(result(ReviewRequestType.DICTIONARY));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", "사전집 리뷰"))
                .when()
                .post("/api/draft-dictionaries/{draftDictionaryId}/review-request", DRAFT_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("type", equalTo("DICTIONARY"));
    }

    @DisplayName("제목이 없으면 400을 응답한다.")
    @Test
    void requestDocumentReview_titleIsBlank() {
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body(Map.of("title", " "))
                .when()
                .post("/api/draft-documents/{draftDocumentId}/review-request", DRAFT_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    private ReviewRequestResult result(ReviewRequestType type) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReviewRequestResult(
                1L, 2L, type, "리뷰", null, MEMBER_ID, ReviewRequestStatus.PENDING_REVIEW, null, null, now, now);
    }
}
