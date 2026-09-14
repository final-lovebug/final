package com.ubidict.backend.reviewrequest.infra;

/** {@code ReviewerRepository.countByReviewRequestIdIn}의 그룹 집계 결과(T-INT-12, D-63). */
public record ReviewerCount(Long reviewRequestId, long count) {}
