package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 초안과 그 원본 문서를 함께 읽어 개정안 조립에 필요한 값을 채운다.
 *
 * <p>제공 도메인 둘(document·draftdocument)의 infra를 읽는다 — DraftDocument에 workspaceId가 없어서 문서를 거쳐야 하고, 스키마를 바꾸지
 * 않기로 한 D-37의 선례를 그대로 따른다. 문서가 삭제됐으면 빈 결과를 준다.
 */
// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("draftDocumentQueryAdapterForReviewRequest")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "real")
public class DraftDocumentQueryAdapter implements DraftDocumentQueryPort {
    private final DraftDocumentRepository draftDocumentRepository;
    private final DocumentRepository documentRepository;

    @Override
    public Optional<DraftDocumentSnapshot> read(Long draftDocumentId) {
        return draftDocumentRepository
                .findByIdAndDeletedAtIsNull(draftDocumentId)
                .flatMap(draft -> documentRepository
                        .findByIdAndDeletedAtIsNull(draft.getDocumentId())
                        .map(Document::getWorkspaceId)
                        .map(workspaceId -> new DraftDocumentSnapshot(
                                draft.getId(),
                                draft.getDocumentId(),
                                workspaceId,
                                draft.getBaseVersionNo(),
                                draft.getDraftBody())));
    }
}
