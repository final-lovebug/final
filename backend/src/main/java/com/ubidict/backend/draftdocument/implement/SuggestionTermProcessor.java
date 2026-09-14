package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.infra.port.TermSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SuggestionTermProcessor {

    private final DraftBodyComposer draftBodyComposer;
    private final DocumentQueryPort documentQueryPort;
    private final DictionaryTermQueryPort dictionaryTermQueryPort;

    public void accept(DraftDocument draftDocument, SuggestionTerm suggestionTerm, Long handlerId) {
        draftDocument.validateExamining();
        validateSuggestionSource(draftDocument, suggestionTerm);
        suggestionTerm.accept(handlerId);
    }

    public void reject(
            DraftDocument draftDocument, SuggestionTerm suggestionTerm, Long handlerId, String rejectReason) {
        draftDocument.validateExamining();
        suggestionTerm.reject(handlerId, rejectReason);
    }

    public String preview(DraftDocument draftDocument, List<SuggestionTerm> suggestionTerms) {
        if (!draftDocument.isExamining()) {
            return draftDocument.getDraftBody();
        }
        return draftBodyComposer.compose(draftDocument.getDraftBody(), suggestionTerms);
    }

    public void complete(DraftDocument draftDocument, List<SuggestionTerm> suggestionTerms) {
        draftDocument.validateExamining();
        if (suggestionTerms.stream().anyMatch(SuggestionTerm::isPending)) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_SUGGESTION_TERM_UNHANDLED_EXISTS);
        }

        draftDocument.markExamined(preview(draftDocument, suggestionTerms));
    }

    private void validateSuggestionSource(DraftDocument draftDocument, SuggestionTerm suggestionTerm) {
        Long workspaceId = documentQueryPort
                .read(draftDocument.getDocumentId())
                .map(DocumentSnapshot::workspaceId)
                .orElseThrow(
                        () -> new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM));
        boolean isActiveTerm = dictionaryTermQueryPort.readActiveTerms(workspaceId).stream()
                .map(TermSnapshot::preferredForm)
                .anyMatch(suggestionTerm.getSuggestionTerm()::equals);
        if (!isActiveTerm) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM);
        }
    }
}
