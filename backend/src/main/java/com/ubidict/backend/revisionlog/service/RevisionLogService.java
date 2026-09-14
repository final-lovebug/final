package com.ubidict.backend.revisionlog.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.implement.RevisionLogReader;
import com.ubidict.backend.revisionlog.service.model.RevisionLogDetailResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogEntryResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogResult;
import com.ubidict.backend.revisionlog.service.model.RevisionLogSearchQuery;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 이벤트로 고정된 개정 이력을 참여자에게 읽기 전용으로 제공한다. */
@Service
@RequiredArgsConstructor
public class RevisionLogService {

    private final RevisionLogReader revisionLogReader;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional(readOnly = true)
    public PageResult<RevisionLogResult> search(RevisionLogSearchQuery query) {
        workspaceAccessValidator.validateParticipant(query.workspaceId(), query.memberId());
        return revisionLogReader.read(query).map(RevisionLogResult::from);
    }

    @Transactional(readOnly = true)
    public RevisionLogDetailResult read(Long workspaceId, Long revisionLogId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        RevisionLog revisionLog = revisionLogReader.read(workspaceId, revisionLogId);
        return new RevisionLogDetailResult(
                RevisionLogResult.from(revisionLog),
                revisionLogReader.readEntries(revisionLog.getId()).stream()
                        .map(RevisionLogEntryResult::from)
                        .toList());
    }
}
