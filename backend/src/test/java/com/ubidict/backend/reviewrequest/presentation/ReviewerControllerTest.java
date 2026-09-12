package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.service.ReviewerService;
import com.ubidict.backend.reviewrequest.service.model.ReviewerResult;
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
@WebMvcTest(ReviewerController.class)
class ReviewerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewerService reviewerService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("리뷰어를 지정하면 201과 지정 결과를 응답한다.")
    @Test
    void assign() {
        // given
        given(reviewerService.assign(any())).willReturn(new ReviewerResult(3L, 1L, 2L, OffsetDateTime.now(), 1L));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "memberId": 2
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/reviewers?memberId={memberId}", 1L, 1L)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("reviewerId", equalTo(3))
                .body("memberId", equalTo(2));
    }

    @DisplayName("리뷰어 회원 식별자가 없으면 400을 응답한다.")
    @Test
    void assign_memberIdIsMissing() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/review-requests/{reviewRequestId}/reviewers?memberId={memberId}", 1L, 1L)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }
}
