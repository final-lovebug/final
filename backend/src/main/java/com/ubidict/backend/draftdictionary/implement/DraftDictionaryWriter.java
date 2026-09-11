package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDictionaryWriter {

    private final DraftDictionaryRepository draftDictionaryRepository;
    private final CandidateTermRepository candidateTermRepository;
    private final DictionaryTermQueryPort dictionaryTermQueryPort;

    public DraftDictionary create(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long memberId) {
        DraftDictionary draftDictionary =
                DraftDictionary.create(workspaceId, dictionaryId, sourceDocumentIds, memberId);

        DraftDictionary saved = draftDictionaryRepository.save(draftDictionary);
        dictionaryTermQueryPort
                .readActiveTerms(workspaceId)
                .forEach(term -> candidateTermRepository.save(CandidateTerm.createExisting(
                        saved.getId(), term.termId(), term.preferredForm(), term.englishName(), memberId)));
        return saved;
    }

    public void updateSourceDocuments(DraftDictionary draftDictionary, List<Long> sourceDocumentIds) {
        draftDictionary.replaceSourceDocuments(sourceDocumentIds);
    }
}
