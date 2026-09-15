package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.port.ExtractedTerm;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class ExtractionResultValidator {

    public void validate(List<Long> sourceDocumentIds, List<ExtractedTerm> extractedTerms) {
        if (extractedTerms == null) invalid();
        Set<String> forms = new HashSet<>();
        Set<Long> sourceIds = Set.copyOf(sourceDocumentIds);
        for (ExtractedTerm term : extractedTerms) {
            if (term == null
                    || term.form() == null
                    || term.form().isBlank()
                    || term.occurrenceCount() < 1
                    || term.occurredDocumentIds() == null
                    || !sourceIds.containsAll(term.occurredDocumentIds())
                    || term.contextSnippets() == null
                    || !forms.add(term.form())) {
                invalid();
            }
        }
    }

    private static void invalid() {
        throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_RESULT);
    }
}
