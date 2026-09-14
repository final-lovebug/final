package com.ubidict.backend.reviewrequest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDocumentQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private DraftDocumentQueryAdapter adapter;

    private Long workspaceId;

    @BeforeEach
    void createWorkspace() {
        adapter = new DraftDocumentQueryAdapter(draftDocumentRepository, documentRepository);
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
    }

    @DisplayName("초안의 기준 버전과 본문을 읽고 워크스페이스는 원본 문서에서 해석한다.")
    @Test
    void read_returnsSnapshot() {
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        DraftDocument draft = draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(document.getId())
                .baseVersionNo(3)
                .draftBody("교정한 본문")
                .build());
        em.flush();
        em.clear();

        Optional<DraftDocumentSnapshot> snapshot = adapter.read(draft.getId());

        assertThat(snapshot)
                .get()
                .extracting(
                        DraftDocumentSnapshot::draftDocumentId,
                        DraftDocumentSnapshot::documentId,
                        DraftDocumentSnapshot::workspaceId,
                        DraftDocumentSnapshot::baseVersionNo,
                        DraftDocumentSnapshot::draftBody)
                .containsExactly(draft.getId(), document.getId(), workspaceId, 3, "교정한 본문");
    }

    @DisplayName("삭제된 초안은 읽히지 않는다.")
    @Test
    void read_draftIsDeleted() {
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        DraftDocument draft = draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(document.getId())
                .build());
        draft.delete();
        em.flush();
        em.clear();

        assertThat(adapter.read(draft.getId())).isEmpty();
    }

    @DisplayName("원본 문서가 삭제되면 워크스페이스를 해석할 수 없어 읽히지 않는다.")
    @Test
    void read_documentIsDeleted() {
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        DraftDocument draft = draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(document.getId())
                .build());
        document.delete();
        em.flush();
        em.clear();

        assertThat(adapter.read(draft.getId())).isEmpty();
    }
}
