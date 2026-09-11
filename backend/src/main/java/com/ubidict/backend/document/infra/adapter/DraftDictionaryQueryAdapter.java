package com.ubidict.backend.document.infra.adapter;

import com.ubidict.backend.document.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "real")
public class DraftDictionaryQueryAdapter implements DraftDictionaryQueryPort {
    private final DraftDictionaryRepository draftDictionaryRepository;

    @Override
    public boolean isSourceOfOngoingDraft(Long documentId) {
        return draftDictionaryRepository.findAll().stream()
                .filter(draft -> draft.getDeletedAt() == null)
                .filter(draft -> draft.getStatus() != DraftDictionaryStatus.REVISED)
                .anyMatch(draft -> draft.getSourceDocumentIds().contains(documentId));
    }
}
