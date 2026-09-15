package com.ubidict.backend.notification.presentation;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.member.infra.security.JwtProvider;
import com.ubidict.backend.notification.domain.NotificationTargetType;
import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.notification.exception.NotificationErrorCode;
import com.ubidict.backend.notification.service.NotificationService;
import com.ubidict.backend.notification.service.model.NotificationResult;
import com.ubidict.backend.notification.service.model.NotificationSearchQuery;
import com.ubidict.backend.support.WithLoginMember;
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
class NotificationControllerTest extends com.ubidict.backend.support.ControllerTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long MEMBER_ID = 2L;
    private static final Long NOTIFICATION_ID = 42L;

    @Autowired
    private MockMvc mockMvc;



    @BeforeEach
    void setUp() {
        RestAssuredMockMvc.mockMvc(mockMvc);
    }

    @DisplayName("알림 목록을 조회하면 200과 페이징 응답을 내려준다.")
    @Test
    void search() {
        // given
        given(notificationService.search(any(NotificationSearchQuery.class)))
                .willReturn(new PageResult<>(List.of(notificationResult()), 0, 20, 1));

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/notifications", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("content[0].notificationId", equalTo(NOTIFICATION_ID.intValue()))
                .body("content[0].read", is(false))
                .body("content[0].title", equalTo("결제 문서 개정안이 반영되었습니다"))
                .body("totalElements", equalTo(1));
    }

    @DisplayName("정렬 화이트리스트 밖의 필드로 정렬하면 400을 응답한다.")
    @Test
    void search_invalidSort() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/notifications?sort=title,desc", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("페이지 크기가 상한을 넘으면 400을 응답한다.")
    @Test
    void search_sizeTooLarge() {
        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/notifications?size=101", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.BAD_REQUEST.value());
    }

    @DisplayName("안 읽은 알림 수를 조회하면 200과 개수를 내려준다.")
    @Test
    void unreadCount() {
        // given
        given(notificationService.countUnread(WORKSPACE_ID, MEMBER_ID)).willReturn(3L);

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .get("/api/workspaces/{workspaceId}/notifications/unread-count", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("unreadCount", equalTo(3));
    }

    @DisplayName("알림을 읽음 처리하면 200과 갱신된 알림을 내려준다.")
    @Test
    void markRead() {
        // given
        given(notificationService.markRead(WORKSPACE_ID, NOTIFICATION_ID, MEMBER_ID))
                .willReturn(readNotificationResult());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .patch(
                        "/api/workspaces/{workspaceId}/notifications/{notificationId}/read",
                        WORKSPACE_ID,
                        NOTIFICATION_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("read", is(true));
    }

    @DisplayName("남의 알림을 읽음 처리하면 403이 아니라 404를 응답한다.")
    @Test
    void markRead_notRecipient() {
        // given
        willThrow(new BusinessException(NotificationErrorCode.NOTIFICATION_NOT_FOUND))
                .given(notificationService)
                .markRead(anyLong(), anyLong(), anyLong());

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .patch(
                        "/api/workspaces/{workspaceId}/notifications/{notificationId}/read",
                        WORKSPACE_ID,
                        NOTIFICATION_ID)
                .then()
                .statusCode(HttpStatus.NOT_FOUND.value())
                .body("code", equalTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND.name()));
    }

    @DisplayName("모두 읽음 처리하면 바뀐 개수를 내려준다.")
    @Test
    void markAllRead() {
        // given
        given(notificationService.markAllRead(WORKSPACE_ID, MEMBER_ID)).willReturn(5);

        // when & then
        RestAssuredMockMvc.given()
                .when()
                .patch("/api/workspaces/{workspaceId}/notifications/read-all", WORKSPACE_ID)
                .then()
                .statusCode(HttpStatus.OK.value())
                .body("updated", equalTo(5));
    }

    private NotificationResult notificationResult() {
        return new NotificationResult(
                NOTIFICATION_ID,
                WORKSPACE_ID,
                NotificationType.REVISED,
                NotificationTargetType.REVIEW_REQUEST,
                100L,
                "결제 문서 개정안이 반영되었습니다",
                "문서 r6 발행",
                false,
                null,
                OffsetDateTime.now());
    }

    private NotificationResult readNotificationResult() {
        return new NotificationResult(
                NOTIFICATION_ID,
                WORKSPACE_ID,
                NotificationType.REVISED,
                NotificationTargetType.REVIEW_REQUEST,
                100L,
                "결제 문서 개정안이 반영되었습니다",
                "문서 r6 발행",
                true,
                OffsetDateTime.now(),
                OffsetDateTime.now());
    }
}
