package com.ubidict.backend.draftdocument.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocumentStatus;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.fixture.SuggestionTermFixture;
import com.ubidict.backend.draftdocument.infra.SuggestionTermRepository;
import com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.ExamineProgressResult;
import com.ubidict.backend.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDocumentExamineServiceTest extends IntegrationTestSupport {

    private static final Long MEMBER_ID = 7L;

    @Autowired
    private DraftDocumentService draftDocumentService;

    @Autowired
    private SuggestionTermRepository suggestionTermRepository;

    @DisplayName("교정을 완료하면 수용한 제안어가 본문에 반영된다.")
    @Test
    void completeExamine() {
        // given
        Long draftDocumentId = createDraftDocument("회원은 결제방법을 선택한다.");
        saveSuggestionTerm(
                draftDocumentId, new TextRange(0, 3), "회원은", "사용자는", SuggestionTermStatus.APPLIED_SUGGESTION);
        saveSuggestionTerm(
                draftDocumentId, new TextRange(4, 8), "결제방법", "결제수단", SuggestionTermStatus.APPLIED_SUGGESTION);

        // when
        DraftDocumentResult result =
                draftDocumentService.completeExamine(new CompleteExamineCommand(draftDocumentId, MEMBER_ID));

        // then
        assertThat(result.status()).isEqualTo(DraftDocumentStatus.EXAMINED);
        assertThat(result.draftBody()).isEqualTo("사용자는 결제수단을 선택한다.");
    }

    @DisplayName("처리하지 않은 제안어가 남아 있으면 교정을 완료할 수 없다.")
    @Test
    void completeExamine_pendingExists() {
        // given
        Long draftDocumentId = createDraftDocument("회원");
        saveSuggestionTerm(draftDocumentId, new TextRange(0, 2), "회원", "사용자", SuggestionTermStatus.PENDING);

        // when & then
        assertThatThrownBy(() ->
                        draftDocumentService.completeExamine(new CompleteExamineCommand(draftDocumentId, MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_SUGGESTION_TERM_UNHANDLED_EXISTS));
    }

    @DisplayName("이미 교정완료된 초안은 다시 완료할 수 없다.")
    @Test
    void completeExamine_alreadyExamined() {
        // given
        Long draftDocumentId = createDraftDocument("회원");
        draftDocumentService.completeExamine(new CompleteExamineCommand(draftDocumentId, MEMBER_ID));

        // when & then
        assertThatThrownBy(() ->
                        draftDocumentService.completeExamine(new CompleteExamineCommand(draftDocumentId, MEMBER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXAMINED));
    }

    @DisplayName("교정 진행률과 현재 수용 결과를 반영한 미리보기를 조회한다.")
    @Test
    void readExamineProgress() {
        // given
        Long draftDocumentId = createDraftDocument("회원은 결제방법을 선택한다.");
        saveSuggestionTerm(
                draftDocumentId, new TextRange(0, 3), "회원은", "사용자는", SuggestionTermStatus.APPLIED_SUGGESTION);
        saveSuggestionTerm(draftDocumentId, new TextRange(4, 8), "결제방법", "결제수단", SuggestionTermStatus.KEPT_ORIGIN);
        saveSuggestionTerm(draftDocumentId, new TextRange(9, 10), "을", "를", SuggestionTermStatus.PENDING);

        // when
        ExamineProgressResult result = draftDocumentService.readExamineProgress(draftDocumentId, MEMBER_ID);

        // then
        assertThat(result.total()).isEqualTo(3);
        assertThat(result.pending()).isEqualTo(1);
        assertThat(result.keptOrigin()).isEqualTo(1);
        assertThat(result.appliedSuggestion()).isEqualTo(1);
        assertThat(result.previewBody()).isEqualTo("사용자는 결제방법을 선택한다.");
    }

    private Long createDraftDocument(String draftBody) {
        return draftDocumentService
                .create(new CreateDraftDocumentCommand(1L, 1, draftBody, MEMBER_ID))
                .draftDocumentId();
    }

    private void saveSuggestionTerm(
            Long draftDocumentId,
            TextRange anchor,
            String originTerm,
            String suggestionTerm,
            SuggestionTermStatus status) {
        SuggestionTerm term = SuggestionTermFixture.suggestionTerm()
                .draftDocumentId(draftDocumentId)
                .anchor(anchor)
                .originTerm(originTerm)
                .suggestionTerm(suggestionTerm)
                .status(status)
                .build();
        suggestionTermRepository.save(term);
    }
}
