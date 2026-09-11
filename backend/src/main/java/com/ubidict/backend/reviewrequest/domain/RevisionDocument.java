package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
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

    @Column(nullable = false, columnDefinition = "text")
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
}
