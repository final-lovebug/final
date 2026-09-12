package com.ubidict.backend.document.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentLabel;
import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.LabelFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DocumentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private DocumentLabelRepository documentLabelRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Long workspaceId;
    private Long otherWorkspaceId;

    @BeforeEach
    void createWorkspaces() {
        workspaceId = saveWorkspace("개발팀");
        otherWorkspaceId = saveWorkspace("플랫폼팀");
    }

    @DisplayName("문서를 저장하면 생성 시각과 수정 시각이 채워진다.")
    @Test
    void save_auditingFieldsAreSet() {
        // when
        Document saved = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        em.flush();

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @DisplayName("문서는 워크스페이스와 식별자가 모두 맞아야 조회된다.")
    @Test
    void findByIdAndWorkspaceIdAndDeletedAtIsNull() {
        // given
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        em.flush();
        em.clear();

        // when
        Optional<Document> found =
                documentRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(document.getId(), workspaceId);

        // then
        assertThat(found).isPresent();
    }

    /**
     * 데이터 격리(NFR-WS-001)의 핵심. 남의 워크스페이스에 그 문서가 있다는 사실이 드러나지 않아야 한다.
     */
    @DisplayName("다른 워크스페이스의 식별자로는 문서가 조회되지 않는다.")
    @Test
    void findByIdAndWorkspaceIdAndDeletedAtIsNull_workspaceIsDifferent() {
        // given
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        em.flush();
        em.clear();

        // when
        Optional<Document> found =
                documentRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(document.getId(), otherWorkspaceId);

        // then
        assertThat(found).isEmpty();
    }

    @DisplayName("삭제된 문서는 조회되지 않는다.")
    @Test
    void findByIdAndWorkspaceIdAndDeletedAtIsNull_documentIsDeleted() {
        // given
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        document.delete();
        em.flush();
        em.clear();

        // when
        Optional<Document> found =
                documentRepository.findByIdAndWorkspaceIdAndDeletedAtIsNull(document.getId(), workspaceId);

        // then
        assertThat(found).isEmpty();
    }

    @DisplayName("문서 목록은 생성일시 내림차순으로 조회된다.")
    @Test
    void findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc() {
        // given
        documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).title("먼저").build());
        em.flush();
        documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).title("나중").build());
        em.flush();
        em.clear();

        // when
        List<Document> documents =
                documentRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId);

        // then
        assertThat(documents).extracting(Document::getTitle).containsExactly("나중", "먼저");
    }

    @DisplayName("문서 목록에 다른 워크스페이스의 문서는 들어오지 않는다.")
    @Test
    void findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc_isolatesWorkspace() {
        // given
        documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .title("내 문서")
                .build());
        documentRepository.save(DocumentFixture.document()
                .workspaceId(otherWorkspaceId)
                .title("남의 문서")
                .build());
        em.flush();
        em.clear();

        // when
        List<Document> documents =
                documentRepository.findAllByWorkspaceIdAndDeletedAtIsNullOrderByCreatedAtDesc(workspaceId);

        // then
        assertThat(documents).extracting(Document::getTitle).containsExactly("내 문서");
    }

    @DisplayName("라벨 필터는 해당 라벨이 붙은 문서만 준다.")
    @Test
    void findAllByWorkspaceIdAndLabelName() {
        // given
        Document tagged = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .title("설계 문서")
                .build());
        documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .title("라벨 없는 문서")
                .build());
        Label label = labelRepository.save(
                LabelFixture.label().workspaceId(workspaceId).name("설계").build());
        em.flush();
        documentLabelRepository.save(DocumentLabel.of(tagged.getId(), label.getId(), 1L));
        em.flush();
        em.clear();

        // when
        List<Document> documents = documentRepository.findAllByWorkspaceIdAndLabelName(workspaceId, "설계");

        // then
        assertThat(documents).extracting(Document::getTitle).containsExactly("설계 문서");
    }

    @DisplayName("라벨 필터에 삭제된 문서는 들어오지 않는다.")
    @Test
    void findAllByWorkspaceIdAndLabelName_documentIsDeleted() {
        // given
        Document tagged = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspaceId)
                .title("설계 문서")
                .build());
        Label label = labelRepository.save(
                LabelFixture.label().workspaceId(workspaceId).name("설계").build());
        em.flush();
        documentLabelRepository.save(DocumentLabel.of(tagged.getId(), label.getId(), 1L));
        tagged.delete();
        em.flush();
        em.clear();

        // when
        List<Document> documents = documentRepository.findAllByWorkspaceIdAndLabelName(workspaceId, "설계");

        // then
        assertThat(documents).isEmpty();
    }

    private Long saveWorkspace(String name) {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().name(name).build());
        em.flush();

        return workspace.getId();
    }
}
