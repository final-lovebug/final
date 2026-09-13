package com.ubidict.backend.revisionlog.service;

import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.implement.DictionaryDiff;
import com.ubidict.backend.revisionlog.implement.DictionaryDiffCalculator;
import com.ubidict.backend.revisionlog.implement.DictionaryGradeDecider;
import com.ubidict.backend.revisionlog.implement.DocumentImpactCounter;
import com.ubidict.backend.revisionlog.implement.DocumentRevisionAssembler;
import com.ubidict.backend.revisionlog.implement.RevisionLogAppender;
import com.ubidict.backend.revisionlog.implement.RevisionSummaryFactory;
import com.ubidict.backend.revisionlog.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.revisionlog.infra.port.DictionaryVersionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.revisionlog.infra.port.TermSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 수신 기술과 무관하게 사전집 발행 이벤트를 개정 이력으로 바꾸는 공용 핸들러. */
@Slf4j
@Service
@RequiredArgsConstructor
public class RevisionLogEventHandler {

    private final DictionaryTermQueryPort dictionaryTermQueryPort;
    private final DictionaryDiffCalculator dictionaryDiffCalculator;
    private final DictionaryGradeDecider dictionaryGradeDecider;
    private final DocumentImpactCounter documentImpactCounter;
    private final RevisionSummaryFactory revisionSummaryFactory;
    private final RevisionLogAppender revisionLogAppender;
    private final DocumentRevisionAssembler documentRevisionAssembler;
    private final ReviewRequestQueryPort reviewRequestQueryPort;

    @Transactional
    public void handle(DictionaryRevisedEvent event) {
        dictionaryTermQueryPort
                .readVersion(event.workspaceId(), event.versionNo())
                .ifPresentOrElse(
                        snapshot -> appendDictionaryRevision(event, snapshot),
                        () -> log.warn(
                                "[RevisionLogEventHandler.handle] Dictionary version not found. workspaceId={}, versionNo={}",
                                event.workspaceId(),
                                event.versionNo()));
    }

    @Transactional
    public void handle(DocumentEditedEvent event) {
        documentRevisionAssembler.appendDirectEdit(event.workspaceId(), event.documentId(), event.versionNo());
    }

    @Transactional
    public void handle(ReviewRequestRevisedEvent event) {
        if (event.type() != ReviewRequestType.DOCUMENT) {
            return;
        }

        reviewRequestQueryPort
                .findDocumentRevision(event.reviewRequestId())
                .ifPresentOrElse(
                        revision -> documentRevisionAssembler.appendReviewRevision(revision.workspaceId(), revision),
                        () -> log.warn(
                                "[RevisionLogEventHandler.handle] Document revision not found. reviewRequestId={}",
                                event.reviewRequestId()));
    }

    private void appendDictionaryRevision(DictionaryRevisedEvent event, DictionaryVersionSnapshot current) {
        Integer previousVersionNo = event.versionNo() == 1 ? null : event.versionNo() - 1;
        List<TermSnapshot> previousTerms = readPreviousTerms(event.workspaceId(), previousVersionNo);
        if (previousVersionNo != null && previousTerms == null) {
            return;
        }

        DictionaryDiff diff = dictionaryDiffCalculator.calculate(
                previousTerms == null ? List.of() : previousTerms,
                dictionaryTermQueryPort.readTerms(current.dictionaryId()));
        RevisionLogGrade grade = dictionaryGradeDecider.decide(previousVersionNo, diff);
        int affectedDocumentCount = documentImpactCounter.count(event.workspaceId(), event.versionNo(), grade);
        RevisionLog revisionLog = RevisionLog.forDictionary(
                event.workspaceId(),
                current.dictionaryId(),
                event.versionNo(),
                previousVersionNo,
                grade,
                diff.addedCount(),
                diff.changedCount(),
                diff.removedCount(),
                affectedDocumentCount,
                revisionSummaryFactory.forDictionary(grade, diff),
                current.publishedBy(),
                current.publishedAt());
        revisionLogAppender.appendDictionary(revisionLog, diff.changes());
    }

    private List<TermSnapshot> readPreviousTerms(Long workspaceId, Integer previousVersionNo) {
        if (previousVersionNo == null) {
            return null;
        }

        return dictionaryTermQueryPort
                .findDictionaryIdByVersion(workspaceId, previousVersionNo)
                .map(dictionaryTermQueryPort::readTerms)
                .orElseGet(() -> {
                    log.warn(
                            "[RevisionLogEventHandler.readPreviousTerms] Previous dictionary version not found. workspaceId={}, versionNo={}",
                            workspaceId,
                            previousVersionNo);
                    return null;
                });
    }
}
