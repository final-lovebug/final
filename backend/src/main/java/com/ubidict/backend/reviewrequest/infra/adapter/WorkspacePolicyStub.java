package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.infra.port.WorkspacePolicyPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 정족수 0을 돌려주는 스텁.
 *
 * <p>룰셋을 읽을 수 없을 때의 값이며, 승인 집계가 언제나 정족수를 채운 것으로 판정된다. 발행 판정의 「1 이상」 경로는 real 어댑터에서만 검증된다.
 */
@Component
@ConditionalOnProperty(name = "app.crossdomain.workspace.mode", havingValue = "stub", matchIfMissing = true)
public class WorkspacePolicyStub implements WorkspacePolicyPort {

    @Override
    public int requiredReviewerCount(Long workspaceId, ReviewRequestType type) {
        return 0;
    }
}
