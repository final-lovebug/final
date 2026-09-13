package com.ubidict.backend.revisionlog.infra.adapter;

import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.revisionlog.infra.port.DocumentQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 현재 버전이 이전 사전집에 맞춰진 활성 문서만 영향 대상으로 센다. */
@Component("documentQueryAdapterForRevisionLog")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.document.mode", havingValue = "real")
public class DocumentQueryAdapter implements DocumentQueryPort {

    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;

    @Override
    public long countAlignedBelow(Long workspaceId, int dictionaryVersionNo) {
        List<Long> documentIds =
                documentRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId).stream()
                        .map(document -> document.getId())
                        .toList();
        if (documentIds.isEmpty()) {
            return 0;
        }

        return documentVersionRepository.findCurrentSummaries(documentIds).stream()
                .filter(version -> !version.edited())
                .filter(version -> version.dictionaryVersionNo() != null)
                .filter(version -> version.dictionaryVersionNo() < dictionaryVersionNo)
                .count();
    }
}
