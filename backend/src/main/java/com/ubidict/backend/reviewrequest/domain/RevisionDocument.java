package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import jakarta.persistence.*;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@Table(indexes = @Index(name = "idx_revision_document_request", columnList = "review_request_id"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevisionDocument extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reviewRequestId;

    @Column(nullable = false)
    private Long documentId;

    @Column(nullable = false)
    private int baseVersionNo;

    @Column(nullable = false)
    private Long draftDocumentId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String proposedBody;

    @Column(nullable = false)
    private int reexamineRound;

    private Integer resultVersionNo;

    @Column(nullable = false)
    private Long createdBy;

    private RevisionDocument(Long r, Long d, int v, Long dd, String body, Long by) {
        if (body == null || body.isBlank()) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        reviewRequestId = r;
        documentId = d;
        baseVersionNo = v;
        draftDocumentId = dd;
        proposedBody = body;
        createdBy = by;
    }

    public static RevisionDocument create(Long r, Long d, int v, Long dd, String b, Long by) {
        return new RevisionDocument(r, d, v, dd, b, by);
    }

    public static RevisionDocument reexamine(
            RevisionDocument previous, int round, String proposedBody, Long createdBy) {
        RevisionDocument revision = new RevisionDocument(
                previous.reviewRequestId,
                previous.documentId,
                previous.baseVersionNo,
                previous.draftDocumentId,
                proposedBody,
                createdBy);
        revision.reexamineRound = round;
        return revision;
    }

    public void recordResult(int resultVersionNo) {
        if (resultVersionNo < 1 || this.resultVersionNo != null) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        this.resultVersionNo = resultVersionNo;
    }
}
