package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.reviewrequest.domain.Comment;
import java.time.OffsetDateTime;
import java.util.List;

public record CommentResult(
        Long commentId,
        Long reviewId,
        Long authorId,
        String content,
        TextRange anchor,
        Long targetItemId,
        Long parentId,
        boolean resolved,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<CommentResult> children) {

    public static CommentResult from(Comment comment) {
        return from(comment, List.of());
    }

    public static CommentResult from(Comment comment, List<CommentResult> children) {
        return new CommentResult(
                comment.getId(),
                comment.getReviewId(),
                comment.getAuthorId(),
                comment.getContent(),
                comment.getAnchor(),
                comment.getTargetItemId(),
                comment.getParentId(),
                comment.isResolved(),
                comment.getCreatedAt(),
                comment.getUpdatedAt(),
                List.copyOf(children));
    }
}
