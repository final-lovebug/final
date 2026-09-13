package com.ubidict.backend.notification.domain;

import static com.ubidict.backend.notification.fixture.NotificationFixture.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.notification.exception.NotificationErrorCode;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationTest {

    @DisplayName("알림을 만들면 안 읽은 상태이고 읽은 시각이 없다.")
    @Test
    void create() {
        // when
        Notification notification = notification().build();

        // then
        assertThat(notification.isRead()).isFalse();
        assertThat(notification.getReadAt()).isNull();
    }

    @DisplayName("읽음으로 표시하면 읽음 여부와 읽은 시각이 함께 채워진다.")
    @Test
    void markRead() {
        // given
        Notification notification = notification().build();

        // when
        notification.markRead();

        // then
        assertThat(notification.isRead()).isTrue();
        assertThat(notification.getReadAt()).isNotNull();
    }

    @DisplayName("이미 읽은 알림을 다시 읽음 처리해도 처음 읽은 시각이 유지된다.")
    @Test
    void markRead_idempotent() {
        // given
        Notification notification = notification().build();
        notification.markRead();
        OffsetDateTime firstReadAt = notification.getReadAt();

        // when
        notification.markRead();

        // then
        assertThat(notification.getReadAt()).isEqualTo(firstReadAt);
    }

    @DisplayName("읽음 여부와 읽은 시각은 항상 같은 말을 한다.")
    @Test
    void readInvariant() {
        // given
        Notification notification = notification().build();

        // when & then — 생성 직후
        assertThat(notification.isRead()).isEqualTo(notification.getReadAt() != null);

        // when & then — 읽은 뒤
        notification.markRead();
        assertThat(notification.isRead()).isEqualTo(notification.getReadAt() != null);
    }

    @DisplayName("수신자 본인인지 확인한다.")
    @Test
    void isRecipient() {
        // given
        Notification notification = notification().recipientId(7L).build();

        // when & then
        assertThat(notification.isRecipient(7L)).isTrue();
        assertThat(notification.isRecipient(8L)).isFalse();
    }

    @DisplayName("제목이 상한을 넘으면 잘라서 저장한다.")
    @Test
    void create_truncatesTitle() {
        // given
        String tooLong = "가".repeat(Notification.TITLE_MAX_LENGTH + 10);

        // when
        Notification notification = notification().title(tooLong).build();

        // then
        assertThat(notification.getTitle()).hasSize(Notification.TITLE_MAX_LENGTH);
    }

    @DisplayName("중복 방지 키가 비어 있으면 알림을 만들 수 없다.")
    @Test
    void create_failsWithoutDedupeKey() {
        // when & then
        assertThatThrownBy(() -> notification().dedupeKey("  ").build())
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_INVALID_DEDUPE_KEY);
    }
}
