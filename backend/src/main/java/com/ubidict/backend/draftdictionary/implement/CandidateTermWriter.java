package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.EditCandidateTermCommand;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandidateTermWriter {
    private final CandidateTermRepository repository;

    public CandidateTerm add(AddCandidateTermCommand c) {
        if (repository.existsByDraftDictionaryIdAndFormAndDeletedAtIsNull(c.draftDictionaryId(), c.form())) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_DUPLICATE_CANDIDATE_FORM);
        }
        return repository.save(CandidateTerm.create(
                c.draftDictionaryId(),
                c.form(),
                c.proposedDefinition(),
                c.proposedEnglishName(),
                c.occurredDocumentIds(),
                c.occurrenceCount(),
                c.contextSnippets(),
                c.memberId()));
    }

    public CandidateTerm edit(CandidateTerm e, EditCandidateTermCommand c) {
        e.edit(c.form(), c.proposedDefinition(), c.proposedEnglishName());
        return e;
    }
}
