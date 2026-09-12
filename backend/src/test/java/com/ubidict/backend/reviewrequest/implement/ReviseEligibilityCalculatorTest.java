package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.fixture.ReviewFixture;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviseEligibilityCalculatorTest {

    private final LatestReviewAggregator aggregator = new LatestReviewAggregator();
    private final ReviseEligibilityCalculator calculator = new ReviseEligibilityCalculator();

    @DisplayName("정족수가 0이면 변경요청이 있어도 발행할 수 있다.")
    @Test
    void isEligible_zeroQuorum() {
        var aggregate = aggregator.aggregate(List.of(review(1L, 0, ReviewVerdict.CHANGES_REQUESTED, 0)));

        assertThat(calculator.isEligible(0, aggregate)).isTrue();
    }

    @DisplayName("변경요청이 남아 있으면 발행할 수 없다.")
    @Test
    void isEligible_changesRequestedRemains() {
        var aggregate = aggregator.aggregate(
                List.of(review(1L, 0, ReviewVerdict.APPROVED, 0), review(2L, 0, ReviewVerdict.CHANGES_REQUESTED, 0)));

        assertThat(calculator.isEligible(1, aggregate)).isFalse();
    }

    @DisplayName("변경요청했던 리뷰어가 다시 승인하면 발행할 수 있다.")
    @Test
    void isEligible_reReviewClearsChangesRequested() {
        var aggregate = aggregator.aggregate(
                List.of(review(1L, 0, ReviewVerdict.CHANGES_REQUESTED, 0), review(1L, 0, ReviewVerdict.APPROVED, 1)));

        assertThat(calculator.isEligible(1, aggregate)).isTrue();
    }

    @DisplayName("재교정 후에도 이전 승인이 유지된다.")
    @Test
    void isEligible_approvalSurvivesReexamine() {
        var aggregate = aggregator.aggregate(
                List.of(review(1L, 0, ReviewVerdict.APPROVED, 0), review(2L, 1, ReviewVerdict.APPROVED, 1)));

        assertThat(calculator.isEligible(2, aggregate)).isTrue();
    }

    @DisplayName("승인 수가 정족수에 못 미치면 발행할 수 없다.")
    @Test
    void isEligible_quorumNotReached() {
        var aggregate = aggregator.aggregate(List.of(review(1L, 0, ReviewVerdict.APPROVED, 0)));

        assertThat(calculator.isEligible(2, aggregate)).isFalse();
    }

    private static com.ubidict.backend.reviewrequest.domain.Review review(
            Long memberId, int round, ReviewVerdict verdict, int minuteOffset) {
        return ReviewFixture.review()
                .id((long) minuteOffset + 1)
                .memberId(memberId)
                .targetRound(round)
                .verdict(verdict)
                .submittedAt(OffsetDateTime.now().plusMinutes(minuteOffset))
                .build();
    }
}
