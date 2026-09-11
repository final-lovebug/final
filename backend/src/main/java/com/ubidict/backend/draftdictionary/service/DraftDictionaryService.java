package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryReader;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryRemover;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryWriter;
import com.ubidict.backend.draftdictionary.service.model.CreateDraftDictionaryCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.draftdictionary.service.model.UpdateSourceDocumentsCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDictionaryService {

    private final DraftDictionaryReader draftDictionaryReader;
    private final DraftDictionaryWriter draftDictionaryWriter;
    private final DraftDictionaryRemover draftDictionaryRemover;

    @Transactional
    public DraftDictionaryResult create(CreateDraftDictionaryCommand command) {
        DraftDictionary draftDictionary = draftDictionaryWriter.create(
                command.workspaceId(), command.dictionaryId(), command.sourceDocumentIds(), command.memberId());

        log.info(
                "[DraftDictionaryService.create] Draft dictionary created. draftDictionaryId={}, workspaceId={}, memberId={}",
                draftDictionary.getId(),
                draftDictionary.getWorkspaceId(),
                command.memberId());

        return DraftDictionaryResult.from(draftDictionary);
    }

    @Transactional(readOnly = true)
    public DraftDictionaryResult read(Long draftDictionaryId, Long memberId) {
        return DraftDictionaryResult.from(draftDictionaryReader.read(draftDictionaryId));
    }

    @Transactional
    public DraftDictionaryResult updateSourceDocuments(UpdateSourceDocumentsCommand command) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(command.draftDictionaryId());
        draftDictionaryWriter.updateSourceDocuments(draftDictionary, command.sourceDocumentIds());

        log.info(
                "[DraftDictionaryService.updateSourceDocuments] Source documents updated. draftDictionaryId={}, memberId={}",
                draftDictionary.getId(),
                command.memberId());

        return DraftDictionaryResult.from(draftDictionary);
    }

    @Transactional
    public void delete(Long draftDictionaryId, Long memberId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        draftDictionaryRemover.remove(draftDictionary);

        log.info(
                "[DraftDictionaryService.delete] Draft dictionary deleted. draftDictionaryId={}, memberId={}",
                draftDictionaryId,
                memberId);
    }
}
