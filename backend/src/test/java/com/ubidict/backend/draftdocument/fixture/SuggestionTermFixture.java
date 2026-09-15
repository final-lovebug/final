package com.ubidict.backend.draftdocument.fixture;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.domain.SuggestionTermStatus;
import org.springframework.test.util.ReflectionTestUtils;

public class SuggestionTermFixture {

    public static SuggestionTermBuilder suggestionTerm() {
        return new SuggestionTermBuilder();
    }

    public static class SuggestionTermBuilder {

        private Long id;
        private Long draftDocumentId = 1L;
        private TextRange anchor = new TextRange(0, 2);
        private String originTerm = "회원";
        private String suggestionTerm = "사용자";
        private SuggestionTermStatus status = SuggestionTermStatus.PENDING;
        private Long handledBy = 1L;
        private Long createdBy = 1L;

        public SuggestionTermBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public SuggestionTermBuilder draftDocumentId(Long draftDocumentId) {
            this.draftDocumentId = draftDocumentId;
            return this;
        }

        public SuggestionTermBuilder anchor(TextRange anchor) {
            this.anchor = anchor;
            return this;
        }

        public SuggestionTermBuilder originTerm(String originTerm) {
            this.originTerm = originTerm;
            return this;
        }

        public SuggestionTermBuilder suggestionTerm(String suggestionTerm) {
            this.suggestionTerm = suggestionTerm;
            return this;
        }

        public SuggestionTermBuilder status(SuggestionTermStatus status) {
            this.status = status;
            return this;
        }

        public SuggestionTerm build() {
            SuggestionTerm term = SuggestionTerm.create(draftDocumentId, anchor, originTerm, suggestionTerm, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(term, "id", id);
            }
            if (status == SuggestionTermStatus.APPLIED_SUGGESTION) {
                term.accept(handledBy);
            } else if (status == SuggestionTermStatus.KEPT_ORIGIN) {
                term.reject(handledBy, "원문을 유지합니다.");
            }
            return term;
        }
    }
}
