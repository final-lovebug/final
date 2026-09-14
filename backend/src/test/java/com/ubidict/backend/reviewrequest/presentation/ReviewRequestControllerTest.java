package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.service.ReviewRequestService;
import com.ubidict.backend.support.WithLoginMember;
import io.restassured.http.ContentType;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WithLoginMember(1L)
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

    @DisplayName("리뷰 요청 직접 생성 엔드포인트는 제공하지 않는다.")
    @Test
    void create_isInternalized() {
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
                .post("/api/review-requests")
                .then()
                .statusCode(HttpStatus.METHOD_NOT_ALLOWED.value());
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
                .get("/api/review-requests/{reviewRequestId}", REVIEW_REQUEST_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("REVIEW_REQUEST_NOT_FOUND"));
    }
}
