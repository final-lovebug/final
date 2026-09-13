package com.ubidict.backend.notification.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.ubidict.backend.notification.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.notification.infra.port.WorkspaceQueryPort;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationRecipientResolverTest {

    private static final Long REVIEW_REQUEST_ID = 100L;
    private static final Long WORKSPACE_ID = 1L;

    @Mock
    private ReviewRequestQueryPort reviewRequestQueryPort;

    @Mock
    private WorkspaceQueryPort workspaceQueryPort;

    @InjectMocks
    private NotificationRecipientResolver resolver;

    @DisplayName("리뷰 요청 도착은 지정된 리뷰어에게만 간다.")
    @Test
    void forReviewRequestReceived() {
        // given
        given(reviewRequestQueryPort.reviewerMemberIds(REVIEW_REQUEST_ID)).willReturn(List.of(2L, 3L));

        // when
        List<Long> recipients = resolver.forReviewRequestReceived(REVIEW_REQUEST_ID, 1L);

        // then
        assertThat(recipients).containsExactly(2L, 3L);
    }

    @DisplayName("지정된 리뷰어가 없으면 리뷰 요청 도착 알림을 아무도 받지 않는다.")
    @Test
    void forReviewRequestReceived_withoutReviewer() {
        // given
        given(reviewRequestQueryPort.reviewerMemberIds(REVIEW_REQUEST_ID)).willReturn(List.of());

        // when
        List<Long> recipients = resolver.forReviewRequestReceived(REVIEW_REQUEST_ID, 1L);

        // then
        assertThat(recipients).isEmpty();
    }

    @DisplayName("리뷰어에 요청자 본인이 섞여 있어도 자기 알림은 만들지 않는다.")
    @Test
    void forReviewRequestReceived_excludesActor() {
        // given
        given(reviewRequestQueryPort.reviewerMemberIds(REVIEW_REQUEST_ID)).willReturn(List.of(1L, 2L));

        // when
        List<Long> recipients = resolver.forReviewRequestReceived(REVIEW_REQUEST_ID, 1L);

        // then
        assertThat(recipients).containsExactly(2L);
    }

    @DisplayName("승인·변경요청·취소는 요청자에게만 간다.")
    @Test
    void forRequester() {
        // when
        List<Long> recipients = resolver.forRequester(1L, 2L);

        // then
        assertThat(recipients).containsExactly(1L);
    }

    @DisplayName("요청자가 스스로 한 일은 알리지 않는다.")
    @Test
    void forRequester_excludesActor() {
        // when
        List<Long> recipients = resolver.forRequester(1L, 1L);

        // then
        assertThat(recipients).isEmpty();
    }

    @DisplayName("행위자를 안 넘기면 요청자가 그대로 수신자다.")
    @Test
    void forRequester_withoutActor() {
        // when & then
        assertThat(resolver.forRequester(1L)).containsExactly(1L);
    }

    @DisplayName("행위자를 안 넘기면 참여자 전원이 그대로 수신자다.")
    @Test
    void forWorkspace_withoutActor() {
        // given
        given(workspaceQueryPort.participantMemberIds(WORKSPACE_ID)).willReturn(List.of(1L, 2L));

        // when & then
        assertThat(resolver.forWorkspace(WORKSPACE_ID)).containsExactly(1L, 2L);
    }

    @DisplayName("반영완료는 워크스페이스 참여자 전원에게 간다.")
    @Test
    void forWorkspace() {
        // given
        given(workspaceQueryPort.participantMemberIds(WORKSPACE_ID)).willReturn(List.of(1L, 2L, 3L));

        // when
        List<Long> recipients = resolver.forWorkspace(WORKSPACE_ID, 2L);

        // then
        assertThat(recipients).containsExactly(1L, 3L);
    }

    @DisplayName("중복된 수신자는 한 번만 남는다.")
    @Test
    void deduplicatesRecipients() {
        // given
        given(workspaceQueryPort.participantMemberIds(WORKSPACE_ID)).willReturn(List.of(1L, 1L, 2L));

        // when
        List<Long> recipients = resolver.forWorkspace(WORKSPACE_ID, null);

        // then
        assertThat(recipients).containsExactly(1L, 2L);
    }

    @DisplayName("수신자 목록에 null이 섞여 있어도 걸러진다.")
    @Test
    void ignoresNullRecipients() {
        // given
        given(workspaceQueryPort.participantMemberIds(WORKSPACE_ID)).willReturn(Arrays.asList(1L, null, 2L));

        // when
        List<Long> recipients = resolver.forWorkspace(WORKSPACE_ID, null);

        // then
        assertThat(recipients).containsExactly(1L, 2L);
    }
}
