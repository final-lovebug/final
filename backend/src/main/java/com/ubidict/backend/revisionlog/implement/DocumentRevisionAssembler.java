package com.ubidict.backend.revisionlog.implement;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import com.ubidict.backend.revisionlog.infra.port.DocumentQueryPort;
import com.ubidict.backend.revisionlog.infra.port.DocumentRevisionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.DocumentVersionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.DraftDocumentQueryPort;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** 문서의 세 확정 경로를 불변 개정 이력으로 조립하고, 누락된 v1 이력을 함께 보정한다. */
@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentRevisionAssembler {

    private final DocumentQueryPort documentQueryPort;
    private final DraftDocumentQueryPort draftDocumentQueryPort;
    private final RevisionSummaryFactory revisionSummaryFactory;
    private final RevisionLogAppender revisionLogAppender;

    public void appendDirectEdit(Long workspaceId, Long documentId, int versionNo) {
        findVersion(documentId, versionNo).ifPresent(version -> {
            appendInitialIfPresent(workspaceId, documentId);
            append(workspaceId, documentId, version, RevisionOrigin.DIRECT_EDIT, List.of(), version.publishedBy());
        });
    }

    public void appendReviewRevision(Long workspaceId, DocumentRevisionSnapshot revision) {
        findVersion(revision.documentId(), revision.resultVersionNo()).ifPresent(version -> {
            appendInitialIfPresent(workspaceId, revision.documentId());
            List<RevisionLogEntry> entries =
                    draftDocumentQueryPort.readAppliedSuggestions(revision.draftDocumentId()).stream()
                            .map(suggestion -> RevisionLogEntry.replacement(
                                    null, suggestion.originTerm(), suggestion.suggestionTerm()))
                            .toList();
            append(
                    workspaceId,
                    revision.documentId(),
                    version,
                    RevisionOrigin.REVIEW_REVISE,
                    entries,
                    revision.performedBy());
        });
    }

    private void appendInitialIfPresent(Long workspaceId, Long documentId) {
        findVersion(documentId, 1)
                .ifPresent(version -> append(
                        workspaceId, documentId, version, RevisionOrigin.UPLOAD, List.of(), version.publishedBy()));
    }

    private void append(
            Long workspaceId,
            Long documentId,
            DocumentVersionSnapshot version,
            RevisionOrigin origin,
            List<RevisionLogEntry> entries,
            Long publishedBy) {
        int changedCount = entries.size();
        RevisionLog revisionLog = RevisionLog.forDocument(
                workspaceId,
                documentId,
                version.versionNo(),
                version.versionNo() == 1 ? null : version.versionNo() - 1,
                origin,
                version.dictionaryVersionNo(),
                changedCount,
                revisionSummaryFactory.forDocument(origin, changedCount),
                publishedBy,
                version.publishedAt());
        revisionLogAppender.append(revisionLog, entries);
    }

    private Optional<DocumentVersionSnapshot> findVersion(Long documentId, int versionNo) {
        Optional<DocumentVersionSnapshot> version = documentQueryPort.readVersions(documentId).stream()
                .filter(candidate -> candidate.versionNo() == versionNo)
                .findFirst();
        if (version.isEmpty()) {
            log.warn(
                    "[DocumentRevisionAssembler.findVersion] Document version not found. documentId={}, versionNo={}",
                    documentId,
                    versionNo);
        }
        return version;
    }
}
