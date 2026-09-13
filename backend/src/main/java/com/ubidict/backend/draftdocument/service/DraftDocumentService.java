package com.ubidict.backend.draftdocument.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.domain.SuggestionTerm;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import com.ubidict.backend.draftdocument.implement.DraftDocumentAccessValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentCreationPolicyValidator;
import com.ubidict.backend.draftdocument.implement.DraftDocumentEventPublisher;
import com.ubidict.backend.draftdocument.implement.DraftDocumentReader;
import com.ubidict.backend.draftdocument.implement.DraftDocumentRemover;
import com.ubidict.backend.draftdocument.implement.DraftDocumentWriter;
import com.ubidict.backend.draftdocument.implement.SuggestionTermProcessor;
import com.ubidict.backend.draftdocument.implement.SuggestionTermReader;
import com.ubidict.backend.draftdocument.infra.port.DocumentQueryPort;
import com.ubidict.backend.draftdocument.infra.port.DocumentSnapshot;
import com.ubidict.backend.draftdocument.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdocument.service.model.CreateDraftDocumentCommand;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentResult;
import com.ubidict.backend.draftdocument.service.model.DraftDocumentSearchQuery;
import com.ubidict.backend.draftdocument.service.model.ExamineProgressResult;
import com.ubidict.backend.draftdocument.service.model.UpdateDraftBodyCommand;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDocumentService {

    private final DraftDocumentReader draftDocumentReader;
    private final DraftDocumentWriter draftDocumentWriter;
    private final DraftDocumentRemover draftDocumentRemover;
    private final SuggestionTermReader suggestionTermReader;
    private final SuggestionTermProcessor suggestionTermProcessor;
    private final DraftDocumentAccessValidator accessValidator;
    private final DraftDocumentCreationPolicyValidator creationPolicyValidator;
    private final DraftDocumentEventPublisher draftDocumentEventPublisher;
    private final DocumentQueryPort documentQueryPort;

    /**
     * 리뷰 요청을 받을 상태인지 판정한다. 리뷰 요청 생성은 ReviewRequest 도메인이 하고(D-44) 이 메서드는 그쪽 어댑터가 호출한다.
     *
     * <p>상태를 바꾸지 않는다 — 전이는 ReviewRequestCreatedEvent를 받는 리스너가 한다.
     */
    @Transactional(readOnly = true)
    public void validateReviewReadiness(Long draftDocumentId) {
        draftDocumentReader.read(draftDocumentId).validateExaminedForReview();
    }

    @Transactional
    public DraftDocumentResult create(CreateDraftDocumentCommand command) {
        DocumentSnapshot document = accessValidator.validateCreation(command.documentId(), command.memberId());
        if (document.currentVersionNo() != command.baseVersionNo()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_BASE_VERSION);
        }
        creationPolicyValidator.validate(command.documentId(), document.workspaceId());
        DraftDocument draftDocument = draftDocumentWriter.append(
                command.documentId(),
                command.baseVersionNo(),
                command.draftBody(),
                command.memberId(),
                command.memberId());
        draftDocumentEventPublisher.publishCreated(draftDocument);

        log.info(
                "[DraftDocumentService.create] Draft document created. draftDocumentId={}, documentId={}, memberId={}",
                draftDocument.getId(),
                draftDocument.getDocumentId(),
                command.memberId());

        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional(readOnly = true)
    public DraftDocumentResult read(Long draftDocumentId, Long memberId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        accessValidator.validateAccess(draftDocument, memberId);
        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional
    public DraftDocumentResult updateBody(UpdateDraftBodyCommand command) {
        DraftDocument draftDocument = draftDocumentReader.read(command.draftDocumentId());
        accessValidator.validateAccess(draftDocument, command.memberId());
        draftDocumentWriter.updateBody(draftDocument, command.draftBody());

        log.info(
                "[DraftDocumentService.updateBody] Draft document body updated. draftDocumentId={}, memberId={}",
                draftDocument.getId(),
                command.memberId());

        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional
    public void delete(Long draftDocumentId, Long memberId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        accessValidator.validateAccess(draftDocument, memberId);
        draftDocumentRemover.remove(draftDocument);

        log.info(
                "[DraftDocumentService.delete] Draft document deleted. draftDocumentId={}, memberId={}",
                draftDocument.getId(),
                memberId);
    }

    @Transactional(readOnly = true)
    public PageResult<DraftDocumentResult> search(DraftDocumentSearchQuery q) {
        String[] p = q.sort().split(",");
        Set<Long> accessibleDocumentIds = documentQueryPort.readAccessibleDocumentIds(q.memberId());
        if (q.documentId() != null) {
            accessibleDocumentIds = accessibleDocumentIds.contains(q.documentId()) ? Set.of(q.documentId()) : Set.of();
        }
        Page<DraftDocument> r = draftDocumentReader.searchAccessible(
                accessibleDocumentIds,
                q.status(),
                PageRequest.of(q.page(), q.size(), Sort.by(Sort.Direction.fromString(p[1]), p[0])));
        return new PageResult<>(
                r.getContent().stream().map(DraftDocumentResult::from).toList(),
                q.page(),
                q.size(),
                r.getTotalElements());
    }

    @Transactional
    public DraftDocumentResult completeExamine(CompleteExamineCommand command) {
        DraftDocument draftDocument = draftDocumentReader.read(command.draftDocumentId());
        accessValidator.validateAccess(draftDocument, command.memberId());
        List<SuggestionTerm> suggestionTerms = suggestionTermReader.readAll(draftDocument.getId());
        suggestionTermProcessor.complete(draftDocument, suggestionTerms);
        draftDocumentEventPublisher.publishExamined(draftDocument);

        log.info(
                "[DraftDocumentService.completeExamine] Draft document examination completed. draftDocumentId={}, documentId={}, memberId={}",
                draftDocument.getId(),
                draftDocument.getDocumentId(),
                command.memberId());

        return DraftDocumentResult.from(draftDocument);
    }

    @Transactional(readOnly = true)
    public ExamineProgressResult readExamineProgress(Long draftDocumentId, Long memberId) {
        DraftDocument draftDocument = draftDocumentReader.read(draftDocumentId);
        accessValidator.validateAccess(draftDocument, memberId);
        List<SuggestionTerm> suggestionTerms = suggestionTermReader.readAll(draftDocumentId);
        String previewBody = suggestionTermProcessor.preview(draftDocument, suggestionTerms);
        return ExamineProgressResult.from(suggestionTerms, previewBody);
    }
}
