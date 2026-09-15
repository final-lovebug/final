package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.service.model.ReviseResult;
import com.ubidict.backend.support.WithLoginMember;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WithLoginMember(2L)
class ReviseControllerTest extends com.ubidict.backend.support.ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("개정안을 발행하면 결과 버전과 수행 일시를 응답한다.")
    @Test
    void perform() {
        // given
        given(reviseService.perform(any())).willReturn(new ReviseResult(3L, 1L, 2, 2L, OffsetDateTime.now()));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/review-requests/{reviewRequestId}/revision", 1L)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("resultVersionNo", equalTo(2));
    }

    @DisplayName("발행 조건을 충족하지 못하면 409를 응답한다.")
    @Test
    void perform_notEligible() {
        // given
        willThrow(new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_ELIGIBLE_FOR_REVISE))
                .given(reviseService)
                .perform(any());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/review-requests/{reviewRequestId}/revision", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("REVIEW_REQUEST_NOT_ELIGIBLE_FOR_REVISE"));
    }

    @DisplayName("ADMIN 미만이 개정안을 발행하면 403을 응답한다.")
    @Test
    void perform_regularPermission() {
        // given
        willThrow(new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_ACCESS_DENIED))
                .given(reviseService)
                .perform(any());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .post("/api/review-requests/{reviewRequestId}/revision", 1L)
                .then()
                .statusCode(HttpStatus.FORBIDDEN.value())
                .body("code", equalTo("REVIEW_REQUEST_ACCESS_DENIED"));
    }
}
