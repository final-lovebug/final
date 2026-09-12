package com.ubidict.backend.reviewrequest.service.model;

public record ResolveCommentCommand(Long commentId, Long actorId, boolean resolved) {}
