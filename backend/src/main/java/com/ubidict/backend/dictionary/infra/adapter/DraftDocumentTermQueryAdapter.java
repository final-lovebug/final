package com.ubidict.backend.dictionary.infra.adapter;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DraftDocumentTermQueryAdapter
        implements com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort {
    private final DraftDictionaryTermQueryAdapter delegate;

    public DraftDocumentTermQueryAdapter(DraftDictionaryTermQueryAdapter delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<com.ubidict.backend.draftdocument.infra.port.TermSnapshot> readActiveTerms(Long workspaceId) {
        return delegate.readActiveTerms(workspaceId).stream()
                .map(term -> new com.ubidict.backend.draftdocument.infra.port.TermSnapshot(
                        term.termId(), term.preferredForm(), term.englishName(), term.definition()))
                .toList();
    }
}
