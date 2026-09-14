package com.ubidict.backend.reviewrequest.infra;

import com.ubidict.backend.reviewrequest.domain.Comment;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    Optional<Comment> findByIdAndDeletedAtIsNull(Long id);

    @Query("""
            select comment
            from Comment comment
            join Review review on review.id = comment.reviewId
            where review.reviewRequestId = :reviewRequestId
              and review.deletedAt is null
              and comment.deletedAt is null
              and (:resolved is null or comment.resolved = :resolved)
              and (:targetItemId is null or comment.targetItemId = :targetItemId)
            order by comment.createdAt asc, comment.id asc
            """)
    List<Comment> findByReviewRequestId(
            @Param("reviewRequestId") Long reviewRequestId,
            @Param("resolved") Boolean resolved,
            @Param("targetItemId") Long targetItemId);
}
