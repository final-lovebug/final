package com.ubidict.backend.revisionlog.infra.adapter;

import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.infra.SuggestionTermRepository;
import com.ubidict.backend.revisionlog.infra.port.AppliedSuggestion;
import com.ubidict.backend.revisionlog.infra.port.DraftDocumentQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** KEEP_ORIGINAL 제안은 제외하고 실제로 적용된 치환만 제공한다. */
@Component("draftDocumentQueryAdapterForRevisionLog")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-document.mode", havingValue = "real")
public class DraftDocumentQueryAdapter implements DraftDocumentQueryPort {

    private final SuggestionTermRepository suggestionTermRepository;

    @Override
    public List<AppliedSuggestion> readAppliedSuggestions(Long draftDocumentId) {
        return suggestionTermRepository.findAllByDraftDocumentIdAndDeletedAtIsNull(draftDocumentId).stream()
                .filter(SuggestionTerm::isApplied)
                .map(suggestion -> new AppliedSuggestion(suggestion.getOriginTerm(), suggestion.getSuggestionTerm()))
                .toList();
    }
}
