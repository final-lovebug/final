package com.ubidict.backend.reviewrequest.service.model;

import com.ubidict.backend.common.domain.TextRange;

public record AddCommentCommand(
        Long reviewId, Long authorId, String content, TextRange anchor, Long targetItemId, Long parentId) {}
