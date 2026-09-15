package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.reviewrequest.service.model.AddCommentCommand;
import jakarta.validation.constraints.NotBlank;

public record AddCommentRequest(@NotBlank String content, TextRange anchor, Long targetItemId, Long parentId) {

    public AddCommentCommand toCommand(Long reviewId, Long authorId) {
        return new AddCommentCommand(reviewId, authorId, content, anchor, targetItemId, parentId);
    }
}
