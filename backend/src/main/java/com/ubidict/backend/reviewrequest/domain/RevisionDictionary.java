package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
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
}
