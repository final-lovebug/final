package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import lombok.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reviewer extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reviewRequestId;

    @Column(nullable = false, updatable = false)
    private Long memberId;

    @Column(nullable = false, updatable = false)
    private OffsetDateTime assignedAt;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Reviewer(Long requestId, Long memberId, Long createdBy) {
        if (requestId == null || memberId == null || createdBy == null)
            throw new IllegalArgumentException("reviewer fields are required");
        this.reviewRequestId = requestId;
        this.memberId = memberId;
        this.createdBy = createdBy;
        this.assignedAt = OffsetDateTime.now();
    }

    public static Reviewer create(Long requestId, Long memberId, Long createdBy) {
        return new Reviewer(requestId, memberId, createdBy);
    }
}
