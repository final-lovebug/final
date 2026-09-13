package com.ubidict.backend.reviewrequest.service.model;

import java.util.List;

public record RequestDocumentReviewCommand(
        Long draftDocumentId, String title, String description, List<Long> reviewerMemberIds, Long requesterId) {}
