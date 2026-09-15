package com.ubidict.backend.draftdictionary.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * DraftDocument에는 workspaceId가 없어 어댑터가 문서를 거쳐 찾는다(D-37). 그 경로를 검증한다.
 */
class DraftDocumentQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private DraftDocumentQueryAdapter adapter;
    private Long workspaceId;

    @BeforeEach
    void setUp() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        workspaceId = workspace.getId();
        adapter = new DraftDocumentQueryAdapter(documentRepository, draftDocumentRepository);
    }

    @DisplayName("워크스페이스의 문서에 반영완료가 아닌 초안이 있으면 진행 중으로 본다.")
    @Test
    void hasOngoingDraft_statusIsNotRevised() {
        saveDraft(saveDocument(workspaceId), DraftDocumentStatus.EXAMINING);
        em.flush();
        em.clear();

        assertThat(adapter.hasOngoingDraft(workspaceId)).isTrue();
    }

    @DisplayName("반영완료 초안만 있으면 진행 중이 아니다.")
    @Test
    void hasOngoingDraft_statusIsRevised() {
        saveDraft(saveDocument(workspaceId), DraftDocumentStatus.REVISED);
        em.flush();
        em.clear();

        assertThat(adapter.hasOngoingDraft(workspaceId)).isFalse();
    }

    @DisplayName("다른 워크스페이스 문서의 초안은 보지 않는다.")
    @Test
    void hasOngoingDraft_otherWorkspace() {
        Workspace other = workspaceRepository.save(WorkspaceFixture.workspace().build());
        saveDraft(saveDocument(other.getId()), DraftDocumentStatus.EXAMINING);
        em.flush();
        em.clear();

        assertThat(adapter.hasOngoingDraft(workspaceId)).isFalse();
    }

    @DisplayName("문서가 하나도 없으면 진행 중이 아니다.")
    @Test
    void hasOngoingDraft_workspaceHasNoDocument() {
        assertThat(adapter.hasOngoingDraft(workspaceId)).isFalse();
    }

    private Long saveDocument(Long ownerWorkspaceId) {
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(ownerWorkspaceId).build());
        return document.getId();
    }

    private void saveDraft(Long documentId, DraftDocumentStatus status) {
        draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(documentId)
                .status(status)
                .build());
    }
}
