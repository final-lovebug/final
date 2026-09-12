package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.implement.LatestReviewAggregator.ReviewAggregate;
import org.springframework.stereotype.Component;

@Component
public class ReviseEligibilityCalculator {

    public boolean isEligible(int requiredReviewerCount, ReviewAggregate aggregate) {
        if (requiredReviewerCount < 0) {
            throw new IllegalArgumentException("requiredReviewerCount must not be negative");
        }
        if (requiredReviewerCount == 0) {
            return true;
        }
        return aggregate.approvedCount() >= requiredReviewerCount && aggregate.changesRequestedCount() == 0;
    }
}
