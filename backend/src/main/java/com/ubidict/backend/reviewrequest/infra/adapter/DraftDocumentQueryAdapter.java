package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("draftDocumentQueryAdapterForReviewRequest")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "real")
public class DraftDocumentQueryAdapter implements DraftDocumentQueryPort {
    private final DraftDocumentRepository draftDocumentRepository;

    @Override
    public Optional<DraftDocumentSnapshot> read(Long draftDocumentId) {
        return draftDocumentRepository
                .findByIdAndDeletedAtIsNull(draftDocumentId)
                .map(draft -> new DraftDocumentSnapshot(
                        draft.getId(), draft.getDocumentId(), draft.getBaseVersionNo(), draft.getDraftBody()));
    }
}
