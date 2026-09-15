package com.ubidict.backend.notification.service;

import static com.ubidict.backend.notification.fixture.NotificationFixture.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.exception.NotificationErrorCode;
import com.ubidict.backend.notification.infra.NotificationRepository;
import com.ubidict.backend.notification.service.model.NotificationResult;
import com.ubidict.backend.notification.service.model.NotificationSearchQuery;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotificationServiceTest extends IntegrationTestSupport {

    private static final Long OWNER_ID = 100L;
    private static final Long MEMBER_ID = 200L;
    private static final Long OUTSIDER_ID = 300L;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private Long workspaceId;

    @BeforeEach
    void setUpWorkspace() {
        workspaceId =
                workspaceRepository.save(Workspace.create("결제팀", OWNER_ID)).getId();
        participantRepository.save(Participant.owner(workspaceId, OWNER_ID));
        participantRepository.save(Participant.join(workspaceId, MEMBER_ID, Permission.REGULAR, OWNER_ID));
    }

    @DisplayName("목록은 요청자 본인이 수신자인 알림만 돌려준다.")
    @Test
    void search_returnsOwnOnly() {
        // given
        save(MEMBER_ID, "a");
        save(OWNER_ID, "b");

        // when
        PageResult<NotificationResult> result =
                notificationService.search(NotificationSearchQuery.of(workspaceId, MEMBER_ID, false, 0, 20, null));

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.totalElements()).isEqualTo(1);
    }

    @DisplayName("안 읽은 것만 조회하면 읽은 알림은 빠진다.")
    @Test
    void search_unreadOnly() {
        // given
        Notification read = save(MEMBER_ID, "a");
        save(MEMBER_ID, "b");
        notificationService.markRead(workspaceId, read.getId(), MEMBER_ID);

        // when
        PageResult<NotificationResult> result =
                notificationService.search(NotificationSearchQuery.of(workspaceId, MEMBER_ID, true, 0, 20, null));

        // then
        assertThat(result.content()).hasSize(1);
    }

    @DisplayName("안 읽은 알림 수를 센다.")
    @Test
    void countUnread() {
        // given
        save(MEMBER_ID, "a");
        save(MEMBER_ID, "b");

        // when & then
        assertThat(notificationService.countUnread(workspaceId, MEMBER_ID)).isEqualTo(2);
    }

    @DisplayName("읽음 처리하면 읽음 여부와 읽은 시각이 함께 채워진다.")
    @Test
    void markRead() {
        // given
        Notification notification = save(MEMBER_ID, "a");

        // when
        NotificationResult result = notificationService.markRead(workspaceId, notification.getId(), MEMBER_ID);

        // then
        assertThat(result.read()).isTrue();
        assertThat(result.readAt()).isNotNull();
    }

    @DisplayName("이미 읽은 알림을 다시 읽음 처리해도 성공하고 읽은 시각이 유지된다.")
    @Test
    void markRead_idempotent() {
        // given
        Notification notification = save(MEMBER_ID, "a");
        NotificationResult first = notificationService.markRead(workspaceId, notification.getId(), MEMBER_ID);

        // when
        NotificationResult second = notificationService.markRead(workspaceId, notification.getId(), MEMBER_ID);

        // then
        assertThat(second.readAt()).isEqualTo(first.readAt());
    }

    @DisplayName("남의 알림을 읽음 처리하면 403이 아니라 404다.")
    @Test
    void markRead_notRecipient() {
        // given
        Notification notification = save(OWNER_ID, "a");

        // when & then
        assertThatThrownBy(() -> notificationService.markRead(workspaceId, notification.getId(), MEMBER_ID))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }

    @DisplayName("모두 읽음은 본인의 안 읽은 알림만 바꾼다.")
    @Test
    void markAllRead() {
        // given
        save(MEMBER_ID, "a");
        save(MEMBER_ID, "b");
        save(OWNER_ID, "c");

        // when
        int updated = notificationService.markAllRead(workspaceId, MEMBER_ID);

        // then
        assertThat(updated).isEqualTo(2);
        assertThat(notificationService.countUnread(workspaceId, MEMBER_ID)).isZero();
        assertThat(notificationService.countUnread(workspaceId, OWNER_ID)).isEqualTo(1);
    }

    @DisplayName("참여하지 않은 워크스페이스의 알림은 조회할 수 없다.")
    @Test
    void search_notParticipant() {
        // when & then
        assertThatThrownBy(() -> notificationService.search(
                        NotificationSearchQuery.of(workspaceId, OUTSIDER_ID, false, 0, 20, null)))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_NOT_FOUND);
    }

    private Notification save(Long recipientId, String dedupeKey) {
        return notificationRepository.saveAndFlush(notification()
                .recipientId(recipientId)
                .workspaceId(workspaceId)
                .dedupeKey(dedupeKey)
                .build());
    }
}
