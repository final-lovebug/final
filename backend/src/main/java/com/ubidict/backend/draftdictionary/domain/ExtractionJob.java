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

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ExtractionJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(updatable = false)
    private Long dictionaryId;

    @ElementCollection
    @CollectionTable(name = "extraction_job_source_document", joinColumns = @JoinColumn(name = "extraction_job_id"))
    @Column(name = "document_id", nullable = false)
    private List<Long> sourceDocumentIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ExtractionJobStatus status;

    private Long draftDictionaryId;

    @Column(length = 1000)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private ExtractionJob(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        validate(workspaceId, sourceDocumentIds, requestedBy);
        this.workspaceId = workspaceId;
        this.dictionaryId = dictionaryId;
        this.sourceDocumentIds = new ArrayList<>(sourceDocumentIds);
        this.requestedBy = requestedBy;
        this.status = ExtractionJobStatus.PENDING;
        this.createdBy = requestedBy;
    }

    public static ExtractionJob create(
            Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        return new ExtractionJob(workspaceId, dictionaryId, sourceDocumentIds, requestedBy);
    }

    public void start() {
        if (status == ExtractionJobStatus.RUNNING) return;
        validateStatus(ExtractionJobStatus.PENDING);
        status = ExtractionJobStatus.RUNNING;
    }

    public void succeed(Long draftDictionaryId) {
        if (status == ExtractionJobStatus.SUCCEEDED && this.draftDictionaryId.equals(draftDictionaryId)) return;
        validateStatus(ExtractionJobStatus.RUNNING);
        if (draftDictionaryId == null) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
        this.draftDictionaryId = draftDictionaryId;
        this.failureReason = null;
        this.status = ExtractionJobStatus.SUCCEEDED;
    }

    public void fail(String failureReason) {
        if (status == ExtractionJobStatus.FAILED) return;
        if (!isInProgress()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
        String normalized =
                failureReason == null || failureReason.isBlank() ? "용어 추출 작업 처리에 실패했습니다." : failureReason.strip();
        this.failureReason = normalized.substring(0, Math.min(normalized.length(), 1000));
        this.status = ExtractionJobStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == ExtractionJobStatus.PENDING || status == ExtractionJobStatus.RUNNING;
    }

    private void validateStatus(ExtractionJobStatus expected) {
        if (status != expected) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_STATUS);
        }
    }

    private static void validate(Long workspaceId, List<Long> sourceDocumentIds, Long requestedBy) {
        if (workspaceId == null
                || requestedBy == null
                || sourceDocumentIds == null
                || sourceDocumentIds.isEmpty()
                || sourceDocumentIds.stream().anyMatch(id -> id == null)
                || new HashSet<>(sourceDocumentIds).size() != sourceDocumentIds.size()) {
            throw new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_EXTRACTION_INVALID_REQUEST);
        }
    }
}
