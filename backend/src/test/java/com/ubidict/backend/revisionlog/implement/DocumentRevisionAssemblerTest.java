package com.ubidict.backend.revisionlog.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.revisionlog.domain.RevisionLog;
import com.ubidict.backend.revisionlog.domain.RevisionLogEntry;
import com.ubidict.backend.revisionlog.domain.RevisionOrigin;
import com.ubidict.backend.revisionlog.infra.port.AppliedSuggestion;
import com.ubidict.backend.revisionlog.infra.port.DocumentQueryPort;
import com.ubidict.backend.revisionlog.infra.port.DocumentRevisionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.DocumentVersionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.DraftDocumentQueryPort;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentRevisionAssemblerTest {

    @Mock
    private DocumentQueryPort documentQueryPort;

    @Mock
    private DraftDocumentQueryPort draftDocumentQueryPort;

    @Mock
    private RevisionLogAppender revisionLogAppender;

    @Captor
    private ArgumentCaptor<RevisionLog> revisionLogCaptor;

    @Captor
    private ArgumentCaptor<List<RevisionLogEntry>> entriesCaptor;

    @DisplayName("직접 편집하면 v1 업로드 이력을 백필하고 항목 없는 직접 편집 이력을 만든다.")
    @Test
    void appendDirectEdit_backfillsInitialAndCreatesEntrylessLog() {
        // given
        given(documentQueryPort.readVersions(20L)).willReturn(List.of(version(2, true), version(1, false)));
        DocumentRevisionAssembler assembler = assembler();

        // when
        assembler.appendDirectEdit(10L, 20L, 2);

        // then
        verify(revisionLogAppender, org.mockito.Mockito.times(2))
                .append(revisionLogCaptor.capture(), entriesCaptor.capture());
        assertThat(revisionLogCaptor.getAllValues())
                .extracting(RevisionLog::getOrigin)
                .containsExactly(RevisionOrigin.UPLOAD, RevisionOrigin.DIRECT_EDIT);
        assertThat(entriesCaptor.getAllValues())
                .allSatisfy(entries -> assertThat(entries).isEmpty());
    }

    @DisplayName("리뷰 반영은 적용된 치환만 항목으로 만들고 v1 이력을 백필한다.")
    @Test
    void appendReviewRevision_recordsOnlyAppliedSuggestions() {
        // given
        given(documentQueryPort.readVersions(20L)).willReturn(List.of(version(2, false), version(1, false)));
        given(draftDocumentQueryPort.readAppliedSuggestions(30L))
                .willReturn(List.of(new AppliedSuggestion("회원", "사용자"), new AppliedSuggestion("로그인", "인증")));
        DocumentRevisionAssembler assembler = assembler();

        // when
        assembler.appendReviewRevision(10L, new DocumentRevisionSnapshot(10L, 20L, 30L, 2, 7L));

        // then
        verify(revisionLogAppender, org.mockito.Mockito.times(2))
                .append(revisionLogCaptor.capture(), entriesCaptor.capture());
        assertThat(revisionLogCaptor.getAllValues().get(1)).satisfies(log -> {
            assertThat(log.getOrigin()).isEqualTo(RevisionOrigin.REVIEW_REVISE);
            assertThat(log.getChangedCount()).isEqualTo(2);
        });
        assertThat(entriesCaptor.getAllValues().get(1))
                .extracting(RevisionLogEntry::getSubject, RevisionLogEntry::getReplacement)
                .containsExactly(tuple("회원", "사용자"), tuple("로그인", "인증"));
    }

    private DocumentRevisionAssembler assembler() {
        return new DocumentRevisionAssembler(
                documentQueryPort, draftDocumentQueryPort, new RevisionSummaryFactory(), revisionLogAppender);
    }

    private static DocumentVersionSnapshot version(int versionNo, boolean edited) {
        return new DocumentVersionSnapshot(
                versionNo, versionNo == 1 ? null : 3, edited, OffsetDateTime.parse("2026-09-13T12:00:00Z"), 7L);
    }

    private static org.assertj.core.groups.Tuple tuple(Object... values) {
        return org.assertj.core.groups.Tuple.tuple(values);
    }
}
