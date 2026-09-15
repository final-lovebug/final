package com.ubidict.backend.revisionlog.service;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.revisionlog.implement.DictionaryDiffCalculator;
import com.ubidict.backend.revisionlog.implement.DictionaryGradeDecider;
import com.ubidict.backend.revisionlog.implement.DocumentImpactCounter;
import com.ubidict.backend.revisionlog.implement.DocumentRevisionAssembler;
import com.ubidict.backend.revisionlog.implement.RevisionLogAppender;
import com.ubidict.backend.revisionlog.implement.RevisionSummaryFactory;
import com.ubidict.backend.revisionlog.infra.port.DictionaryTermQueryPort;
import com.ubidict.backend.revisionlog.infra.port.DocumentRevisionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.ReviewRequestQueryPort;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RevisionLogEventHandlerDocumentTest {

    @Mock
    private DictionaryTermQueryPort dictionaryTermQueryPort;

    @Mock
    private DictionaryDiffCalculator dictionaryDiffCalculator;

    @Mock
    private DictionaryGradeDecider dictionaryGradeDecider;

    @Mock
    private DocumentImpactCounter documentImpactCounter;

    @Mock
    private RevisionSummaryFactory revisionSummaryFactory;

    @Mock
    private RevisionLogAppender revisionLogAppender;

    @Mock
    private DocumentRevisionAssembler documentRevisionAssembler;

    @Mock
    private ReviewRequestQueryPort reviewRequestQueryPort;

    @DisplayName("문서 직접 편집 이벤트는 문서 개정 이력 조립기로 전달한다.")
    @Test
    void handle_documentEdited() {
        DocumentEditedEvent event = new DocumentEditedEvent(20L, 10L, 2, OffsetDateTime.now());

        handler().handle(event);

        verify(documentRevisionAssembler).appendDirectEdit(10L, 20L, 2);
    }

    @DisplayName("문서 리뷰 반영 이벤트는 조회한 개정안으로 문서 개정 이력을 만든다.")
    @Test
    void handle_documentReviewRevised() {
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(40L, ReviewRequestType.DOCUMENT, 30L, 2, OffsetDateTime.now());
        DocumentRevisionSnapshot revision = new DocumentRevisionSnapshot(10L, 20L, 30L, 2, 7L);
        given(reviewRequestQueryPort.findDocumentRevision(40L)).willReturn(Optional.of(revision));

        handler().handle(event);

        verify(documentRevisionAssembler).appendReviewRevision(10L, revision);
    }

    @DisplayName("사전집 리뷰 반영 이벤트는 문서 개정 이력을 만들지 않는다.")
    @Test
    void handle_dictionaryReviewRevised_skipsDocumentLog() {
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(40L, ReviewRequestType.DICTIONARY, 30L, 2, OffsetDateTime.now());

        handler().handle(event);

        verifyNoInteractions(reviewRequestQueryPort, documentRevisionAssembler);
    }

    private RevisionLogEventHandler handler() {
        return new RevisionLogEventHandler(
                dictionaryTermQueryPort,
                dictionaryDiffCalculator,
                dictionaryGradeDecider,
                documentImpactCounter,
                revisionSummaryFactory,
                revisionLogAppender,
                documentRevisionAssembler,
                reviewRequestQueryPort);
    }
}
