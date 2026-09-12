package com.ubidict.backend.draftdictionary.presentation.dto;

import com.ubidict.backend.draftdictionary.service.model.ExamineProgressResult;

public record ExamineProgressResponse(
        long total, long pending, long kept, long approved, long merged, long rejected, long onHold) {
    public static ExamineProgressResponse from(ExamineProgressResult r) {
        return new ExamineProgressResponse(
                r.total(), r.pending(), r.kept(), r.approved(), r.merged(), r.rejected(), r.onHold());
    }
}
