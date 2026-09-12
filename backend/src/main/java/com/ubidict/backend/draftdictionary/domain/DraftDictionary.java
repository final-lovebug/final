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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftDictionary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(updatable = false)
    private Long dictionaryId;

    @ElementCollection
    @CollectionTable(name = "draft_dictionary_source_document", joinColumns = @JoinColumn(name = "draft_dictionary_id"))
    @Column(name = "document_id", nullable = false)
    private List<Long> sourceDocumentIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DraftDictionaryStatus status;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private DraftDictionary(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long createdBy) {
        validateSourceDocumentIds(sourceDocumentIds);
        this.workspaceId = workspaceId;
        this.dictionaryId = dictionaryId;
        this.sourceDocumentIds = new ArrayList<>(sourceDocumentIds);
        this.status = DraftDictionaryStatus.EXAMINING;
        this.createdBy = createdBy;
    }

    public static DraftDictionary create(
            Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long createdBy) {
        return new DraftDictionary(workspaceId, dictionaryId, sourceDocumentIds, createdBy);
    }

    public void replaceSourceDocuments(List<Long> sourceDocumentIds) {
        validateSourceDocumentIds(sourceDocumentIds);
        this.sourceDocumentIds = new ArrayList<>(sourceDocumentIds);
    }

    public boolean isExamining() {
        return status == DraftDictionaryStatus.EXAMINING;
    }

    public boolean isExamined() {
        return status == DraftDictionaryStatus.EXAMINED;
    }

    public boolean isRevised() {
        return status == DraftDictionaryStatus.REVISED;
    }

    public void markExamined() {
        validateExaminingForCompletion();
        status = DraftDictionaryStatus.EXAMINED;
    }

    public void markReviewRequested() {
        if (status == DraftDictionaryStatus.REVIEW_REQUESTED)
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_REVIEW_REQUESTED);
        if (!isExamined()) throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINED);
        status = DraftDictionaryStatus.REVIEW_REQUESTED;
    }

    public void validateExamining() {
        if (!isExamining()) throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINABLE);
    }

    public void validateExaminingForCompletion() {
        if (isExamined()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_EXAMINED);
        }
        validateExamining();
    }

    public void validateExaminedForReview() {
        if (status == DraftDictionaryStatus.REVIEW_REQUESTED) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_ALREADY_REVIEW_REQUESTED);
        }
        if (status != DraftDictionaryStatus.EXAMINED) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_NOT_EXAMINED);
        }
    }

    private static void validateSourceDocumentIds(List<Long> sourceDocumentIds) {
        if (sourceDocumentIds == null || sourceDocumentIds.isEmpty()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_SOURCE_DOCUMENT_REQUIRED);
        }
        if (sourceDocumentIds.stream().anyMatch(id -> id == null)) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_SOURCE_DOCUMENT_REQUIRED);
        }
        if (new HashSet<>(sourceDocumentIds).size() != sourceDocumentIds.size()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_DUPLICATE_SOURCE_DOCUMENT);
        }
    }
}
