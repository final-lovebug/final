package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.infra.ai.LlmJobOutboxService;
import com.ubidict.backend.common.infra.ai.LlmJobRequest;
import com.ubidict.backend.common.infra.ai.LlmProperties;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobReader;
import com.ubidict.backend.draftdictionary.implement.ExtractionJobWriter;
import com.ubidict.backend.draftdictionary.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdictionary.service.model.CreateExtractionJobCommand;
import com.ubidict.backend.draftdictionary.service.model.ExtractionJobResult;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDictionaryExtractionService {

    private final ExtractionJobReader extractionJobReader;
    private final ExtractionJobWriter extractionJobWriter;
    private final ExtractionJobCreationPolicyValidator extractionJobCreationPolicyValidator;
    private final DraftDictionaryCreationPolicyValidator draftDictionaryCreationPolicyValidator;
    private final DocumentQueryPort documentQueryPort;
    private final LlmJobOutboxService outboxService;
    private final LlmProperties llmProperties;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional
    public ExtractionJobResult request(CreateExtractionJobCommand command) {
        workspaceAccessValidator.validateAtLeast(command.workspaceId(), command.memberId(), Permission.ADMIN);
        draftDictionaryCreationPolicyValidator.validate(command.workspaceId());
        extractionJobCreationPolicyValidator.validate(command.workspaceId());
        List<Long> extractableDocumentIds = command.sourceDocumentIds().stream()
                .filter(documentQueryPort::isExtractable)
                .toList();
        if (extractableDocumentIds.isEmpty()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NO_EXTRACTABLE_DOCUMENT);
        }

        ExtractionJob extractionJob = extractionJobWriter.append(
                command.workspaceId(), command.dictionaryId(), extractableDocumentIds, command.memberId());
        String requestId = LlmJobRequest.newRequestId();
        extractionJob.assignRequestId(requestId);
        outboxService.enqueue(LlmJobRequest.termExtraction(
                requestId,
                extractionJob.getId(),
                extractionJob.getWorkspaceId(),
                extractionJob.getDictionaryId(),
                extractionJob.getSourceDocumentIds(),
                llmProperties.mode()));
        log.info(
                "[DraftDictionaryExtractionService.request] Draft dictionary extraction requested. extractionJobId={}, workspaceId={}, memberId={}",
                extractionJob.getId(),
                extractionJob.getWorkspaceId(),
                command.memberId());
        return ExtractionJobResult.from(extractionJob);
    }

    @Transactional(readOnly = true)
    public ExtractionJobResult read(Long extractionJobId, Long memberId) {
        ExtractionJob extractionJob = extractionJobReader.read(extractionJobId);
        workspaceAccessValidator.validateParticipant(extractionJob.getWorkspaceId(), memberId);
        return ExtractionJobResult.from(extractionJob);
    }
}
