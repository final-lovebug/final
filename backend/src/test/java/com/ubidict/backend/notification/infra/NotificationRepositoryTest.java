package com.ubidict.backend.notification.infra;

import static com.ubidict.backend.notification.fixture.NotificationFixture.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class NotificationRepositoryTest extends RepositoryTestSupport {

    private static final Long RECIPIENT_ID = 1L;
    private static final Long WORKSPACE_ID = 10L;

    @Autowired
    private NotificationRepository notificationRepository;

    @DisplayName("같은 수신자에게 같은 중복 방지 키로 두 번 저장하면 거부된다.")
    @Test
    void dedupeKeyIsUniquePerRecipient() {
        // given
        notificationRepository.saveAndFlush(notification()
                .recipientId(RECIPIENT_ID)
                .dedupeKey("RR_REVISED:100:6")
                .build());

        // when & then
        assertThatThrownBy(() -> notificationRepository.saveAndFlush(notification()
                        .recipientId(RECIPIENT_ID)
                        .dedupeKey("RR_REVISED:100:6")
                        .build()))
                .isInstanceOf(Exception.class);
    }

    @DisplayName("수신자가 다르면 같은 중복 방지 키로도 저장된다.")
    @Test
    void dedupeKeyIsScopedToRecipient() {
        // given
        notificationRepository.saveAndFlush(
                notification().recipientId(1L).dedupeKey("RR_REVISED:100:6").build());

        // when
        Notification other = notificationRepository.saveAndFlush(
                notification().recipientId(2L).dedupeKey("RR_REVISED:100:6").build());

        // then
        assertThat(other.getId()).isNotNull();
    }

    @DisplayName("안 읽은 알림 수를 센다.")
    @Test
    void countUnread() {
        // given
        notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("a")
                .build());
        Notification read = notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("b")
                .build());
        read.markRead();
        notificationRepository.flush();

        // when
        long unread = notificationRepository.countByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
                RECIPIENT_ID, WORKSPACE_ID);

        // then
        assertThat(unread).isEqualTo(1);
    }

    @DisplayName("다른 워크스페이스의 알림은 세지 않는다.")
    @Test
    void countUnread_isolatedByWorkspace() {
        // given
        notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("a")
                .build());
        notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(99L)
                .dedupeKey("b")
                .build());
        notificationRepository.flush();

        // when
        long unread = notificationRepository.countByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
                RECIPIENT_ID, WORKSPACE_ID);

        // then
        assertThat(unread).isEqualTo(1);
    }

    @DisplayName("모두 읽음은 안 읽은 알림만 한 문장으로 바꾼다.")
    @Test
    void markAllRead() {
        // given
        notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("a")
                .build());
        notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("b")
                .build());
        notificationRepository.flush();

        // when
        int updated = notificationRepository.markAllRead(RECIPIENT_ID, WORKSPACE_ID, OffsetDateTime.now());

        // then
        assertThat(updated).isEqualTo(2);
        assertThat(notificationRepository.countByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
                        RECIPIENT_ID, WORKSPACE_ID))
                .isZero();
    }

    @DisplayName("안 읽은 알림만 걸러 조회한다.")
    @Test
    void findUnreadOnly() {
        // given
        Notification read = notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("a")
                .build());
        read.markRead();
        notificationRepository.save(notification()
                .recipientId(RECIPIENT_ID)
                .workspaceId(WORKSPACE_ID)
                .dedupeKey("b")
                .build());
        notificationRepository.flush();

        // when
        var page = notificationRepository.findAllByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
                RECIPIENT_ID, WORKSPACE_ID, PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")));

        // then
        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().getFirst().getDedupeKey()).isEqualTo("b");
    }
}
