package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;

public record SubmitReviewCommand(Long reviewRequestId, Long memberId, int targetRound, ReviewVerdict verdict) {}
