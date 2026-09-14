package com.ubidict.backend.revisionlog.service.model;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import java.time.OffsetDateTime;

public record RevisionLogResult(
        Long revisionLogId,
        Long workspaceId,
        RevisionLogTargetType targetType,
        Long targetId,
        int versionNo,
        Integer previousVersionNo,
        RevisionOrigin origin,
        String summary,
        int addedCount,
        int changedCount,
        int removedCount,
        RevisionLogGrade grade,
        int affectedDocumentCount,
        Integer baseDictionaryVersionNo,
        Long publishedBy,
        OffsetDateTime publishedAt) {

    public static RevisionLogResult from(RevisionLog revisionLog) {
        return new RevisionLogResult(
                revisionLog.getId(),
                revisionLog.getWorkspaceId(),
                revisionLog.getTargetType(),
                revisionLog.getTargetId(),
                revisionLog.getVersionNo(),
                revisionLog.getPreviousVersionNo(),
                revisionLog.getOrigin(),
                revisionLog.getSummary(),
                revisionLog.getAddedCount(),
                revisionLog.getChangedCount(),
                revisionLog.getRemovedCount(),
                revisionLog.getGrade(),
                revisionLog.getAffectedDocumentCount(),
                revisionLog.getBaseDictionaryVersionNo(),
                revisionLog.getPublishedBy(),
                revisionLog.getPublishedAt());
    }
}
