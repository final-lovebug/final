package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.ResolveCommentCommand;
import jakarta.validation.constraints.NotNull;

public record ResolveCommentRequest(@NotNull Boolean resolved) {

    public ResolveCommentCommand toCommand(Long commentId, Long actorId) {
        return new ResolveCommentCommand(commentId, actorId, resolved);
    }
}
