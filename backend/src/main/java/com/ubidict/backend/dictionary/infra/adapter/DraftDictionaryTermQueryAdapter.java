package com.ubidict.backend.dictionary.infra.adapter;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class DraftDictionaryTermQueryAdapter
        implements com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort {
    private final DictionaryRepository dictionaryRepository;
    private final TermRepository termRepository;

    public DraftDictionaryTermQueryAdapter(DictionaryRepository dictionaryRepository, TermRepository termRepository) {
        this.dictionaryRepository = dictionaryRepository;
        this.termRepository = termRepository;
    }

    @Override
    public List<com.ubidict.backend.draftdictionary.infra.port.TermSnapshot> readActiveTerms(Long workspaceId) {
        return activeDictionary(workspaceId)
                .map(Dictionary::getId)
                .map(termRepository::findAllByDictionaryIdOrderByPreferredFormAsc)
                .orElseGet(List::of)
                .stream()
                .map(this::toSnapshot)
                .toList();
    }

    @Override
    public Optional<Integer> activeVersionNo(Long workspaceId) {
        return activeDictionary(workspaceId).map(Dictionary::versionNo);
    }

    private Optional<Dictionary> activeDictionary(Long workspaceId) {
        return dictionaryRepository.findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE);
    }

    private com.ubidict.backend.draftdictionary.infra.port.TermSnapshot toSnapshot(Term term) {
        return new com.ubidict.backend.draftdictionary.infra.port.TermSnapshot(
                term.getId(), term.getPreferredForm(), term.getEnglishName(), term.getDefinition());
    }
}
