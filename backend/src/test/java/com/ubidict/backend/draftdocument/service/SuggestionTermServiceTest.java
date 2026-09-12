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
import com.ubidict.backend.draftdocument.service.model.*;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.ParticipantFixture;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SuggestionTermServiceTest extends IntegrationTestSupport {
    private Long documentId;

    @Autowired
    SuggestionTermService service;

    @Autowired
    DraftDocumentService drafts;

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

    @Test
    void add_edit_delete_search() {
        var d = drafts.create(new CreateDraftDocumentCommand(documentId, 1, "hello", 1L));
        var a = service.add(new AddSuggestionTermCommand(d.draftDocumentId(), new TextRange(0, 2), "he", "hi", 1L));
        assertThat(service.search(new SuggestionTermSearchQuery(d.draftDocumentId(), null, 0, 20, "id,asc", 1L))
                        .content())
                .hasSize(1);
        var e = service.edit(new EditSuggestionTermCommand(a.id(), new TextRange(1, 3), "ell", "ALL", 1L));
        assertThat(e.originTerm()).isEqualTo("ell");
        service.delete(a.id(), 1L);
        assertThat(service.search(new SuggestionTermSearchQuery(d.draftDocumentId(), null, 0, 20, "id,asc", 1L))
                        .content())
                .isEmpty();
    }

    @Test
    void add_anchorOutOfBody() {
        var d = drafts.create(new CreateDraftDocumentCommand(documentId, 1, "hi", 1L));
        assertThatThrownBy(() -> service.add(
                        new AddSuggestionTermCommand(d.draftDocumentId(), new TextRange(0, 3), "h", "x", 1L)))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY));
    }

    @Test
    void edit_anchorOutOfBody() {
        var draft = drafts.create(new CreateDraftDocumentCommand(documentId, 1, "hi", 1L));
        var suggestion = service.add(
                new AddSuggestionTermCommand(draft.draftDocumentId(), new TextRange(0, 2), "hi", "hello", 1L));

        assertThatThrownBy(() -> service.edit(
                        new EditSuggestionTermCommand(suggestion.id(), new TextRange(0, 3), null, null, 1L)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ANCHOR_OUT_OF_BODY));
    }
}
