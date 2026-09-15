package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.service.ReexamineService;
import com.ubidict.backend.reviewrequest.service.model.ReexamineResult;
import com.ubidict.backend.support.WithLoginMember;
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

@WithLoginMember(2L)
class ReexamineControllerTest extends com.ubidict.backend.support.ControllerTest {

    @Autowired
    private MockMvc mockMvc;



    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("재교정을 수행하면 새 회차와 수행 일시를 응답한다.")
    @Test
    void perform() {
        // given
        OffsetDateTime performedAt = OffsetDateTime.now();
        given(reexamineService.perform(any())).willReturn(new ReexamineResult(3L, 1L, 1, 2L, List.of(4L), performedAt));

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "proposedBody": "재교정 본문",
                          "addressedCommentIds": [4]
                        }
                        """)
                .when()
                .post("/api/review-requests/{reviewRequestId}/reexaminations", 1L)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("round", equalTo(1));
    }

    @DisplayName("변경요청 상태가 아니면 재교정에 409를 응답한다.")
    @Test
    void perform_notChangesRequested() {
        // given
        willThrow(new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REEXAMINABLE))
                .given(reexamineService)
                .perform(any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("{}")
                .when()
                .post("/api/review-requests/{reviewRequestId}/reexaminations", 1L)
                .then()
                .statusCode(HttpStatus.CONFLICT.value())
                .body("code", equalTo("REVIEW_REQUEST_NOT_REEXAMINABLE"));
    }
}
