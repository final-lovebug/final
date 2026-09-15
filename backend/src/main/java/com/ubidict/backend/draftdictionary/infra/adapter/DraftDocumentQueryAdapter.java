package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.draftdictionary.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 워크스페이스에 진행 중인 문서 초안이 있는지 본다.
 *
 * <p>{@code DraftDocument}에는 workspaceId가 없고 documentId만 있어 문서를 한 번 거친다(D-37). 스키마를 바꾸는 대신
 * 어댑터가 흡수하는 쪽을 골랐고, 그래서 제공 도메인 둘의 infra를 읽는다.
 */
// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("draftDocumentQueryAdapterForDraftDictionary")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "real")
public class DraftDocumentQueryAdapter implements DraftDocumentQueryPort {
    private final DocumentRepository documentRepository;
    private final DraftDocumentRepository draftDocumentRepository;

    @Override
    public boolean hasOngoingDraft(Long workspaceId) {
        List<Long> documentIds =
                documentRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId).stream()
                        .map(Document::getId)
                        .toList();
        if (documentIds.isEmpty()) {
            return false;
        }
        return draftDocumentRepository.existsByDocumentIdInAndStatusNotAndDeletedAtIsNull(
                documentIds, DraftDocumentStatus.REVISED);
    }
}
