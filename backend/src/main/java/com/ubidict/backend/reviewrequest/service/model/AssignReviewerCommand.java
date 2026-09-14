package com.ubidict.backend.reviewrequest.service.model;

public record AssignReviewerCommand(Long reviewRequestId, Long memberId, Long actorId) {}
