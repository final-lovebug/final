package com.ubidict.backend.reviewrequest.service.model;

public record SubmitRevisionCommand(
        Long reviewRequestId, Long targetId, int baseVersionNo, Long draftId, String proposedBody, Long actorId) {}
