package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.Reviewer;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewerRepository extends JpaRepository<Reviewer, Long> {
    boolean existsByReviewRequestIdAndMemberId(Long requestId, Long memberId);

    long countByReviewRequestId(Long requestId);

    List<Reviewer> findByReviewRequestId(Long requestId);

    Optional<Reviewer> findByIdAndDeletedAtIsNull(Long id);
}
