package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.draftdictionary.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdictionary.infra.port.TermSnapshot;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 빈 이름을 명시한다 — draftdocument에도 같은 이름의 어댑터가 있고, 두 소비 도메인이 각자
// 정의한 DictionaryTermQueryPort의 이름이 같아 Spring 기본 빈 이름이 충돌한다.
@Component("draftDictionaryTermQueryAdapter")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "real")
public class DictionaryTermQueryAdapter implements DictionaryTermQueryPort {
    private final DictionaryRepository dictionaryRepository;
    private final TermRepository termRepository;

    @Override
    public List<TermSnapshot> readActiveTerms(Long workspaceId) {
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

    private TermSnapshot toSnapshot(Term term) {
        return new TermSnapshot(term.getId(), term.getPreferredForm(), term.getEnglishName(), term.getDefinition());
    }
}
