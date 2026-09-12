package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("documentQueryAdapterForDraftDocument")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.document.mode", havingValue = "real")
public class DocumentQueryAdapter implements DocumentQueryPort {
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;

    @Override
    public Optional<DocumentSnapshot> read(Long documentId) {
        return documentRepository.findByIdAndDeletedAtIsNull(documentId).flatMap(this::toSnapshot);
    }

    private Optional<DocumentSnapshot> toSnapshot(Document document) {
        return documentVersionRepository
                .findByDocumentIdAndVersionVersionNo(document.getId(), document.getCurrentVersionNo())
                .map(version -> new DocumentSnapshot(
                        document.getId(),
                        document.getWorkspaceId(),
                        document.getCurrentVersionNo(),
                        version.getBody()));
    }
}
