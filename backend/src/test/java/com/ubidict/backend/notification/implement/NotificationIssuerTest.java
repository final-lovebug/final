package com.ubidict.backend.notification.implement;

import static com.ubidict.backend.notification.fixture.NotificationFixture.notification;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.notification.domain.Notification;
import com.ubidict.backend.notification.domain.NotificationType;
import com.ubidict.backend.notification.service.model.IssueNotificationCommand;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationIssuerTest {

    private static final Long WORKSPACE_ID = 1L;
    private static final Long TARGET_ID = 100L;

    @Mock
    private NotificationAppender notificationAppender;

    @InjectMocks
    private NotificationIssuer notificationIssuer;

    @DisplayName("수신자가 없으면 저장을 시도하지 않는다.")
    @Test
    void issue_withoutRecipients() {
        // when
        int issued = notificationIssuer.issue(command(List.of()));

        // then — 채널 해석은 설정이 없으면 기본값을 '쓰므로', 수신자가 없을 때 불리면 안 된다
        assertThat(issued).isZero();
        verifyNoInteractions(notificationAppender);
    }

    @DisplayName("수신자 수만큼 저장을 시도한다.")
    @Test
    void issue_appendsPerRecipient() {
        // given
        given(notificationAppender.append(any(Notification.class)))
                .willAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        // when
        int issued = notificationIssuer.issue(command(List.of(2L, 3L, 4L)));

        // then
        assertThat(issued).isEqualTo(3);
        verify(notificationAppender, times(3)).append(any(Notification.class));
    }

    @DisplayName("중복으로 걸러진 알림은 세지 않는다.")
    @Test
    void issue_countsOnlySaved() {
        // given
        given(notificationAppender.append(any(Notification.class)))
                .willReturn(Optional.of(notification().build()))
                .willReturn(Optional.empty());

        // when
        int issued = notificationIssuer.issue(command(List.of(2L, 3L)));

        // then
        assertThat(issued).isEqualTo(1);
    }

    @DisplayName("커맨드의 값이 알림에 그대로 실린다.")
    @Test
    void issue_carriesCommandValues() {
        // given
        given(notificationAppender.append(any(Notification.class)))
                .willAnswer(invocation -> Optional.of(invocation.getArgument(0)));

        // when
        notificationIssuer.issue(command(List.of(2L)));

        // then
        verify(notificationAppender)
                .append(org.mockito.ArgumentMatchers.argThat(
                        notification -> notification.getRecipientId().equals(2L)
                                && notification.getTargetId().equals(TARGET_ID)
                                && notification.getWorkspaceId().equals(WORKSPACE_ID)));
    }

    private IssueNotificationCommand command(List<Long> recipients) {
        return IssueNotificationCommand.withoutActor(
                WORKSPACE_ID,
                TARGET_ID,
                NotificationType.REVISED,
                recipients,
                new NotificationContent("결제 문서 개정안이 반영되었습니다", "문서 r6 발행"),
                "RR_REVISED:100:6");
    }
}
