package com.ubidict.backend.draftdictionary.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CandidateTerm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long draftDictionaryId;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CandidateTermOrigin origin;

    private Long handledBy;

    @Lob
    private String rejectReason;

    private Long mergeTargetTermId;
    private Long resultTermId;

    private Long sourceTermId;

    @Column(nullable = false, length = 200)
    private String form;

    private String proposedDefinition;
    private String proposedEnglishName;

    @Column(nullable = false)
    private Integer occurrenceCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CandidateTermStatus status;

    /** 사람이 고른 분류. 추출 생성분은 비어 있다({@link CandidateTermType} 참고). */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private CandidateTermType type;

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "candidate_term_occurred_document", joinColumns = @JoinColumn(name = "candidate_term_id"))
    @Column(name = "document_id")
    private List<Long> occurredDocumentIds = new ArrayList<>();

    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "candidate_term_context_snippet", joinColumns = @JoinColumn(name = "candidate_term_id"))
    @Column(name = "snippet", length = 1000)
    private List<String> contextSnippets = new ArrayList<>();

    /**
     * 추출기가 같은 개념으로 묶어서 돌려준 표기 변형들(ubidict-py {@code GroupCandidate.forms}). 대표 표기는
     * {@link #form}에 남고, 그 대표 표기를 포함해 그룹에 속했던 모든 표기가 여기 보존된다 — 프론트에서 "같은 개념의
     * 여러 표기"를 묶어 보여주는 화면을 만들 때 쓴다(T-INT-11 후속 과제, `D-65`).
     */
    @ElementCollection
    @BatchSize(size = 100)
    @CollectionTable(name = "candidate_term_variant_form", joinColumns = @JoinColumn(name = "candidate_term_id"))
    @Column(name = "variant_form", length = 200)
    private List<String> variantForms = new ArrayList<>();

    private CandidateTerm(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets,
            List<String> variants,
            Long createdBy) {
        if (form == null || form.isBlank())
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_FORM);
        if (count < 1) throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_OCCURRENCE_COUNT);
        this.draftDictionaryId = draftDictionaryId;
        this.createdBy = createdBy;
        this.origin = CandidateTermOrigin.EXTRACTED;
        this.form = form;
        this.proposedDefinition = definition;
        this.proposedEnglishName = english;
        this.occurrenceCount = count;
        this.status = CandidateTermStatus.PENDING;
        if (docs != null) this.occurredDocumentIds = new ArrayList<>(docs);
        if (snippets != null) this.contextSnippets = new ArrayList<>(snippets);
        if (variants != null) this.variantForms = new ArrayList<>(variants);
    }

    public static CandidateTerm create(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets,
            List<String> variants,
            Long createdBy) {
        return new CandidateTerm(
                draftDictionaryId, form, definition, english, docs, count, snippets, variants, createdBy);
    }

    public static CandidateTerm create(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets,
            List<String> variants,
            Long createdBy,
            CandidateTermType type) {
        CandidateTerm candidate =
                create(draftDictionaryId, form, definition, english, docs, count, snippets, variants, createdBy);
        candidate.type = type;
        return candidate;
    }

    public static CandidateTerm create(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets,
            Long createdBy) {
        return create(draftDictionaryId, form, definition, english, docs, count, snippets, List.of(), createdBy);
    }

    public static CandidateTerm create(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets) {
        return create(draftDictionaryId, form, definition, english, docs, count, snippets, List.of(), null);
    }

    /**
     * 이전 사전집에서 승계한 용어를 초안에 싣는다(G-1).
     *
     * <p><b>{@code definition}을 반드시 함께 넘긴다.</b> 확정 사전집의 {@code Term.definition}은 비어 있을 수 없고, 발행은 이전 버전에서
     * 복사하지 않고 넘어온 목록만으로 새 버전을 만든다(R-12). 여기서 정의를 버리면 교정 화면에서 이전 정의를 볼 수 없고, 발행 시점에
     * {@code Term.create}가 거절한다(Y-29).
     */
    public static CandidateTerm createExisting(
            Long draftDictionaryId, Long sourceTermId, String form, String definition, String english, Long createdBy) {
        CandidateTerm candidate = new CandidateTerm(
                draftDictionaryId, form, definition, english, List.of(), 1, List.of(), List.of(), createdBy);
        candidate.origin = CandidateTermOrigin.EXISTING;
        candidate.sourceTermId = sourceTermId;
        candidate.occurrenceCount = null;
        candidate.status = CandidateTermStatus.KEPT;
        return candidate;
    }

    public void edit(String form, String definition, String english) {
        edit(form, definition, english, null);
    }

    public void edit(String form, String definition, String english, CandidateTermType type) {
        if (form != null && form.isBlank())
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_FORM);
        if (form != null) this.form = form;
        if (definition != null) this.proposedDefinition = definition;
        if (english != null) this.proposedEnglishName = english;
        if (type != null) this.type = type;
    }

    public void approveRegistration(Long handlerId) {
        if (proposedDefinition == null || proposedDefinition.isBlank())
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_DEFINITION_REQUIRED);
        decide(CandidateTermStatus.REGISTRATION_APPROVED, handlerId);
    }

    public void mergeAsSynonym(Long handlerId, Long targetId) {
        if (targetId == null)
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_MERGE_TARGET_REQUIRED);
        decide(CandidateTermStatus.MERGED_AS_SYNONYM, handlerId);
        mergeTargetTermId = targetId;
    }

    public void reject(Long handlerId, String reason) {
        if (reason == null || reason.isBlank())
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_REJECT_REASON_REQUIRED);
        decide(CandidateTermStatus.REJECTED, handlerId);
        rejectReason = reason.strip();
    }

    public void hold(Long handlerId) {
        decide(CandidateTermStatus.ON_HOLD, handlerId);
    }

    private void decide(CandidateTermStatus next, Long handlerId) {
        status = next;
        handledBy = handlerId;
        rejectReason = null;
        mergeTargetTermId = null;
    }

    public boolean isPending() {
        return status == CandidateTermStatus.PENDING;
    }

    public boolean isDecided() {
        return status != CandidateTermStatus.PENDING;
    }

    public boolean isRegistrationApproved() {
        return status == CandidateTermStatus.REGISTRATION_APPROVED;
    }

    public boolean isMergedAsSynonym() {
        return status == CandidateTermStatus.MERGED_AS_SYNONYM;
    }

    public boolean isRejected() {
        return status == CandidateTermStatus.REJECTED;
    }

    public boolean isKept() {
        return status == CandidateTermStatus.KEPT;
    }

    public boolean isOnHold() {
        return status == CandidateTermStatus.ON_HOLD;
    }

    public boolean isPublished() {
        return isRegistrationApproved() || isKept();
    }
}
