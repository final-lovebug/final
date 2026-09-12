package com.ubidict.backend.draftdocument.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.Optional;
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
    private WorkspaceRepository workspaceRepository;

    private DocumentQueryAdapter adapter;
    private Long workspaceId;

    @BeforeEach
    void setUp() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
        adapter = new DocumentQueryAdapter(documentRepository, documentVersionRepository);
    }

    @DisplayName("현재 버전의 본문을 스냅샷으로 읽는다.")
    @Test
    void read_returnsCurrentVersionBody() {
        Long documentId = saveDocument(2);
        saveVersion(documentId, 1, "첫 버전");
        saveVersion(documentId, 2, "현재 버전");
        em.flush();
        em.clear();

        Optional<DocumentSnapshot> snapshot = adapter.read(documentId);

        assertThat(snapshot)
                .get()
                .extracting(
                        DocumentSnapshot::documentId,
                        DocumentSnapshot::workspaceId,
                        DocumentSnapshot::currentVersionNo,
                        DocumentSnapshot::body)
                .containsExactly(documentId, workspaceId, 2, "현재 버전");
    }

    @DisplayName("삭제된 문서는 읽히지 않는다.")
    @Test
    void read_documentIsDeleted() {
        Long documentId = saveDocument(1);
        saveVersion(documentId, 1, "본문");
        documentRepository.findById(documentId).orElseThrow().delete();
        em.flush();
        em.clear();

        assertThat(adapter.read(documentId)).isEmpty();
    }

    @DisplayName("현재 버전 행이 없으면 빈 Optional을 반환한다.")
    @Test
    void read_currentVersionIsAbsent() {
        Long documentId = saveDocument(3);
        saveVersion(documentId, 1, "본문");
        em.flush();
        em.clear();

        assertThat(adapter.read(documentId)).isEmpty();
    }

    private Long saveDocument(int currentVersionNo) {
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .currentVersionNo(currentVersionNo)
                .build());
        return document.getId();
    }

    private void saveVersion(Long documentId, int versionNo, String body) {
        DocumentVersion version = DocumentVersionFixture.documentVersion()
                .documentId(documentId)
                .versionNo(versionNo)
                .body(body)
                .build();
        documentVersionRepository.save(version);
    }
}
