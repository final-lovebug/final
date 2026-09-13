package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;
import com.ubidict.backend.revisionlog.infra.RevisionLogEntryRepository;
import com.ubidict.backend.revisionlog.infra.RevisionLogRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/** 자연 키로 선행 중복을 피하고, 동시 수신은 DB 유니크 제약으로 막는다(D-58). */
@Slf4j
@Component
@RequiredArgsConstructor
public class RevisionLogAppender {

    private final RevisionLogRepository revisionLogRepository;
    private final RevisionLogEntryRepository revisionLogEntryRepository;

    public Optional<RevisionLog> appendDictionary(RevisionLog revisionLog, List<DictionaryTermChange> changes) {
        return append(
                revisionLog,
                changes.stream()
                        .map(change -> RevisionLogEntry.term(
                                null,
                                change.changeType(),
                                change.term().preferredForm(),
                                change.term().englishName(),
                                change.detail()))
                        .toList());
    }

    public Optional<RevisionLog> append(RevisionLog revisionLog, List<RevisionLogEntry> entries) {
        if (revisionLogRepository.existsByWorkspaceIdAndTargetTypeAndTargetIdAndVersionNo(
                revisionLog.getWorkspaceId(),
                revisionLog.getTargetType(),
                revisionLog.getTargetId(),
                revisionLog.getVersionNo())) {
            log.debug(
                    "[RevisionLogAppender.append] Duplicated revision log skipped. targetType={}, targetId={}, versionNo={}",
                    revisionLog.getTargetType(),
                    revisionLog.getTargetId(),
                    revisionLog.getVersionNo());
            return Optional.empty();
        }

        try {
            RevisionLog saved = revisionLogRepository.saveAndFlush(revisionLog);
            revisionLogEntryRepository.saveAll(
                    entries.stream().map(entry -> copyFor(saved.getId(), entry)).toList());
            return Optional.of(saved);
        } catch (DataIntegrityViolationException exception) {
            log.debug(
                    "[RevisionLogAppender.append] Concurrent duplicate rejected by unique constraint. targetType={}, targetId={}, versionNo={}",
                    revisionLog.getTargetType(),
                    revisionLog.getTargetId(),
                    revisionLog.getVersionNo());
            return Optional.empty();
        }
    }

    private static RevisionLogEntry copyFor(Long revisionLogId, RevisionLogEntry entry) {
        if (entry.getReplacement() != null) {
            return RevisionLogEntry.replacement(revisionLogId, entry.getSubject(), entry.getReplacement());
        }
        return RevisionLogEntry.term(
                revisionLogId,
                entry.getChangeType(),
                entry.getSubject(),
                entry.getSubjectEnglishName(),
                entry.getDetail());
    }
}
