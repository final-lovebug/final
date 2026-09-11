package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRequestRepository extends JpaRepository<ReviewRequest, Long> {

    Optional<ReviewRequest> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select reviewRequest from ReviewRequest reviewRequest
            where reviewRequest.workspaceId = :workspaceId
              and reviewRequest.status = :status
              and reviewRequest.deletedAt is null
            """)
    List<ReviewRequest> findByWorkspaceIdAndStatus(
            @Param("workspaceId") Long workspaceId, @Param("status") ReviewRequestStatus status);
}
