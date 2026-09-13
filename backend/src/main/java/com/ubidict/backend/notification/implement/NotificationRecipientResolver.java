package com.ubidict.backend.notification.implement;

import com.ubidict.backend.notification.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.notification.infra.port.WorkspaceQueryPort;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 알림 유형별 수신자를 정한다. <b>수신자 정책이 사는 유일한 곳이다.</b>
 *
 * <table>
 *   <tr><th>유형</th><th>수신자</th></tr>
 *   <tr><td>리뷰요청도착</td><td>지정된 리뷰어. <b>없으면 알림을 만들지 않는다</b></td></tr>
 *   <tr><td>승인·변경요청·취소</td><td>요청자</td></tr>
 *   <tr><td>반영완료</td><td>워크스페이스 참여자 전원</td></tr>
 * </table>
 *
 * <p>「지정된 리뷰어가 없으면 아무에게도 보내지 않는다」가 {@code G-4}(미지정 참여자도 정족수에 산입된다)와 모순되지 않는다 — <b>지정은 알림을 누구에게
 * 보낼지를 정할 뿐 리뷰 자격을 제한하지 않는다.</b> 지정되지 않은 참여자는 알림 없이도 목록에서 요청을 보고 리뷰할 수 있다.
 *
 * <p><b>자기 알림은 억제한다.</b> 행위자 본인에게 「당신이 한 일」을 알리는 것은 소음이다.
 */
@Component
@RequiredArgsConstructor
public class NotificationRecipientResolver {

    private final ReviewRequestQueryPort reviewRequestQueryPort;
    private final WorkspaceQueryPort workspaceQueryPort;

    /**
     * 리뷰 요청 도착 — 지정된 리뷰어에게만.
     */
    public List<Long> forReviewRequestReceived(Long reviewRequestId, Long actorId) {
        return exclude(reviewRequestQueryPort.reviewerMemberIds(reviewRequestId), actorId);
    }

    /**
     * 승인·변경요청·취소 — 요청자에게만.
     */
    public List<Long> forRequester(Long requesterId, Long actorId) {
        return exclude(List.of(requesterId), actorId);
    }

    /**
     * 행위자가 이벤트에 담기지 않은 경우. 뺄 사람이 없으니 요청자가 그대로 수신자다.
     */
    public List<Long> forRequester(Long requesterId) {
        return forRequester(requesterId, null);
    }

    /**
     * 반영완료 — 워크스페이스 참여자 전원. 새 문서·사전집 버전이 생긴 사건이라 요청 관계자를 넘어 전체가 알아야 의미가 있다.
     */
    public List<Long> forWorkspace(Long workspaceId, Long actorId) {
        return exclude(workspaceQueryPort.participantMemberIds(workspaceId), actorId);
    }

    /**
     * 행위자가 이벤트에 담기지 않은 경우. 참여자 전원이 그대로 수신자다.
     */
    public List<Long> forWorkspace(Long workspaceId) {
        return forWorkspace(workspaceId, null);
    }

    /**
     * null과 중복을 걷어내고 행위자를 뺀다. 순서를 보존해 테스트가 순서에 흔들리지 않게 한다.
     */
    private static List<Long> exclude(List<Long> memberIds, Long actorId) {
        Set<Long> distinct = new LinkedHashSet<>();
        for (Long memberId : memberIds) {
            if (memberId == null || memberId.equals(actorId)) {
                continue;
            }
            distinct.add(memberId);
        }

        return List.copyOf(distinct);
    }
}
