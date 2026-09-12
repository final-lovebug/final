package com.ubidict.backend.reviewrequest.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.service.CommentService;
import com.ubidict.backend.reviewrequest.service.model.CommentResult;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
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
@WebMvcTest(CommentController.class)
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CommentService commentService;

    @MockitoBean
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("코멘트를 추가하면 201과 생성 결과를 응답한다.")
    @Test
    void add() {
        // given
        given(commentService.add(any())).willReturn(commentResult());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "content": "본문 표현을 확인해 주세요."
                        }
                        """)
                .when()
                .post("/api/reviews/{reviewId}/comments?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.CREATED.value())
                .body("commentId", equalTo(3))
                .body("content", equalTo("본문 표현을 확인해 주세요."));
    }

    @DisplayName("코멘트 내용이 비어 있으면 400을 응답한다.")
    @Test
    void add_contentIsBlank() {
        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "content": " "
                        }
                        """)
                .when()
                .post("/api/reviews/{reviewId}/comments?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("COMMON_INVALID_REQUEST"));
    }

    @DisplayName("비참여자가 코멘트 목록을 조회하면 리소스를 노출하지 않고 404를 응답한다.")
    @Test
    void list_notParticipant() {
        // given
        willThrow(new BusinessException(WorkspaceErrorCode.WORKSPACE_NOT_FOUND))
                .given(commentService)
                .list(1L, 2L, null, null);

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/review-requests/{reviewRequestId}/comments?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo("WORKSPACE_NOT_FOUND"));
    }

    @DisplayName("다른 리뷰의 코멘트를 상위로 지정하면 400을 응답한다.")
    @Test
    void add_invalidParent() {
        // given
        willThrow(new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_COMMENT_PARENT))
                .given(commentService)
                .add(any());

        // when & then
        RestAssuredMockMvc.given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "content": "답글",
                          "parentId": 99
                        }
                        """)
                .when()
                .post("/api/reviews/{reviewId}/comments?memberId={memberId}", 1L, 2L)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value())
                .body("code", equalTo("REVIEW_REQUEST_INVALID_COMMENT_PARENT"));
    }

    private static CommentResult commentResult() {
        OffsetDateTime now = OffsetDateTime.now();
        return new CommentResult(3L, 1L, 2L, "본문 표현을 확인해 주세요.", null, null, null, false, now, now, List.of());
    }
}
