package com.ubidict.backend.notification.implement;

import com.ubidict.backend.notification.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.notification.infra.port.ReviewRequestSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 알림을 걸 리뷰 요청을 읽는다.
 *
 * <p>구독하는 이벤트 5종 중 {@code workspaceId}를 담은 것은 {@code ReviewRequestCreatedEvent} 하나뿐이고 제목은 어느 이벤트에도 없다.
 * 그래서 알림 문구와 워크스페이스 범위를 식별자로 되짚어 조회한다 — {@code docs/ARCHITECTURE.md}의 「상태가 필요한 컨슈머는 식별자로 다시 조회한다」.
 *
 * <p><b>못 찾으면 예외가 아니라 빈 Optional이다.</b> 리뷰 요청이 지워진 뒤 이벤트가 재수신되는 것은 정상이고, 그때는 알림을 만들지 않고 넘어가면 된다. 다만
 * 조용히 지나가지는 않도록 WARN을 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewRequestSnapshotReader {

    private final ReviewRequestQueryPort reviewRequestQueryPort;

    public Optional<ReviewRequestSnapshot> read(Long reviewRequestId) {
        Optional<ReviewRequestSnapshot> snapshot = reviewRequestQueryPort.findSnapshot(reviewRequestId);
        if (snapshot.isEmpty()) {
            log.warn(
                    "[ReviewRequestSnapshotReader.read] Review request not found. Notification skipped. reviewRequestId={}",
                    reviewRequestId);
        }

        return snapshot;
    }
}
