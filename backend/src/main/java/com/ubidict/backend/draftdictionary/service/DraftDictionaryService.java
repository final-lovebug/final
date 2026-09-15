package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.implement.CandidateTermReader;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryCreationPolicyValidator;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryEventPublisher;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryReader;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryRemover;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryReviewReadinessValidator;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryWriter;
import com.ubidict.backend.draftdictionary.service.model.CompleteExamineCommand;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionaryResult;
import com.ubidict.backend.draftdictionary.service.model.DraftDictionarySearchQuery;
import com.ubidict.backend.draftdictionary.service.model.UpdateSourceDocumentsCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DraftDictionaryService {

    private final DraftDictionaryReader draftDictionaryReader;
    private final DraftDictionaryWriter draftDictionaryWriter;
    private final DraftDictionaryRemover draftDictionaryRemover;
    private final CandidateTermReader candidateTermReader;
    private final DraftDictionaryReviewReadinessValidator readinessValidator;
    private final DraftDictionaryCreationPolicyValidator creationPolicyValidator;
    private final DraftDictionaryEventPublisher eventPublisher;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional(readOnly = true)
    public DraftDictionaryResult read(Long draftDictionaryId, Long memberId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        workspaceAccessValidator.validateParticipant(draftDictionary.getWorkspaceId(), memberId);
        return DraftDictionaryResult.from(draftDictionary);
    }

    /** 워크스페이스 기준 사전 초안 목록 조회(T-INT-20, D-64). */
    @Transactional(readOnly = true)
    public PageResult<DraftDictionaryResult> search(DraftDictionarySearchQuery query) {
        workspaceAccessValidator.validateParticipant(query.workspaceId(), query.memberId());

        String[] p = query.sort().split(",");
        Page<DraftDictionary> page = draftDictionaryReader.search(
                query.workspaceId(),
                query.status(),
                PageRequest.of(query.page(), query.size(), Sort.by(Sort.Direction.fromString(p[1]), p[0])));

        return new PageResult<>(
                page.getContent().stream().map(DraftDictionaryResult::from).toList(),
                query.page(),
                query.size(),
                page.getTotalElements());
    }

    @Transactional
    public DraftDictionaryResult updateSourceDocuments(UpdateSourceDocumentsCommand command) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(command.draftDictionaryId());
        workspaceAccessValidator.validateParticipant(draftDictionary.getWorkspaceId(), command.memberId());
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
        workspaceAccessValidator.validateParticipant(draftDictionary.getWorkspaceId(), memberId);
        draftDictionaryRemover.remove(draftDictionary);

        log.info(
                "[DraftDictionaryService.delete] Draft dictionary deleted. draftDictionaryId={}, memberId={}",
                draftDictionaryId,
                memberId);
    }

    @Transactional
    public DraftDictionaryResult completeExamine(CompleteExamineCommand c) {
        DraftDictionary d = draftDictionaryReader.read(c.draftDictionaryId());
        workspaceAccessValidator.validateParticipant(d.getWorkspaceId(), c.memberId());
        readinessValidator.validateExamineCompletion(d, candidateTermReader.readAll(d.getId()));
        d.markExamined();
        log.info(
                "[DraftDictionaryService.completeExamine] Examination completed. draftDictionaryId={}, memberId={}",
                c.draftDictionaryId(),
                c.memberId());
        return DraftDictionaryResult.from(d);
    }

    /**
     * 리뷰 요청을 받을 상태인지 판정한다. 리뷰 요청 생성은 ReviewRequest 도메인이 하고(D-44) 이 메서드는 그쪽 어댑터가 호출한다.
     *
     * <p>교정 완료 여부만 보지 않는다 — 최종 등재 목록이 활성 사전집과 실제로 다른지와 그 정의가 비어 있지 않은지까지 본다(D-21). 상태를 바꾸지 않으며 전이는
     * ReviewRequestCreatedEvent를 받는 리스너가 한다.
     */
    @Transactional(readOnly = true)
    public void validateReviewReadiness(Long draftDictionaryId) {
        DraftDictionary draftDictionary = draftDictionaryReader.read(draftDictionaryId);
        readinessValidator.validateReviewRequest(draftDictionary, candidateTermReader.readAll(draftDictionaryId));
    }
}
