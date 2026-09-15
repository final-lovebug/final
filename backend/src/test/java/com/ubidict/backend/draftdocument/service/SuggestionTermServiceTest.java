package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.*;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.*;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.draftdocument.service.model.*;
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

class SuggestionTermServiceTest extends IntegrationTestSupport {
    private Long documentId;

    @Autowired
    SuggestionTermService service;

    @Autowired
    DraftDocumentRepository draftDocumentRepository;

    @Autowired
    WorkspaceRepository workspaceRepository;

    @Autowired
    ParticipantRepository participantRepository;

    @Autowired
    DocumentRepository documentRepository;

    @Autowired
    DocumentVersionRepository documentVersionRepository;

    @BeforeEach
    void setUpDocument() {
        Workspace workspace =
                workspaceRepository.save(WorkspaceFixture.workspace().build());
        participantRepository.save(
                ParticipantFixture.participant().workspaceId(workspace.getId()).build());
        Document document = documentRepository.save(
                DocumentFixture.document().workspaceId(workspace.getId()).build());
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(document.getId())
                .build());
        documentId = document.getId();
    }

    @DisplayName("제안어를 등록·수정·삭제하면 목록 조회 결과가 따라 바뀐다.")
    @Test
    void add_edit_delete_search() {
        Long draftDocumentId = createDraft("hello");
        var a = service.add(new AddSuggestionTermCommand(draftDocumentId, new TextRange(0, 2), "he", "hi", 1L));
        assertThat(service.search(new SuggestionTermSearchQuery(draftDocumentId, null, 0, 20, "id,asc", 1L))
                        .content())
                .hasSize(1);
        var e = service.edit(new EditSuggestionTermCommand(a.id(), new TextRange(1, 3), "ell", "ALL", 1L));
        assertThat(e.originTerm()).isEqualTo("ell");
        service.delete(a.id(), 1L);
        assertThat(service.search(new SuggestionTermSearchQuery(draftDocumentId, null, 0, 20, "id,asc", 1L))
                        .content())
                .isEmpty();
    }

    @DisplayName("본문 범위를 벗어난 위치에는 제안어를 등록할 수 없다.")
    @Test
    void add_anchorOutOfBody() {
        Long draftDocumentId = createDraft("hi");
        assertThatThrownBy(() ->
                        service.add(new AddSuggestionTermCommand(draftDocumentId, new TextRange(0, 3), "h", "x", 1L)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY));
    }

    @DisplayName("제안어 위치를 본문 범위 밖으로 수정할 수 없다.")
    @Test
    void edit_anchorOutOfBody() {
        Long draftDocumentId = createDraft("hi");
        var suggestion =
                service.add(new AddSuggestionTermCommand(draftDocumentId, new TextRange(0, 2), "hi", "hello", 1L));

        assertThatThrownBy(() -> service.edit(
                        new EditSuggestionTermCommand(suggestion.id(), new TextRange(0, 3), null, null, 1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY));
    }

    /** 초안 생성 진입점은 비동기 대조 작업뿐이므로(D-45) 제안어만 보는 테스트는 초안을 직접 만든다. */
    private Long createDraft(String draftBody) {
        return draftDocumentRepository
                .save(DraftDocumentFixture.draftDocument()
                        .documentId(documentId)
                        .draftBody(draftBody)
                        .build())
                .getId();
    }
}
