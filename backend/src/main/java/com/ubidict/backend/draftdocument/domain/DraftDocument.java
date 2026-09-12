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
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 사전집 대조 결과를 사람이 교정하는 문서 초안. */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DraftDocument extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long documentId;

    @Column(nullable = false, updatable = false)
    private int baseVersionNo;

    @Lob
    @Column(nullable = false)
    private String draftBody;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DraftDocumentStatus status;

    private Long requestedBy;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private DraftDocument(Long documentId, int baseVersionNo, String draftBody, Long requestedBy, Long createdBy) {
        this.documentId = documentId;
        this.baseVersionNo = validateBaseVersionNo(baseVersionNo);
        this.draftBody = normalizeBody(draftBody);
        this.status = DraftDocumentStatus.EXAMINING;
        this.requestedBy = requestedBy;
        this.createdBy = createdBy;
    }

    public static DraftDocument create(
            Long documentId, int baseVersionNo, String draftBody, Long requestedBy, Long createdBy) {
        return new DraftDocument(documentId, baseVersionNo, draftBody, requestedBy, createdBy);
    }

    public void updateBody(String draftBody) {
        validateExamining();
        this.draftBody = normalizeBody(draftBody);
    }

    public void markExamined(String composedBody) {
        validateExamining();
        this.draftBody = normalizeBody(composedBody);
        this.status = DraftDocumentStatus.EXAMINED;
    }

    public void validateExamining() {
        if (status != DraftDocumentStatus.EXAMINING) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_ALREADY_EXAMINED);
        }
    }

    public boolean isExamining() {
        return status == DraftDocumentStatus.EXAMINING;
    }

    public boolean isExamined() {
        return status == DraftDocumentStatus.EXAMINED;
    }

    private static int validateBaseVersionNo(int baseVersionNo) {
        if (baseVersionNo < 1) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_BASE_VERSION);
        }

        return baseVersionNo;
    }

    private static String normalizeBody(String draftBody) {
        if (draftBody == null) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_BODY);
        }

        String normalized = draftBody.strip();
        if (normalized.isEmpty()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_BODY);
        }

        return normalized;
    }
}
