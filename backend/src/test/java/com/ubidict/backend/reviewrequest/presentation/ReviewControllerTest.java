package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.service.ReviewService;
import com.ubidict.backend.reviewrequest.service.model.ReviewProgressResult;
import com.ubidict.backend.reviewrequest.service.model.ReviewResult;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
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
@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewService reviewService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("리뷰를 제출하면 201과 제출 결과를 응답한다.")
    @Test
    void submit() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        given(reviewService.submit(any()))
                .willReturn(new ReviewResult(3L, 1L, 2L, 0, ReviewVerdict.APPROVED, now, now));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "targetRound": 0,
                          "verdict": "APPROVED"
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/reviews?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("reviewId", equalTo(3))
                .body("verdict", equalTo("APPROVED"));
    }

    @DisplayName("판정이 없으면 리뷰 제출 요청에 400을 응답한다.")
    @Test
    void submit_verdictIsMissing() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "targetRound": 0
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/reviews?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("리뷰 진행률을 조회하면 정족수와 최신 판정 집계를 응답한다.")
    @Test
    void progress() {
        // given
        given(reviewService.progress(1L, 2L)).willReturn(new ReviewProgressResult(2, 2, 0, true));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/review-requests/{reviewRequestId}/review-progress?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("requiredReviewerCount", equalTo(2))
                .body("approvedCount", equalTo(2))
                .body("changesRequestedCount", equalTo(0))
                .body("reviseEligible", equalTo(true));
    }

    @DisplayName("비참여자가 리뷰 이력을 조회하면 리소스를 노출하지 않고 404를 응답한다.")
    @Test
    void list_notParticipant() {
        // given
        willThrow(new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND))
                .given(reviewService)
                .list(1L, 2L, null);

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/review-requests/{reviewRequestId}/reviews?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("WORKSPACE_NOT_FOUND"));
    }

    @DisplayName("리뷰할 수 없는 상태에서 제출하면 409를 응답한다.")
    @Test
    void submit_notReviewableStatus() {
        // given
        willThrow(new BusinessException(
                        com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode
                                .REVIEW_REQUEST_NOT_REVIEWABLE_STATUS))
                .given(reviewService)
                .submit(any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "targetRound": 0,
                          "verdict": "APPROVED"
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/reviews?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("REVIEW_REQUEST_NOT_REVIEWABLE_STATUS"));
    }
}
