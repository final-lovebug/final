package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.fixture.DictionaryFixture;
import com.ubidict.backend.dictionary.fixture.TermFixture;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.fixture.DocumentFixture;
import com.ubidict.backend.document.fixture.DocumentVersionFixture;
import com.ubidict.backend.document.infra.DocumentRepository;
import com.ubidict.backend.document.infra.DocumentVersionRepository;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.service.model.AcceptSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.AddSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.RejectSuggestionTermCommand;
import com.ubidict.backend.draftdocument.service.model.SuggestionTermResult;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SuggestionTermDecisionServiceTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 7L;

    @Autowired
    private SuggestionTermService suggestionTermService;

    @Autowired
    private DraftDocumentService draftDocumentService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private DocumentVersionRepository documentVersionRepository;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private TermRepository termRepository;

    @DisplayName("활성 사전집의 제안어를 수용하면 처리 결과를 기록한다.")
    @Test
    void accept() {
        // given
        SuggestionTermResult suggestionTerm = createSuggestionTerm("사용자");

        // when
        SuggestionTermResult result =
                suggestionTermService.accept(new AcceptSuggestionTermCommand(suggestionTerm.id(), MEMBER_ID));

        // then
        assertThat(result.status()).isEqualTo(SuggestionTermStatus.APPLIED_SUGGESTION);
        assertThat(result.handledBy()).isEqualTo(MEMBER_ID);
        assertThat(result.rejectReason()).isNull();
    }

    @DisplayName("활성 사전집에 없는 제안 용어는 수용할 수 없다.")
    @Test
    void accept_suggestionTermIsNotActive() {
        // given
        SuggestionTermResult suggestionTerm = createSuggestionTerm("미등록어");

        // when & then
        assertThatThrownBy(() ->
                        suggestionTermService.accept(new AcceptSuggestionTermCommand(suggestionTerm.id(), MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM));
    }

    @DisplayName("제안어를 거절하면 본문은 유지되고 사유가 기록된다.")
    @Test
    void reject() {
        // given
        SuggestionTermResult suggestionTerm = createSuggestionTerm("사용자");

        // when
        SuggestionTermResult result = suggestionTermService.reject(
                new RejectSuggestionTermCommand(suggestionTerm.id(), "도메인 고유 표현", MEMBER_ID));

        // then
        assertThat(result.status()).isEqualTo(SuggestionTermStatus.KEPT_ORIGIN);
        assertThat(result.handledBy()).isEqualTo(MEMBER_ID);
        assertThat(result.rejectReason()).isEqualTo("도메인 고유 표현");
        assertThat(draftDocumentService
                        .read(result.draftDocumentId(), MEMBER_ID)
                        .draftBody())
                .isEqualTo("회원");
    }

    @DisplayName("교정완료된 초안의 제안어는 수용할 수 없다.")
    @Test
    void accept_afterExamined() {
        // given
        SuggestionTermResult suggestionTerm = createSuggestionTerm("사용자");
        suggestionTermService.reject(new RejectSuggestionTermCommand(suggestionTerm.id(), "원문 유지", MEMBER_ID));
        draftDocumentService.completeExamine(new CompleteExamineCommand(suggestionTerm.draftDocumentId(), MEMBER_ID));

        // when & then
        assertThatThrownBy(() ->
                        suggestionTermService.accept(new AcceptSuggestionTermCommand(suggestionTerm.id(), MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXAMINED));
    }

    @DisplayName("교정완료된 초안의 제안어는 거절할 수 없다.")
    @Test
    void reject_afterExamined() {
        // given
        SuggestionTermResult suggestionTerm = createSuggestionTerm("사용자");
        suggestionTermService.reject(new RejectSuggestionTermCommand(suggestionTerm.id(), "원문 유지", MEMBER_ID));
        draftDocumentService.completeExamine(new CompleteExamineCommand(suggestionTerm.draftDocumentId(), MEMBER_ID));

        // when & then
        assertThatThrownBy(() -> suggestionTermService.reject(
                        new RejectSuggestionTermCommand(suggestionTerm.id(), "다시 거절", MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXAMINED));
    }

    private SuggestionTermResult createSuggestionTerm(String suggestionTerm) {
        Workspace workspace = workspaceRepository.save(
                WorkspaceFixture.workspace().createdBy(MEMBER_ID).build());
        Document document = documentRepository.save(DocumentFixture.document()
                .workspaceId(workspace.getId())
                .createdBy(MEMBER_ID)
                .build());
        documentVersionRepository.save(DocumentVersionFixture.documentVersion()
                .documentId(document.getId())
                .body("회원")
                .createdBy(MEMBER_ID)
                .build());
        Dictionary dictionary = dictionaryRepository.save(DictionaryFixture.dictionary()
                .workspaceId(workspace.getId())
                .createdBy(MEMBER_ID)
                .build());
        termRepository.save(TermFixture.term()
                .dictionaryId(dictionary.getId())
                .preferredForm("사용자")
                .createdBy(MEMBER_ID)
                .build());

        Long draftDocumentId = draftDocumentService
                .create(new CreateDraftDocumentCommand(document.getId(), 1, "회원", MEMBER_ID))
                .draftDocumentId();
        return suggestionTermService.add(
                new AddSuggestionTermCommand(draftDocumentId, new TextRange(0, 2), "회원", suggestionTerm, MEMBER_ID));
    }
}
