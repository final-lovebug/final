package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 초안을 찾지 못하고 용어 목록도 비어 있다고 답하는 스텁.
 */
@Component("draftDictionaryQueryStubForReviewRequest")
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "stub", matchIfMissing = true)
public class DraftDictionaryQueryStub implements DraftDictionaryQueryPort {

    @Override
    public Optional<DraftDictionarySnapshot> read(Long draftDictionaryId) {
        return Optional.empty();
    }

    @Override
    public List<NewTermSnapshot> readFinalTerms(Long draftDictionaryId) {
        return List.of();
    }
}
