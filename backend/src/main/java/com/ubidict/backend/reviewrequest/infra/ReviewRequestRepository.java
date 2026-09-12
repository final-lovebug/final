package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.*;
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

    @Query(
            "select distinct r from ReviewRequest r left join Reviewer v on v.reviewRequestId=r.id and v.deletedAt is null where r.workspaceId=:workspaceId and (:type is null or r.type=:type) and (:status is null or r.status=:status) and (:requesterId is null or r.requesterId=:requesterId) and (:reviewerMemberId is null or v.memberId=:reviewerMemberId) and r.deletedAt is null")
    Page<ReviewRequest> search(
            @Param("workspaceId") Long workspaceId,
            @Param("type") ReviewRequestType type,
            @Param("status") ReviewRequestStatus status,
            @Param("requesterId") Long requesterId,
            @Param("reviewerMemberId") Long reviewerMemberId,
            Pageable pageable);
}
