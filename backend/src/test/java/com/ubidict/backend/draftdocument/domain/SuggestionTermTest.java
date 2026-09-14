package com.ubidict.backend.draftdocument.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SuggestionTermTest {
    @DisplayName("제안어를 생성하면 처리 전 상태로 시작한다.")
    @Test
    void create() {
        assertThat(SuggestionTerm.create(1L, new TextRange(0, 1), "a", "b", 2L).getStatus())
                .isEqualTo(SuggestionTermStatus.PENDING);
    }

    @DisplayName("제안어를 수용하면 처리자와 채택 상태를 기록한다.")
    @Test
    void accept() {
        // given
        SuggestionTerm suggestionTerm = SuggestionTerm.create(1L, new TextRange(0, 1), "a", "b", 2L);

        // when
        suggestionTerm.accept(3L);

        // then
        assertThat(suggestionTerm.getStatus()).isEqualTo(SuggestionTermStatus.APPLIED_SUGGESTION);
        assertThat(suggestionTerm.getHandledBy()).isEqualTo(3L);
        assertThat(suggestionTerm.getRejectReason()).isNull();
    }

    @DisplayName("거절 사유가 비어 있으면 예외가 발생한다.")
    @Test
    void reject_reasonIsBlank() {
        // given
        SuggestionTerm suggestionTerm = SuggestionTerm.create(1L, new TextRange(0, 1), "a", "b", 2L);

        // when & then
        assertThatThrownBy(() -> suggestionTerm.reject(3L, " "))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_REJECT_REASON_REQUIRED));
    }

    @DisplayName("교정 중이면 거절한 제안어를 다시 수용할 수 있다.")
    @Test
    void accept_afterReject() {
        // given
        SuggestionTerm suggestionTerm = SuggestionTerm.create(1L, new TextRange(0, 1), "a", "b", 2L);
        suggestionTerm.reject(3L, "원문 유지");

        // when
        suggestionTerm.accept(4L);

        // then
        assertThat(suggestionTerm.getStatus()).isEqualTo(SuggestionTermStatus.APPLIED_SUGGESTION);
        assertThat(suggestionTerm.getHandledBy()).isEqualTo(4L);
        assertThat(suggestionTerm.getRejectReason()).isNull();
    }
}
