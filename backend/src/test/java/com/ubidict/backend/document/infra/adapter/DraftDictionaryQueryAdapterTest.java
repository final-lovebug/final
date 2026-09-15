package com.ubidict.backend.document.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.DraftDictionaryStatus;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * G-15 — 사전 초안이 진행 중이면 그 원천 문서를 편집할 수 없다.
 *
 * <p>유래 문서는 {@code @ElementCollection}이라 join 쿼리로 찾는다. 이전에는 전체를 읽어 메모리에서 걸렀다(Y-30).
 */
class DraftDictionaryQueryAdapterTest extends RepositoryTestSupport {

    private static final Long SOURCE_DOCUMENT_ID = 10L;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    private DraftDictionaryQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DraftDictionaryQueryAdapter(draftDictionaryRepository);
    }

    @DisplayName("진행 중 초안의 유래 문서면 참이다.")
    @Test
    void isSourceOfOngoingDraft_ongoingDraftHasDocument() {
        save(List.of(SOURCE_DOCUMENT_ID, 20L), DraftDictionaryStatus.EXAMINING);
        em.flush();
        em.clear();

        assertThat(adapter.isSourceOfOngoingDraft(SOURCE_DOCUMENT_ID)).isTrue();
    }

    @DisplayName("반영완료 초안의 유래 문서는 보지 않는다.")
    @Test
    void isSourceOfOngoingDraft_draftIsRevised() {
        save(List.of(SOURCE_DOCUMENT_ID), DraftDictionaryStatus.REVISED);
        em.flush();
        em.clear();

        assertThat(adapter.isSourceOfOngoingDraft(SOURCE_DOCUMENT_ID)).isFalse();
    }

    @DisplayName("삭제된 초안의 유래 문서는 보지 않는다.")
    @Test
    void isSourceOfOngoingDraft_draftIsDeleted() {
        Long id = save(List.of(SOURCE_DOCUMENT_ID), DraftDictionaryStatus.EXAMINING);
        draftDictionaryRepository.findById(id).orElseThrow().delete();
        em.flush();
        em.clear();

        assertThat(adapter.isSourceOfOngoingDraft(SOURCE_DOCUMENT_ID)).isFalse();
    }

    @DisplayName("어느 초안의 유래 문서도 아니면 거짓이다.")
    @Test
    void isSourceOfOngoingDraft_documentIsNotSource() {
        save(List.of(20L), DraftDictionaryStatus.EXAMINING);
        em.flush();
        em.clear();

        assertThat(adapter.isSourceOfOngoingDraft(SOURCE_DOCUMENT_ID)).isFalse();
    }

    private Long save(List<Long> sourceDocumentIds, DraftDictionaryStatus status) {
        return draftDictionaryRepository
                .save(DraftDictionaryFixture.draftDictionary()
                        .sourceDocumentIds(sourceDocumentIds)
                        .status(status)
                        .build())
                .getId();
    }
}
