package com.ubidict.backend.revisionlog.presentation.dto;

import com.ubidict.backend.revisionlog.service.model.RevisionLogDetailResult;
import java.util.List;

public record RevisionLogDetailResponse(RevisionLogResponse revisionLog, List<RevisionLogEntryResponse> entries) {

    public static RevisionLogDetailResponse from(RevisionLogDetailResult result) {
        return new RevisionLogDetailResponse(
                RevisionLogResponse.from(result.revisionLog()),
                result.entries().stream().map(RevisionLogEntryResponse::from).toList());
    }
}
