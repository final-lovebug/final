package com.ubidict.backend.notification.infra.adapter;

import com.ubidict.backend.notification.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.notification.infra.port.ReviewRequestSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 「찾지 못했다」를 정직하게 돌려주는 스텁. 빈 Optional·빈 목록이면 알림이 만들어지지 않을 뿐 흐름이 깨지지 않는다.
 */
@Component
@ConditionalOnProperty(name = "app.crossdomain.review-request.mode", havingValue = "stub", matchIfMissing = true)
public class ReviewRequestQueryStub implements ReviewRequestQueryPort {

    @Override
    public Optional<ReviewRequestSnapshot> findSnapshot(Long reviewRequestId) {
        return Optional.empty();
    }

    @Override
    public List<Long> reviewerMemberIds(Long reviewRequestId) {
        return List.of();
    }
}
