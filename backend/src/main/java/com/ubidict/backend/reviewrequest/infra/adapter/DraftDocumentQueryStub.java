package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 초안을 찾지 못한 것으로 답하는 스텁. 발행이 근거를 읽지 못해 거절된다.
 */
@Component("draftDocumentQueryStubForReviewRequest")
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "stub", matchIfMissing = true)
public class DraftDocumentQueryStub implements DraftDocumentQueryPort {

    @Override
    public Optional<DraftDocumentSnapshot> read(Long draftDocumentId) {
        return Optional.empty();
    }
}
