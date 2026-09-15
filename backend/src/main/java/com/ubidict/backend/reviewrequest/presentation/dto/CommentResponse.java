package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.reviewrequest.service.model.CommentResult;
import java.time.OffsetDateTime;
import java.util.List;

public record CommentResponse(
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
        List<CommentResponse> children) {

    public static CommentResponse from(CommentResult result) {
        return new CommentResponse(
                result.commentId(),
                result.reviewId(),
                result.authorId(),
                result.content(),
                result.anchor(),
                result.targetItemId(),
                result.parentId(),
                result.resolved(),
                result.createdAt(),
                result.updatedAt(),
                result.children().stream().map(CommentResponse::from).toList());
    }
}
