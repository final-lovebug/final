package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
// 회차는 요청마다 한 번씩만 열린다.
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_reexamine_request_round",
                        columnNames = {"review_request_id", "round"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reexamine extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reviewRequestId;

    @Column(nullable = false, updatable = false)
    private int round;

    @Column(nullable = false, updatable = false)
    private Long performedBy;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "reexamine_addressed_comment", joinColumns = @JoinColumn(name = "reexamine_id"))
    @Column(name = "comment_id", nullable = false)
    private List<Long> addressedCommentIds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private OffsetDateTime performedAt;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Reexamine(Long reviewRequestId, int round, Long performedBy, List<Long> addressedCommentIds) {
        if (round < 1) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        this.reviewRequestId = reviewRequestId;
        this.round = round;
        this.performedBy = performedBy;
        this.addressedCommentIds =
                addressedCommentIds == null ? new ArrayList<>() : new ArrayList<>(addressedCommentIds);
        this.performedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
        this.createdBy = performedBy;
    }

    public static Reexamine perform(Long reviewRequestId, int round, Long performedBy, List<Long> addressedCommentIds) {
        return new Reexamine(reviewRequestId, round, performedBy, addressedCommentIds);
    }

    public List<Long> getAddressedCommentIds() {
        return List.copyOf(addressedCommentIds);
    }
}
