package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Revise extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reviewRequestId;

    @Column(nullable = false, updatable = false)
    private int resultVersionNo;

    @Column(nullable = false, updatable = false)
    private Long performedBy;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime performedAt;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Revise(Long reviewRequestId, int resultVersionNo, Long performedBy) {
        if (resultVersionNo < 1) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        this.reviewRequestId = reviewRequestId;
        this.resultVersionNo = resultVersionNo;
        this.performedBy = performedBy;
        this.performedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.createdBy = performedBy;
    }

    public static Revise perform(Long reviewRequestId, int resultVersionNo, Long performedBy) {
        return new Revise(reviewRequestId, resultVersionNo, performedBy);
    }
}
