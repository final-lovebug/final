package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.Review;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class LatestReviewAggregator {

    public ReviewAggregate aggregate(List<Review> reviews) {
        Map<Long, Review> latestByMember = new LinkedHashMap<>();
        for (Review review : reviews) {
            latestByMember.merge(review.getMemberId(), review, LatestReviewAggregator::later);
        }

        int approvedCount = (int)
                latestByMember.values().stream().filter(Review::isApproval).count();
        return new ReviewAggregate(approvedCount, latestByMember.size() - approvedCount);
    }

    private static Review later(Review first, Review second) {
        return reviewOrder().compare(first, second) <= 0 ? second : first;
    }

    private static Comparator<Review> reviewOrder() {
        return Comparator.comparing(Review::getSubmittedAt)
                .thenComparing(Review::getId, Comparator.nullsFirst(Comparator.naturalOrder()));
    }

    public record ReviewAggregate(int approvedCount, int changesRequestedCount) {}
}
