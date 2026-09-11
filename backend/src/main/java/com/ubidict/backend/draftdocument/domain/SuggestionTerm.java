package com.ubidict.backend.draftdocument.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.domain.TextRange;
import jakarta.persistence.*;
import lombok.*;

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
        if (anchor != null) this.anchor = anchor;
        if (origin != null) this.originTerm = required(origin);
        if (suggestion != null) this.suggestionTerm = required(suggestion);
    }

    private static String required(String v) {
        if (v == null || v.isBlank())
            throw new com.ubidict.backend.common.exception.BusinessException(
                    com.ubidict.backend.draftdocument.exception.DraftDocumentErrorCode
                            .DRAFT_DOCUMENT_INVALID_SUGGESTION_TERM);
        return v.strip();
    }
}
