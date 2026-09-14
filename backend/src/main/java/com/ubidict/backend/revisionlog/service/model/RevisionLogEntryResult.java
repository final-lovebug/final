package com.ubidict.backend.revisionlog.service.model;

import com.ubidict.backend.revisionlog.domain.RevisionLogChangeType;
import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;

public record RevisionLogEntryResult(
        RevisionLogChangeType changeType,
        String subject,
        String subjectEnglishName,
        String replacement,
        String detail) {

    public static RevisionLogEntryResult from(RevisionLogEntry entry) {
        return new RevisionLogEntryResult(
                entry.getChangeType(),
                entry.getSubject(),
                entry.getSubjectEnglishName(),
                entry.getReplacement(),
                entry.getDetail());
    }
}
