package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.Reviewer;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewerRepository extends JpaRepository<Reviewer, Long> {
    boolean existsByReviewRequestIdAndMemberId(Long requestId, Long memberId);

    long countByReviewRequestId(Long requestId);

    List<Reviewer> findByReviewRequestId(Long requestId);

    Optional<Reviewer> findByIdAndDeletedAtIsNull(Long id);

    /** 목록 조회에서 항목마다 count 쿼리를 따로 안 날리려고 한 번에 모아 센다(T-INT-12). */
    @Query("""
            select new com.ubidict.backend.reviewrequest.infra.ReviewerCount(r.reviewRequestId, count(r))
            from Reviewer r
            where r.reviewRequestId in :reviewRequestIds
            group by r.reviewRequestId
            """)
    List<ReviewerCount> countByReviewRequestIdIn(@Param("reviewRequestIds") Collection<Long> reviewRequestIds);
}
