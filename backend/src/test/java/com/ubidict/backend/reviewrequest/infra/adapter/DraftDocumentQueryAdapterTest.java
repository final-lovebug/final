package com.ubidict.backend.reviewrequest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.draftdocument.infra.DraftDocumentRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDocumentQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    private DraftDocumentQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DraftDocumentQueryAdapter(draftDocumentRepository);
    }

    @DisplayName("초안의 기준 버전과 본문을 스냅샷으로 읽는다.")
    @Test
    void read_returnsSnapshot() {
        DraftDocument draft = draftDocumentRepository.save(DraftDocumentFixture.draftDocument()
                .documentId(10L)
                .baseVersionNo(3)
                .draftBody("교정한 본문")
                .build());
        em.flush();
        em.clear();

        Optional<DraftDocumentSnapshot> snapshot = adapter.read(draft.getId());

        assertThat(snapshot)
                .get()
                .extracting(
                        DraftDocumentSnapshot::draftDocumentId,
                        DraftDocumentSnapshot::documentId,
                        DraftDocumentSnapshot::baseVersionNo,
                        DraftDocumentSnapshot::draftBody)
                .containsExactly(draft.getId(), 10L, 3, "교정한 본문");
    }

    @DisplayName("삭제된 초안은 읽히지 않는다.")
    @Test
    void read_draftIsDeleted() {
        DraftDocument draft = draftDocumentRepository.save(
                DraftDocumentFixture.draftDocument().build());
        draft.delete();
        em.flush();
        em.clear();

        assertThat(adapter.read(draft.getId())).isEmpty();
    }
}
