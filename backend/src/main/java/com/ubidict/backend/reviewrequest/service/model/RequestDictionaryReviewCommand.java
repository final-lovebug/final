package com.ubidict.backend.reviewrequest.service.model;

import java.util.List;

public record RequestDictionaryReviewCommand(
        Long draftDictionaryId, String title, String description, List<Long> reviewerMemberIds, Long requesterId) {}
