package com.ubidict.backend.draftdocument.presentation.dto;

import com.ubidict.backend.draftdocument.service.model.ExamineProgressResult;

public record ExamineProgressResponse(
        long total, long pending, long keptOrigin, long appliedSuggestion, String previewBody) {

    public static ExamineProgressResponse from(ExamineProgressResult result) {
        return new ExamineProgressResponse(
                result.total(),
                result.pending(),
                result.keptOrigin(),
                result.appliedSuggestion(),
                result.previewBody());
    }
}
