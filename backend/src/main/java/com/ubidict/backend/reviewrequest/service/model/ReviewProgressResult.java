package com.ubidict.backend.reviewrequest.service.model;

public record ReviewProgressResult(
        int requiredReviewerCount, int approvedCount, int changesRequestedCount, boolean reviseEligible) {}
