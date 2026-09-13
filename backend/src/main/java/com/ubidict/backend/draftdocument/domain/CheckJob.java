package com.ubidict.backend.draftdocument.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CheckJob extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long documentId;

    @Column(nullable = false, updatable = false)
    private Long requestedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CheckJobStatus status;

    private Long draftDocumentId;

    @Column(length = 1000)
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private CheckJob(Long documentId, Long requestedBy) {
        if (documentId == null || requestedBy == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_REQUEST);
        }
        this.documentId = documentId;
        this.requestedBy = requestedBy;
        this.status = CheckJobStatus.PENDING;
        this.createdBy = requestedBy;
    }

    public static CheckJob create(Long documentId, Long requestedBy) {
        return new CheckJob(documentId, requestedBy);
    }

    public void start() {
        if (status == CheckJobStatus.RUNNING) {
            return;
        }
        validateStatus(CheckJobStatus.PENDING);
        status = CheckJobStatus.RUNNING;
    }

    public void succeed(Long draftDocumentId) {
        if (status == CheckJobStatus.SUCCEEDED && this.draftDocumentId.equals(draftDocumentId)) {
            return;
        }
        validateStatus(CheckJobStatus.RUNNING);
        if (draftDocumentId == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
        this.draftDocumentId = draftDocumentId;
        this.failureReason = null;
        this.status = CheckJobStatus.SUCCEEDED;
    }

    public void fail(String failureReason) {
        if (status == CheckJobStatus.FAILED) {
            return;
        }
        if (status != CheckJobStatus.PENDING && status != CheckJobStatus.RUNNING) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
        String normalized =
                failureReason == null || failureReason.isBlank() ? "대조 작업 처리에 실패했습니다." : failureReason.strip();
        this.failureReason = normalized.substring(0, Math.min(normalized.length(), 1000));
        this.status = CheckJobStatus.FAILED;
    }

    public boolean isInProgress() {
        return status == CheckJobStatus.PENDING || status == CheckJobStatus.RUNNING;
    }

    private void validateStatus(CheckJobStatus expected) {
        if (status != expected) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_CHECK_INVALID_STATUS);
        }
    }
}
