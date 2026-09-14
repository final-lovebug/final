package com.ubidict.backend.document.infra;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class LabelRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private LabelRepository labelRepository;

    @Autowired
    private DocumentLabelRepository documentLabelRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    private Long workspaceId;
    private Long otherWorkspaceId;

    @BeforeEach
    void createWorkspaces() {
        workspaceId = saveWorkspace("개발팀");
        otherWorkspaceId = saveWorkspace("플랫폼팀");
    }

    /**
     * 오타로 같은 뜻의 라벨이 갈라지는 것을 DB가 막는다는 것이 라벨 모델의 전제다.
     */
    @DisplayName("같은 워크스페이스에 같은 이름의 라벨을 넣으면 유니크 제약에 걸린다.")
    @Test
    void save_nameIsDuplicatedInWorkspace() {
        // given
        labelRepository.save(
                LabelFixture.label().workspaceId(workspaceId).name("설계").build());
        em.flush();

        // when & then
        assertThatThrownBy(() -> {
                    labelRepository.save(LabelFixture.label()
                            .workspaceId(workspaceId)
                            .name("설계")
                            .build());
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @DisplayName("워크스페이스가 다르면 같은 이름의 라벨이 허용된다.")
    @Test
    void save_nameIsReusedAcrossWorkspaces() {
        // when
        labelRepository.save(
                LabelFixture.label().workspaceId(workspaceId).name("설계").build());
        labelRepository.save(
                LabelFixture.label().workspaceId(otherWorkspaceId).name("설계").build());
        em.flush();
        em.clear();

        // then
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId))
                .hasSize(1);
        assertThat(labelRepository.findAllByWorkspaceIdOrderByNameAsc(otherWorkspaceId))
                .hasSize(1);
    }

    @DisplayName("라벨 목록은 이름 오름차순으로 조회된다.")
    @Test
    void findAllByWorkspaceIdOrderByNameAsc() {
        // given
        labelRepository.save(
                LabelFixture.label().workspaceId(workspaceId).name("정산").build());
        labelRepository.save(
                LabelFixture.label().workspaceId(workspaceId).name("결제").build());
        em.flush();
        em.clear();

        // when
        List<Label> labels = labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId);

        // then
        assertThat(labels).extracting(Label::getName).containsExactly("결제", "정산");
    }

    @DisplayName("같은 문서에 같은 라벨을 두 번 붙이면 유니크 제약에 걸린다.")
    @Test
    void save_documentLabelIsDuplicated() {
        // given
        Long documentId = saveDocument();
        Long labelId = labelRepository
                .save(LabelFixture.label().workspaceId(workspaceId).name("설계").build())
                .getId();
        em.flush();
        documentLabelRepository.save(DocumentLabel.of(documentId, labelId, 1L));
        em.flush();

        // when & then
        assertThatThrownBy(() -> {
                    documentLabelRepository.save(DocumentLabel.of(documentId, labelId, 1L));
                    em.flush();
                })
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private Long saveWorkspace(String name) {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().name(name).build());
        em.flush();

        return workspace.getId();
    }

    private Long saveDocument() {
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        em.flush();

        return document.getId();
    }
}
