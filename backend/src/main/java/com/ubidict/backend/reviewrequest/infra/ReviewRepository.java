package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndDeletedAtIsNull(Long id);

    List<Review> findByReviewRequestIdAndDeletedAtIsNullOrderBySubmittedAtAscIdAsc(Long reviewRequestId);

    List<Review> findByReviewRequestIdAndTargetRoundAndDeletedAtIsNullOrderBySubmittedAtAscIdAsc(
            Long reviewRequestId, int targetRound);

    long countByReviewRequestIdAndVerdictAndDeletedAtIsNull(Long reviewRequestId, ReviewVerdict verdict);

    @Query("""
            select review
            from Review review
            where review.reviewRequestId = :reviewRequestId
              and review.deletedAt is null
              and not exists (
                  select newer.id
                  from Review newer
                  where newer.reviewRequestId = review.reviewRequestId
                    and newer.memberId = review.memberId
                    and newer.deletedAt is null
                    and (
                        newer.submittedAt > review.submittedAt
                        or (newer.submittedAt = review.submittedAt and newer.id > review.id)
                    )
              )
            order by review.memberId asc
            """)
    List<Review> findLatestByReviewRequestId(@Param("reviewRequestId") Long reviewRequestId);
}
