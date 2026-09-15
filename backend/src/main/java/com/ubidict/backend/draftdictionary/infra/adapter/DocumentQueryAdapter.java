package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdictionary.infra.port.DocumentQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 추출 대상 여부를 G-12 조건으로 판정한다.
 *
 * <p>판정 규칙은 {@link DocumentVersion#isAligned(Integer)} 하나만 쓴다. 같은 규칙이 두 벌이 되지 않게
 * document 도메인이 static으로 둔 것을 그대로 호출한다.
 *
 * <p><b>프로퍼티 키는 {@code document}지만 dictionary의 Repository도 읽는다.</b> 판정에 활성 사전집 버전이 필요하기 때문이다.
 * 키는 「이 포트를 real로 쓸지」를 고르는 것이고 「그 제공 도메인을 차단할지」가 아니다.
 */
// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("documentQueryAdapterForDraftDictionary")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.document.mode", havingValue = "real")
public class DocumentQueryAdapter implements DocumentQueryPort {
    private final DocumentRepository documentRepository;
    private final DocumentVersionRepository documentVersionRepository;
    private final DictionaryRepository dictionaryRepository;

    @Override
    public boolean isExtractable(Long documentId) {
        return documentRepository
                .findByIdAndDeletedAtIsNull(documentId)
                .flatMap(this::currentVersionOf)
                .map(entry -> entry.version().isAligned(activeVersionNo(entry.workspaceId())))
                .orElse(false);
    }

    private java.util.Optional<CurrentVersion> currentVersionOf(Document document) {
        return documentVersionRepository
                .findByDocumentIdAndVersionVersionNo(document.getId(), document.getCurrentVersionNo())
                .map(version -> new CurrentVersion(document.getWorkspaceId(), version));
    }

    private Integer activeVersionNo(Long workspaceId) {
        return dictionaryRepository
                .findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE)
                .map(Dictionary::versionNo)
                .orElse(null);
    }

    private record CurrentVersion(Long workspaceId, DocumentVersion version) {}
}
