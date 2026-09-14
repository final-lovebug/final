package com.ubidict.backend.draftdocument.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.draftdocument.domain.*;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;

class SuggestionTermRepositoryTest extends RepositoryTestSupport {
    @Autowired
    SuggestionTermRepository repository;

    @Autowired
    com.ubidict.backend.draftdocument.infra.DraftDocumentRepository drafts;

    @Test
    void findByDraftDocumentIdAndStatus() {
        var d = drafts.save(DraftDocumentFixture.draftDocument().build());
        repository.save(SuggestionTerm.create(d.getId(), new TextRange(0, 1), "a", "b", 1L));
        em.flush();
        assertThat(repository
                        .findAllByDraftDocumentIdAndStatusAndDeletedAtIsNull(
                                d.getId(), SuggestionTermStatus.PENDING, PageRequest.of(0, 20))
                        .getContent())
                .hasSize(1);
    }

    @Test
    void save_persistsAnchor() {
        var d = drafts.save(DraftDocumentFixture.draftDocument().build());
        var t = repository.save(SuggestionTerm.create(d.getId(), new TextRange(2, 4), "a", "b", 1L));
        em.flush();
        em.clear();
        assertThat(repository
                        .findByIdAndDeletedAtIsNull(t.getId())
                        .orElseThrow()
                        .getAnchor())
                .isEqualTo(new TextRange(2, 4));
    }

    @Test
    void search_appliesSort() {
        var d = drafts.save(DraftDocumentFixture.draftDocument().build());
        var a = repository.save(SuggestionTerm.create(d.getId(), new TextRange(0, 1), "a", "b", 1L));
        var b = repository.save(SuggestionTerm.create(d.getId(), new TextRange(1, 2), "c", "d", 1L));
        em.flush();
        var result = repository.findAllByDraftDocumentIdAndDeletedAtIsNull(
                d.getId(), PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")));
        assertThat(result.getContent()).extracting(SuggestionTerm::getId).containsExactly(a.getId(), b.getId());
    }
}
