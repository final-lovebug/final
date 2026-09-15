package com.ubidict.backend.notification.infra.port;

import java.util.List;
import java.util.Optional;

/**
 * 리뷰 요청을 되짚어 조회한다.
 *
 * <p><b>이 포트가 필요한 이유</b> — 구독하는 이벤트 5종 중 {@code workspaceId}를 담은 것은 {@code ReviewRequestCreatedEvent}
 * 하나뿐이다. 나머지는 {@code reviewRequestId}만 담아, 알림을 어느 워크스페이스에 걸지도 누구에게 보낼지도 이벤트만으로는 알 수 없다.
 * {@code ARCHITECTURE.md}가 이것을 허용한다 — 「상태가 필요한 컨슈머는 식별자로 다시 조회한다」.
 */
public interface ReviewRequestQueryPort {

    /**
     * 없거나 삭제됐으면 빈 Optional. 알림을 만들 수 없다는 뜻이며 예외가 아니다 — 리뷰 요청이 지워진 뒤 이벤트가 재수신되는 것은 정상이다.
     */
    Optional<ReviewRequestSnapshot> findSnapshot(Long reviewRequestId);

    /**
     * 지정된 리뷰어의 회원 식별자. 지정된 리뷰어가 없으면 빈 목록이고, 그때 리뷰 요청 도착 알림은 만들어지지 않는다.
     */
    List<Long> reviewerMemberIds(Long reviewRequestId);
}
