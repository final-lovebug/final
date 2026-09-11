package com.ubidict.backend.draftdictionary.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import jakarta.persistence.*;
import java.util.*;
import lombok.*;
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

    private CandidateTerm(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets,
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
        return new CandidateTerm(draftDictionaryId, form, definition, english, docs, count, snippets, createdBy);
    }

    public static CandidateTerm create(
            Long draftDictionaryId,
            String form,
            String definition,
            String english,
            List<Long> docs,
            int count,
            List<String> snippets) {
        return create(draftDictionaryId, form, definition, english, docs, count, snippets, null);
    }

    public static CandidateTerm createExisting(
            Long draftDictionaryId, Long sourceTermId, String form, String english, Long createdBy) {
        CandidateTerm candidate =
                new CandidateTerm(draftDictionaryId, form, null, english, List.of(), 1, List.of(), createdBy);
        candidate.origin = CandidateTermOrigin.EXISTING;
        candidate.sourceTermId = sourceTermId;
        candidate.occurrenceCount = null;
        candidate.status = CandidateTermStatus.KEPT;
        return candidate;
    }

    public void edit(String form, String definition, String english) {
        if (status != CandidateTermStatus.PENDING)
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_EXAMINABLE);
        if (form != null && form.isBlank())
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_INVALID_FORM);
        if (form != null) this.form = form;
        if (definition != null) this.proposedDefinition = definition;
        if (english != null) this.proposedEnglishName = english;
    }
}
