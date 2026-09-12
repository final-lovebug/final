package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.DictionaryStatus;
import com.ubidict.backend.dictionary.domain.Term;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.TermSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 빈 이름을 명시한다 — draftdictionary에도 같은 이름의 어댑터가 있다.
@Component("draftDocumentTermQueryAdapter")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.dictionary.mode", havingValue = "real")
public class DictionaryTermQueryAdapter implements DictionaryTermQueryPort {
    private final DictionaryRepository dictionaryRepository;
    private final TermRepository termRepository;

    @Override
    public List<TermSnapshot> readActiveTerms(Long workspaceId) {
        return dictionaryRepository
                .findByWorkspaceIdAndStatus(workspaceId, DictionaryStatus.ACTIVE)
                .map(Dictionary::getId)
                .map(termRepository::findAllByDictionaryIdOrderByPreferredFormAsc)
                .orElseGet(List::of)
                .stream()
                .map(this::toSnapshot)
                .toList();
    }

    private TermSnapshot toSnapshot(Term term) {
        return new TermSnapshot(term.getId(), term.getPreferredForm(), term.getEnglishName(), term.getDefinition());
    }
}
