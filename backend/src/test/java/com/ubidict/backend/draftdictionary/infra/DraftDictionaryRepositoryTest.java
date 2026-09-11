package com.ubidict.backend.draftdictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DraftDictionaryRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @DisplayName("대상 사전집 식별자로 삭제되지 않은 사전 초안을 조회한다.")
    @Test
    void findByDictionaryId() {
        DraftDictionary expected = draftDictionaryRepository.save(
                DraftDictionaryFixture.draftDictionary().dictionaryId(10L).build());
        draftDictionaryRepository.save(
                DraftDictionaryFixture.draftDictionary().dictionaryId(20L).build());
        em.flush();
        em.clear();

        Optional<DraftDictionary> found = draftDictionaryRepository.findByDictionaryIdAndDeletedAtIsNull(10L);

        assertThat(found).get().extracting(DraftDictionary::getId).isEqualTo(expected.getId());
    }

    @DisplayName("유래 문서 목록이 사전 초안과 함께 저장된다.")
    @Test
    void save_persistsSourceDocumentIds() {
        DraftDictionary saved = draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .sourceDocumentIds(List.of(10L, 20L))
                .build());
        em.flush();
        em.clear();

        DraftDictionary found = draftDictionaryRepository
                .findByIdAndDeletedAtIsNull(saved.getId())
                .orElseThrow();

        assertThat(found.getSourceDocumentIds()).containsExactlyInAnyOrder(10L, 20L);
    }

    @DisplayName("사전 초안을 저장하면 생성 시각과 수정 시각이 채워진다.")
    @Test
    void save_auditingFieldsAreSet() {
        DraftDictionary saved = draftDictionaryRepository.save(
                DraftDictionaryFixture.draftDictionary().build());
        em.flush();

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @DisplayName("삭제된 사전 초안은 조회되지 않는다.")
    @Test
    void findById_deleted() {
        DraftDictionary draftDictionary = draftDictionaryRepository.save(
                DraftDictionaryFixture.draftDictionary().build());
        draftDictionary.delete();
        em.flush();
        em.clear();

        Optional<DraftDictionary> found = draftDictionaryRepository.findByIdAndDeletedAtIsNull(draftDictionary.getId());

        assertThat(found).isEmpty();
    }
}
