package com.ubidict.backend.document.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentVersion;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.implement.DocumentVersionAppender;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.document.service.DocumentService;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DocumentVersionPublishAdapterTest extends IntegrationTestSupport {

    private static final Long PUBLISHED_BY = 7L;

    @Autowired
    private DocumentService documentService;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private DocumentVersionAppender documentVersionAppender;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    private DocumentVersionPublishAdapter adapter;

    @BeforeEach
    void setUpAdapter() {
        adapter = new DocumentVersionPublishAdapter(documentService);
    }

    @DisplayName("기준 버전으로 교정 반영본을 발행하고 새 버전 번호를 반환한다.")
    @Test
    void publish_usesBaseVersionAndReturnsResultVersionNo() {
        Long workspaceId = createWorkspace();
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        documentVersionAppender.appendFirst(document, "초안 본문", PUBLISHED_BY);

        int resultVersionNo = adapter.publish(document.getId(), 1, "교정 본문", 3, PUBLISHED_BY);

        Document foundDocument = documentRepository.findById(document.getId()).orElseThrow();
        DocumentVersion revised = documentVersionRepository
                .findByDocumentIdAndVersionVersionNo(document.getId(), resultVersionNo)
                .orElseThrow();
        assertThat(resultVersionNo).isEqualTo(2);
        assertThat(foundDocument.getUpdaterId()).isEqualTo(PUBLISHED_BY);
        assertThat(revised.getBody()).isEqualTo("교정 본문");
        assertThat(revised.getDictionaryVersionNo()).isEqualTo(3);
        assertThat(revised.getCreatedBy()).isEqualTo(PUBLISHED_BY);
        assertThat(revised.isEdited()).isFalse();
    }

    @DisplayName("현재 버전과 다른 기준 버전으로는 교정 반영할 수 없다.")
    @Test
    void publish_rejectsWhenBaseVersionDiffers() {
        Long workspaceId = createWorkspace();
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspaceId).build());
        documentVersionAppender.appendFirst(document, "초안 본문", PUBLISHED_BY);

        assertThatThrownBy(() -> adapter.publish(document.getId(), 0, "교정 본문", 3, PUBLISHED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(DocumentErrorCode.DOCUMENT_VERSION_CONFLICT);
    }

    private Long createWorkspace() {
        Long workspaceId =
                workspaceRepository.save(WorkspaceFixture.workspace().build()).getId();
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspaceId)
                .memberId(PUBLISHED_BY)
                .permission(Permission.OWNER)
                .build());
        return workspaceId;
    }
}
