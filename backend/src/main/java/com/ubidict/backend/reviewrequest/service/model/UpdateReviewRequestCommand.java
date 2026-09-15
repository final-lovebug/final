package com.ubidict.backend.reviewrequest.service.model;

public record UpdateReviewRequestCommand(Long reviewRequestId, String title, String description, Long actorId) {}
