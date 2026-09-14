package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;
import com.ubidict.backend.revisionlog.exception.RevisionLogErrorCode;
import com.ubidict.backend.revisionlog.infra.RevisionLogEntryRepository;
import com.ubidict.backend.revisionlog.infra.RevisionLogRepository;
import com.ubidict.backend.revisionlog.service.model.RevisionLogSearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionLogReader {

    private final RevisionLogRepository revisionLogRepository;
    private final RevisionLogEntryRepository revisionLogEntryRepository;

    public PageResult<RevisionLog> read(RevisionLogSearchQuery query) {
        Page<RevisionLog> page = query.targetId() == null
                ? revisionLogRepository.findAllByWorkspaceIdAndTargetType(
                        query.workspaceId(), query.targetType(), query.toPageable())
                : revisionLogRepository.findAllByWorkspaceIdAndTargetTypeAndTargetId(
                        query.workspaceId(), query.targetType(), query.targetId(), query.toPageable());
        return new PageResult<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }

    public RevisionLog read(Long workspaceId, Long revisionLogId) {
        RevisionLog revisionLog = revisionLogRepository
                .findById(revisionLogId)
                .orElseThrow(() -> new BusinessException(RevisionLogErrorCode.REVISION_LOG_NOT_FOUND));
        if (!revisionLog.getWorkspaceId().equals(workspaceId)) {
            throw new BusinessException(RevisionLogErrorCode.REVISION_LOG_NOT_FOUND);
        }
        return revisionLog;
    }

    public List<RevisionLogEntry> readEntries(Long revisionLogId) {
        return revisionLogEntryRepository.findAllByRevisionLogId(revisionLogId);
    }
}
