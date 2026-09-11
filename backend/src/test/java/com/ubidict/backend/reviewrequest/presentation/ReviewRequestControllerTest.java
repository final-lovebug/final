package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.service.ReviewRequestService;
import com.ubidict.backend.reviewrequest.service.model.CreateReviewRequestCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewRequestResult;
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
@WebMvcTest(ReviewRequestController.class)
class ReviewRequestControllerTest {

    private static final Long MEMBER_ID = 1L;
    private static final Long REVIEW_REQUEST_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReviewRequestService reviewRequestService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("리뷰 요청을 생성하면 201과 상세를 응답한다.")
    @Test
    void create() {
        // given
        given(reviewRequestService.create(any(CreateReviewRequestCommand.class)))
                .willReturn(reviewRequestResult());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "workspaceId": 10,
                          "type": "DOCUMENT",
                          "title": "결제 문서 리뷰",
                          "description": "결제 문서의 개정안을 검토합니다."
                        }
                        """)
                .when()
                .post("/api/review-requests?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("reviewRequestId", equalTo(REVIEW_REQUEST_ID.intValue()))
                .body("status", equalTo("PENDING_REVIEW"));
    }

    @DisplayName("리뷰 요청 유형이 올바르지 않으면 400을 응답한다.")
    @Test
    void create_typeIsInvalid() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "workspaceId": 10,
                          "type": "INVALID",
                          "title": "결제 문서 리뷰"
                        }
                        """)
                .when()
                .post("/api/review-requests?memberId={memberId}", MEMBER_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("없는 리뷰 요청을 조회하면 404와 도메인 에러 코드를 응답한다.")
    @Test
    void read_notFound() {
        // given
        willThrow(new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_FOUND))
                .given(reviewRequestService)
                .read(anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/review-requests/{reviewRequestId}?memberId={memberId}", REVIEW_REQUEST_ID, MEMBER_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("REVIEW_REQUEST_NOT_FOUND"));
    }

    private static ReviewRequestResult reviewRequestResult() {
        OffsetDateTime now = OffsetDateTime.now();
        return new ReviewRequestResult(
                REVIEW_REQUEST_ID,
                10L,
                ReviewRequestType.DOCUMENT,
                "결제 문서 리뷰",
                "결제 문서의 개정안을 검토합니다.",
                MEMBER_ID,
                ReviewRequestStatus.PENDING_REVIEW,
                null,
                null,
                now,
                now);
    }
}
