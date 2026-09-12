package com.ubidict.backend.draftdocument.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
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

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SuggestionTerm extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long draftDocumentId;

    @Embedded
    private TextRange anchor;

    @Column(nullable = false)
    private String originTerm;

    @Column(nullable = false)
    private String suggestionTerm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SuggestionTermStatus status;

    private Long handledBy;

    @Lob
    private String rejectReason;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private SuggestionTerm(
            Long draftDocumentId, TextRange anchor, String originTerm, String suggestionTerm, Long createdBy) {
        this.draftDocumentId = draftDocumentId;
        this.anchor = anchor;
        this.originTerm = required(originTerm);
        this.suggestionTerm = required(suggestionTerm);
        this.createdBy = createdBy;
        this.status = SuggestionTermStatus.PENDING;
    }

    public static SuggestionTerm create(
            Long draftDocumentId, TextRange anchor, String originTerm, String suggestionTerm, Long createdBy) {
        return new SuggestionTerm(draftDocumentId, anchor, originTerm, suggestionTerm, createdBy);
    }

    public void edit(TextRange anchor, String origin, String suggestion) {
        if (anchor != null) {
            this.anchor = anchor;
        }
        if (origin != null) {
            this.originTerm = required(origin);
        }
        if (suggestion != null) {
            this.suggestionTerm = required(suggestion);
        }
    }

    public void accept(Long handlerId) {
        this.status = SuggestionTermStatus.APPLIED_SUGGESTION;
        this.handledBy = handlerId;
        this.rejectReason = null;
    }

    public void reject(Long handlerId, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_REJECT_REASON_REQUIRED);
        }

        this.status = SuggestionTermStatus.KEPT_ORIGIN;
        this.handledBy = handlerId;
        this.rejectReason = rejectReason.strip();
    }

    public boolean isPending() {
        return status == SuggestionTermStatus.PENDING;
    }

    public boolean isApplied() {
        return status == SuggestionTermStatus.APPLIED_SUGGESTION;
    }

    private static String required(String v) {
        if (v == null || v.isBlank()) {
            throw new BusinessException(DraftDocumentErrorCode.DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM);
        }
        return v.strip();
    }
}
