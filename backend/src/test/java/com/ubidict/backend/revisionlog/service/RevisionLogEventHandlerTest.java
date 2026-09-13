package com.ubidict.backend.revisionlog.service;

import static com.ubidict.backend.dictionary.fixture.DictionaryFixture.dictionary;
import static com.ubidict.backend.dictionary.fixture.TermFixture.term;
import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.dictionary.infra.DictionaryRepository;
import com.ubidict.backend.dictionary.infra.TermRepository;
import com.ubidict.backend.revisionlog.domain.RevisionLogGrade;
import com.ubidict.backend.revisionlog.infra.RevisionLogEntryRepository;
import com.ubidict.backend.revisionlog.infra.RevisionLogRepository;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.fixture.WorkspaceFixture;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevisionLogEventHandlerTest extends IntegrationTestSupport {

    @Autowired
    private RevisionLogEventHandler handler;

    @Autowired
    private DictionaryRepository dictionaryRepository;

    @Autowired
    private TermRepository termRepository;

    @Autowired
    private RevisionLogRepository revisionLogRepository;

    @Autowired
    private RevisionLogEntryRepository revisionLogEntryRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @DisplayName("같은 사전집 발행 이벤트를 두 번 처리해도 개정 이력은 한 건이다.")
    @Test
    void handle_isIdempotent() {
        // given
        Long workspaceId = workspaceRepository
                .save(WorkspaceFixture.workspace().createdBy(10L).build())
                .getId();
        Dictionary previous = dictionaryRepository.save(dictionary()
                .workspaceId(workspaceId)
                .createdBy(10L)
                .status(com.ubidict.backend.dictionary.domain.DictionaryStatus.ARCHIVED)
                .build());
        Dictionary current = dictionaryRepository.save(dictionary()
                .workspaceId(workspaceId)
                .createdBy(20L)
                .versionNo(2)
                .build());
        termRepository.save(
                term().dictionaryId(previous.getId()).preferredForm("이용자").build());
        termRepository.save(
                term().dictionaryId(current.getId()).preferredForm("사용자").build());
        DictionaryRevisedEvent event = new DictionaryRevisedEvent(
                workspaceId, current.getId(), 2, OffsetDateTime.parse("2026-09-13T12:00:00Z"));

        // when
        handler.handle(event);
        handler.handle(event);

        // then
        assertThat(revisionLogRepository.findAll()).singleElement().satisfies(log -> {
            assertThat(log.getGrade()).isEqualTo(RevisionLogGrade.RECHECK_REQUIRED);
            assertThat(log.getPublishedBy()).isEqualTo(20L);
            assertThat(log.getAddedCount()).isEqualTo(1);
            assertThat(log.getRemovedCount()).isEqualTo(1);
        });
        assertThat(revisionLogEntryRepository.findAll()).hasSize(2);
    }
}
