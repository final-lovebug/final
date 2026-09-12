package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.service.model.CreateReviewRequestCommand;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReviewRequestRequest(
        @NotNull Long workspaceId,
        @NotNull ReviewRequestType type,
        @NotBlank @Size(max = 255) String title,
        String description) {

    public CreateReviewRequestCommand toCommand(Long memberId) {
        return new CreateReviewRequestCommand(workspaceId, type, title, description, memberId);
    }
}
