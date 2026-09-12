package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.fixture.ReviewFixture;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LatestReviewAggregatorTest {

    private final LatestReviewAggregator aggregator = new LatestReviewAggregator();

    @DisplayName("같은 회원이 여러 번 리뷰하면 최신 판정만 집계한다.")
    @Test
    void aggregate_keepsOnlyLatestPerMember() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        var reviews = List.of(
                ReviewFixture.review()
                        .id(1L)
                        .memberId(1L)
                        .verdict(ReviewVerdict.CHANGES_REQUESTED)
                        .submittedAt(now.minusMinutes(1))
                        .build(),
                ReviewFixture.review()
                        .id(2L)
                        .memberId(1L)
                        .verdict(ReviewVerdict.APPROVED)
                        .submittedAt(now)
                        .build());

        // when
        var result = aggregator.aggregate(reviews);

        // then
        assertThat(result.approvedCount()).isEqualTo(1);
        assertThat(result.changesRequestedCount()).isZero();
    }

    @DisplayName("회차가 달라도 회원별 최신 판정에 함께 집계한다.")
    @Test
    void aggregate_ignoresTargetRound() {
        // given
        OffsetDateTime now = OffsetDateTime.now();
        var reviews = List.of(
                ReviewFixture.review()
                        .id(1L)
                        .memberId(1L)
                        .targetRound(0)
                        .verdict(ReviewVerdict.APPROVED)
                        .submittedAt(now.minusMinutes(1))
                        .build(),
                ReviewFixture.review()
                        .id(2L)
                        .memberId(2L)
                        .targetRound(1)
                        .verdict(ReviewVerdict.APPROVED)
                        .submittedAt(now)
                        .build());

        // when
        var result = aggregator.aggregate(reviews);

        // then
        assertThat(result.approvedCount()).isEqualTo(2);
    }
}
