package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.implement.CheckJobCreationPolicyValidator;
import com.ubidict.backend.draftdocument.implement.CheckJobEventPublisher;
import com.ubidict.backend.draftdocument.implement.CheckJobReader;
import com.ubidict.backend.draftdocument.implement.CheckJobWriter;
import com.ubidict.backend.draftdocument.implement.DraftDocumentAccessValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentCreationPolicyValidator;
import com.ubidict.backend.draftdocument.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.service.model.CheckJobResult;
import com.ubidict.backend.draftdocument.service.model.CreateCheckJobCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentCheckService {

    private final CheckJobReader checkJobReader;
    private final CheckJobWriter checkJobWriter;
    private final CheckJobCreationPolicyValidator checkJobCreationPolicyValidator;
    private final DraftDocumentAccessValidator accessValidator;
    private final DraftDocumentCreationPolicyValidator draftCreationPolicyValidator;
    private final DictionaryTermQueryPort dictionaryTermQueryPort;
    private final CheckJobEventPublisher checkJobEventPublisher;

    @Transactional
    public CheckJobResult request(CreateCheckJobCommand command) {
        DocumentSnapshot document = accessValidator.validateCreation(command.documentId(), command.memberId());
        draftCreationPolicyValidator.validate(command.documentId());
        checkJobCreationPolicyValidator.validate(command.documentId());
        if (dictionaryTermQueryPort.activeVersionNo(document.workspaceId()).isEmpty()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_DICTIONARY_NOT_FOUND);
        }

        CheckJob checkJob = checkJobWriter.append(command.documentId(), command.memberId());
        checkJobEventPublisher.publishRequested(checkJob);
        log.info(
                "[DraftDocumentCheckService.request] Draft document check requested. checkJobId={}, documentId={}, memberId={}",
                checkJob.getId(),
                checkJob.getDocumentId(),
                command.memberId());
        return CheckJobResult.from(checkJob);
    }

    @Transactional(readOnly = true)
    public CheckJobResult read(Long checkJobId, Long memberId) {
        CheckJob checkJob = checkJobReader.read(checkJobId);
        accessValidator.validateCreation(checkJob.getDocumentId(), memberId);
        return CheckJobResult.from(checkJob);
    }
}
