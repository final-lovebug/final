package com.ubidict.backend.draftdocument.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdocument.domain.DraftDocument;
import com.ubidict.backend.draftdocument.fixture.DraftDocumentFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDocumentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private DraftDocumentRepository draftDocumentRepository;

    @DisplayName("문서 초안을 저장하면 생성 시각과 수정 시각이 채워진다.")
    @Test
    void save_auditingFieldsAreSet() {
        // when
        DraftDocument saved = draftDocumentRepository.save(
                DraftDocumentFixture.draftDocument().build());
        em.flush();

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @DisplayName("원본 문서 식별자로 삭제되지 않은 초안을 조회한다.")
    @Test
    void findByDocumentId() {
        // given
        DraftDocument expected = draftDocumentRepository.save(
                DraftDocumentFixture.draftDocument().documentId(10L).build());
        draftDocumentRepository.save(
                DraftDocumentFixture.draftDocument().documentId(20L).build());
        em.flush();
        em.clear();

        // when
        List<DraftDocument> found =
                draftDocumentRepository.findAllByDocumentIdAndDeletedAtIsNullOrderByCreatedAtDesc(10L);

        // then
        assertThat(found).extracting(DraftDocument::getId).containsExactly(expected.getId());
    }

    @DisplayName("삭제된 초안은 조회되지 않는다.")
    @Test
    void findById_deleted() {
        // given
        DraftDocument draftDocument = draftDocumentRepository.save(
                DraftDocumentFixture.draftDocument().build());
        draftDocument.delete();
        em.flush();
        em.clear();

        // when
        Optional<DraftDocument> found = draftDocumentRepository.findByIdAndDeletedAtIsNull(draftDocument.getId());

        // then
        assertThat(found).isEmpty();
    }
}
