package com.ubidict.backend.draftdictionary.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DocumentQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private DocumentQueryAdapter adapter;
    private Long workspaceId;

    @BeforeEach
    void setUp() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
        adapter = new DocumentQueryAdapter(documentRepository, documentVersionRepository, dictionaryRepository);
    }

    @DisplayName("사전집이 없으면 기준 버전이 null인 문서도 추출 대상이다.")
    @Test
    void isExtractable_dictionaryIsAbsent() {
        Long documentId = saveDocumentWithVersion(null, false);
        em.flush();
        em.clear();

        assertThat(adapter.isExtractable(documentId)).isTrue();
    }

    @DisplayName("활성 사전집 버전과 기준 버전이 같고 편집되지 않았으면 추출 대상이다.")
    @Test
    void isExtractable_alignedWithActiveDictionary() {
        saveActiveDictionary(2);
        Long documentId = saveDocumentWithVersion(2, false);
        em.flush();
        em.clear();

        assertThat(adapter.isExtractable(documentId)).isTrue();
    }

    @DisplayName("직접 편집된 버전은 추출 대상이 아니다.")
    @Test
    void isExtractable_editedVersion() {
        saveActiveDictionary(2);
        Long documentId = saveDocumentWithVersion(2, true);
        em.flush();
        em.clear();

        assertThat(adapter.isExtractable(documentId)).isFalse();
    }

    @DisplayName("기준 사전집 버전이 활성 버전보다 낮으면 추출 대상이 아니다.")
    @Test
    void isExtractable_staleDictionaryVersion() {
        saveActiveDictionary(3);
        Long documentId = saveDocumentWithVersion(2, false);
        em.flush();
        em.clear();

        assertThat(adapter.isExtractable(documentId)).isFalse();
    }

    @DisplayName("없는 문서는 추출 대상이 아니다.")
    @Test
    void isExtractable_documentIsAbsent() {
        assertThat(adapter.isExtractable(404L)).isFalse();
    }

    private void saveActiveDictionary(int versionNo) {
        dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspaceId)
                .versionNo(versionNo)
                .build());
    }

    private Long saveDocumentWithVersion(Integer dictionaryVersionNo, boolean edited) {
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .currentVersionNo(1)
                .build());
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(document.getId())
                .versionNo(1)
                .dictionaryVersionNo(dictionaryVersionNo)
                .edited(edited)
                .build());
        return document.getId();
    }
}
