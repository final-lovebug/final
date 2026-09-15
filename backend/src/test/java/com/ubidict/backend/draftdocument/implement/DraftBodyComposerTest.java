package com.ubidict.backend.draftdocument.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.fixture.SuggestionTermFixture;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DraftBodyComposerTest {

    private final DraftBodyComposer composer = new DraftBodyComposer();

    @DisplayName("여러 제안어를 반영해도 앞쪽 위치가 정확히 치환된다.")
    @Test
    void compose_appliesFromTailToHead() {
        // given
        String originalBody = "회원은 결제방법을 선택한다.";
        SuggestionTerm first = appliedTerm(new TextRange(0, 3), "회원은", "사용자는");
        SuggestionTerm second = appliedTerm(new TextRange(4, 8), "결제방법", "결제수단");

        // when
        String composedBody = composer.compose(originalBody, List.of(first, second));

        // then
        assertThat(composedBody).isEqualTo("사용자는 결제수단을 선택한다.");
    }

    @DisplayName("수용한 제안어의 위치가 겹치면 예외가 발생한다.")
    @Test
    void compose_overlappingAnchors() {
        // given
        String originalBody = "결제방법";
        SuggestionTerm first = appliedTerm(new TextRange(0, 2), "결제", "지불");
        SuggestionTerm second = appliedTerm(new TextRange(1, 4), "제방법", "수단");

        // when & then
        assertThatThrownBy(() -> composer.compose(originalBody, List.of(first, second)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_ANCHOR));
    }

    @DisplayName("수용한 제안어가 없으면 본문이 그대로다.")
    @Test
    void compose_noAcceptedSuggestion() {
        // given
        String originalBody = "회원은 결제할 수 있다.";
        SuggestionTerm pending = SuggestionTermFixture.suggestionTerm().build();

        // when
        String composedBody = composer.compose(originalBody, List.of(pending));

        // then
        assertThat(composedBody).isEqualTo(originalBody);
    }

    private SuggestionTerm appliedTerm(TextRange anchor, String originTerm, String suggestionTerm) {
        return SuggestionTermFixture.suggestionTerm()
                .anchor(anchor)
                .originTerm(originTerm)
                .suggestionTerm(suggestionTerm)
                .status(SuggestionTermStatus.APPLIED_SUGGESTION)
                .build();
    }
}
