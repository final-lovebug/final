package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DraftDictionaryWriter {

    private final DraftDictionaryRepository draftDictionaryRepository;

    public DraftDictionary create(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long memberId) {
        DraftDictionary draftDictionary =
                DraftDictionary.create(workspaceId, dictionaryId, sourceDocumentIds, memberId);

        return draftDictionaryRepository.save(draftDictionary);
    }

    public void updateSourceDocuments(DraftDictionary draftDictionary, List<Long> sourceDocumentIds) {
        draftDictionary.replaceSourceDocuments(sourceDocumentIds);
    }
}
