package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDocumentServiceTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 1L;

    private Long documentId;

    @Autowired
    private DraftDocumentService draftDocumentService;

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @BeforeEach
    void setUpDocument() {
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.workspace().createdBy(MEMBER_ID).build());
        participantRepository.save(ParticipantFixture.participant()
                .workspaceId(workspace.getId())
                .memberId(MEMBER_ID)
                .build());
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspace.getId())
                .createdBy(MEMBER_ID)
                .build());
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(document.getId())
                .createdBy(MEMBER_ID)
                .build());
        documentId = document.getId();
    }

    @DisplayName("문서 초안의 본문을 수정한다.")
    @Test
    void updateBody() {
        // given
        DraftDocumentResult created = createDraft("기존 본문");

        // when
        DraftDocumentResult result = draftDocumentService.updateBody(
                new UpdateDraftBodyCommand(created.draftDocumentId(), "수정 본문", MEMBER_ID));

        // then
        assertThat(result.draftBody()).isEqualTo("수정 본문");
        assertThat(draftDocumentService
                        .read(created.draftDocumentId(), MEMBER_ID)
                        .draftBody())
                .isEqualTo("수정 본문");
    }

    @DisplayName("존재하지 않는 문서 초안의 본문을 수정하면 예외가 발생한다.")
    @Test
    void updateBody_draftDocumentNotFound() {
        // when & then
        assertThatThrownBy(() -> draftDocumentService.updateBody(new UpdateDraftBodyCommand(999L, "수정 본문", MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_NOT_FOUND));
    }

    @DisplayName("문서 초안을 삭제하면 이후 조회되지 않는다.")
    @Test
    void delete() {
        // given
        DraftDocumentResult created = createDraft("삭제할 본문");

        // when
        draftDocumentService.delete(created.draftDocumentId(), MEMBER_ID);

        // then
        assertThat(draftDocumentRepository.findByIdAndDeletedAtIsNull(created.draftDocumentId()))
                .isEmpty();
        assertThat(draftDocumentRepository.findById(created.draftDocumentId()))
                .get()
                .extracting(DraftDocument::isDeleted)
                .isEqualTo(true);
    }

    /**
     * 초안을 만드는 진입점은 비동기 대조 작업뿐이므로(D-45) 교정 이후 흐름만 보는 테스트는 초안을 직접 만든다. 생성 자체는
     * DraftDocumentCheckExecutionServiceTest가 검증한다.
     */
    private DraftDocumentResult createDraft(String draftBody) {
        DraftDocument draftDocument = draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(documentId)
                .draftBody(draftBody)
                .requestedBy(MEMBER_ID)
                .build());
        return draftDocumentService.read(draftDocument.getId(), MEMBER_ID);
    }
}
