package com.ubidict.backend.draftdocument.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDictionaryQueryAdapterTest extends RepositoryTestSupport {

    private static final Long WORKSPACE_ID = 1L;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    private DraftDictionaryQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DraftDictionaryQueryAdapter(draftDictionaryRepository);
    }

    @DisplayName("반영완료가 아닌 초안이 있으면 진행 중으로 본다.")
    @Test
    void hasOngoingDraft_statusIsNotRevised() {
        save(DraftDictionaryStatus.REVIEW_REQUESTED);
        em.flush();
        em.clear();

        assertThat(adapter.hasOngoingDraft(WORKSPACE_ID)).isTrue();
    }

    @DisplayName("반영완료 초안만 있으면 진행 중이 아니다.")
    @Test
    void hasOngoingDraft_statusIsRevised() {
        save(DraftDictionaryStatus.REVISED);
        em.flush();
        em.clear();

        assertThat(adapter.hasOngoingDraft(WORKSPACE_ID)).isFalse();
    }

    @DisplayName("다른 워크스페이스의 초안은 보지 않는다.")
    @Test
    void hasOngoingDraft_otherWorkspace() {
        draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(999L)
                .status(DraftDictionaryStatus.EXAMINING)
                .build());
        em.flush();
        em.clear();

        assertThat(adapter.hasOngoingDraft(WORKSPACE_ID)).isFalse();
    }

    private void save(DraftDictionaryStatus status) {
        draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(WORKSPACE_ID)
                .status(status)
                .build());
    }
}
