package com.ubidict.backend.reviewrequest.service.model;

public record CancelReviewRequestCommand(Long reviewRequestId, Long actorId) {}
