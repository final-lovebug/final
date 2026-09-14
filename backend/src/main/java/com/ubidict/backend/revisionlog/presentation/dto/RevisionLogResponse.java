package com.ubidict.backend.revisionlog.presentation.dto;

import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.domain.RevisionLogTargetType;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import com.ubidict.backend.revisionlog.service.model.RevisionLogResult;
import java.time.OffsetDateTime;

public record RevisionLogResponse(
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

    public static RevisionLogResponse from(RevisionLogResult result) {
        return new RevisionLogResponse(
                result.revisionLogId(),
                result.workspaceId(),
                result.targetType(),
                result.targetId(),
                result.versionNo(),
                result.previousVersionNo(),
                result.origin(),
                result.summary(),
                result.addedCount(),
                result.changedCount(),
                result.removedCount(),
                result.grade(),
                result.affectedDocumentCount(),
                result.baseDictionaryVersionNo(),
                result.publishedBy(),
                result.publishedAt());
    }
}
