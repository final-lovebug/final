package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.domain.CandidateTermOrigin;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.CandidateTermType;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * 후보어 한 건.
 *
 * <p><b>{@code createdBy}를 담지 않는다</b>({@code docs/plan/DRAFT_PLAN.md}). 초안 화면이 후보어별 작성자를 더 이상 보여주지
 * 않기 때문이다 — 작성자는 초안 하나에 한 명이고 그것은 초안 응답의 {@code createdBy}로 충분하다. DB 컬럼은 감사 용도로 남겨 둔다.
 */
public record CandidateTermResponse(
        Long candidateTermId,
        Long draftDictionaryId,
        CandidateTermOrigin origin,
        Long sourceTermId,
        String form,
        String proposedDefinition,
        String proposedEnglishName,
        Integer occurrenceCount,
        CandidateTermStatus status,
        CandidateTermType type,
        Long handledBy,
        String rejectReason,
        Long mergeTargetTermId,
        Long resultTermId,
        List<Long> occurredDocumentIds,
        List<String> contextSnippets,
        List<String> variantForms,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {
    public static CandidateTermResponse from(CandidateTermResult r) {
        return new CandidateTermResponse(
                r.candidateTermId(),
                r.draftDictionaryId(),
                r.origin(),
                r.sourceTermId(),
                r.form(),
                r.proposedDefinition(),
                r.proposedEnglishName(),
                r.occurrenceCount(),
                r.status(),
                r.type(),
                r.handledBy(),
                r.rejectReason(),
                r.mergeTargetTermId(),
                r.resultTermId(),
                r.occurredDocumentIds(),
                r.contextSnippets(),
                r.variantForms(),
                r.createdAt(),
                r.updatedAt());
    }
}
