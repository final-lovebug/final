package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RevisionDictionary extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reviewRequestId;

    @Column
    private Long dictionaryId;

    @Column(nullable = false)
    private int baseVersionNo;

    @Column(nullable = false)
    private Long draftDictionaryId;

    @Column(nullable = false)
    private int reexamineRound;

    private Integer resultVersionNo;

    @Column(nullable = false)
    private Long createdBy;

    private RevisionDictionary(Long r, Long d, int v, Long dd, Long by) {
        reviewRequestId = r;
        dictionaryId = d;
        baseVersionNo = v;
        draftDictionaryId = dd;
        createdBy = by;
    }

    public static RevisionDictionary create(Long r, Long d, int v, Long dd, Long by) {
        return new RevisionDictionary(r, d, v, dd, by);
    }

    public static RevisionDictionary reexamine(RevisionDictionary previous, int round, Long createdBy) {
        RevisionDictionary revision = new RevisionDictionary(
                previous.reviewRequestId,
                previous.dictionaryId,
                previous.baseVersionNo,
                previous.draftDictionaryId,
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
