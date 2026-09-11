package com.ubidict.backend.reviewrequest.presentation.dto;

import com.ubidict.backend.reviewrequest.service.model.UpdateReviewRequestCommand;
import jakarta.validation.constraints.Size;

public record UpdateReviewRequestRequest(@Size(max = 255) String title, String description) {

    public UpdateReviewRequestCommand toCommand(Long reviewRequestId, Long memberId) {
        return new UpdateReviewRequestCommand(reviewRequestId, title, description, memberId);
    }
}
