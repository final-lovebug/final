package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;

public record CreateReviewRequestCommand(
        Long workspaceId, ReviewRequestType type, String title, String description, Long requesterId) {}
