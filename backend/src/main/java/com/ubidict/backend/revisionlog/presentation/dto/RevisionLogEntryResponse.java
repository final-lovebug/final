package com.ubidict.backend.revisionlog.presentation.dto;

import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.service.model.RevisionLogEntryResult;

public record RevisionLogEntryResponse(
        RevisionLogChangeType changeType,
        String subject,
        String subjectEnglishName,
        String replacement,
        String detail) {

    public static RevisionLogEntryResponse from(RevisionLogEntryResult result) {
        return new RevisionLogEntryResponse(
                result.changeType(),
                result.subject(),
                result.subjectEnglishName(),
                result.replacement(),
                result.detail());
    }
}
