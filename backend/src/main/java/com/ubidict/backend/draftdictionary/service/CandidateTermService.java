package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.implement.CandidateTermBulkDecisionProcessor;
import com.ubidict.backend.draftdictionary.implement.CandidateTermDecisionProcessor;
import com.ubidict.backend.draftdictionary.implement.CandidateTermReader;
import com.ubidict.backend.draftdictionary.implement.CandidateTermWriter;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryEventPublisher;
import com.ubidict.backend.draftdictionary.implement.DraftDictionaryReader;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.BulkDecideCandidateTermsCommand;
import com.ubidict.backend.draftdictionary.service.model.BulkDecisionResult;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermSearchQuery;
import com.ubidict.backend.draftdictionary.service.model.DecideCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.EditCandidateTermCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateTermService {
    private final CandidateTermReader reader;
    private final CandidateTermWriter writer;
    private final DraftDictionaryReader draftReader;
    private final CandidateTermDecisionProcessor decisionProcessor;
    private final CandidateTermBulkDecisionProcessor bulkProcessor;
    private final DraftDictionaryEventPublisher eventPublisher;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional
    public CandidateTermResult add(AddCandidateTermCommand c) {
        DraftDictionary draft = draftReader.read(c.draftDictionaryId());
        workspaceAccessValidator.validateParticipant(draft.getWorkspaceId(), c.memberId());
        draft.validateExamining();
        CandidateTermResult result = CandidateTermResult.from(writer.add(c));
        log.info(
                "[CandidateTermService.add] Candidate term added. draftDictionaryId={}, memberId={}",
                c.draftDictionaryId(),
                c.memberId());
        return result;
    }

    @Transactional(readOnly = true)
    public CandidateTermResult read(Long id, Long memberId) {
        CandidateTerm term = reader.read(id);
        validateAccess(term.getDraftDictionaryId(), memberId);
        return CandidateTermResult.from(term);
    }

    @Transactional
    public CandidateTermResult edit(EditCandidateTermCommand c) {
        CandidateTerm term = reader.read(c.candidateTermId());
        DraftDictionary draft = draftReader.read(term.getDraftDictionaryId());
        workspaceAccessValidator.validateParticipant(draft.getWorkspaceId(), c.memberId());
        draft.validateExamining();
        CandidateTermResult result = CandidateTermResult.from(writer.edit(term, c));
        log.info(
                "[CandidateTermService.edit] Candidate term edited. candidateTermId={}, memberId={}",
                c.candidateTermId(),
                c.memberId());
        return result;
    }

    @Transactional
    public void delete(Long id, Long memberId) {
        CandidateTerm term = reader.read(id);
        DraftDictionary draft = draftReader.read(term.getDraftDictionaryId());
        workspaceAccessValidator.validateParticipant(draft.getWorkspaceId(), memberId);
        draft.validateExamining();
        term.delete();
        log.info("[CandidateTermService.delete] Candidate term deleted. candidateTermId={}", id);
    }

    @Transactional(readOnly = true)
    public PageResult<CandidateTermResult> search(CandidateTermSearchQuery q) {
        validateAccess(q.draftDictionaryId(), q.memberId());
        return reader.search(q);
    }

    @Transactional
    public CandidateTermResult decide(DecideCandidateTermCommand c) {
        CandidateTerm term = reader.read(c.candidateTermId());
        DraftDictionary draft = draftReader.read(term.getDraftDictionaryId());
        workspaceAccessValidator.validateParticipant(draft.getWorkspaceId(), c.memberId());
        CandidateTermResult result = CandidateTermResult.from(decisionProcessor.decide(draft, term, c));
        eventPublisher.publishCandidateDecided(term);
        log.info(
                "[CandidateTermService.decide] Candidate term decided. candidateTermId={}, memberId={}",
                c.candidateTermId(),
                c.memberId());
        return result;
    }

    @Transactional
    public BulkDecisionResult bulkDecide(BulkDecideCandidateTermsCommand c) {
        DraftDictionary draft = draftReader.read(c.draftDictionaryId());
        workspaceAccessValidator.validateParticipant(draft.getWorkspaceId(), c.memberId());
        BulkDecisionResult result = bulkProcessor.decide(draft, reader.readAll(c.draftDictionaryId()), c);
        reader.readAll(c.draftDictionaryId()).stream()
                .filter(term -> result.succeeded().contains(term.getId()))
                .forEach(eventPublisher::publishCandidateDecided);
        log.info(
                "[CandidateTermService.bulkDecide] Candidate terms decided. draftDictionaryId={}, memberId={}",
                c.draftDictionaryId(),
                c.memberId());
        return result;
    }

    private void validateAccess(Long draftDictionaryId, Long memberId) {
        DraftDictionary draft = draftReader.read(draftDictionaryId);
        workspaceAccessValidator.validateParticipant(draft.getWorkspaceId(), memberId);
    }
}
