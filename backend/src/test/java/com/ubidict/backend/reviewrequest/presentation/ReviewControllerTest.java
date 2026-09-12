package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.service.ReviewService;
import com.ubidict.backend.reviewrequest.service.model.ReviewResult;
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
}
