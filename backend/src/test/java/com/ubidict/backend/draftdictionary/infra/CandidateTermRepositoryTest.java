package com.ubidict.backend.draftdictionary.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.support.RepositoryTestSupport;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

class CandidateTermRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private CandidateTermRepository candidateTermRepository;

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @DisplayName("상태와 표현 필터는 페이징 전에 적용되어 전체 건수가 정확하다.")
    @Test
    void search_filtersBeforePaging() {
        Long draftDictionaryId = saveDraftDictionary();
        candidateTermRepository.save(candidate(draftDictionaryId, "검색 대상", 3));
        candidateTermRepository.save(candidate(draftDictionaryId, "다른 후보", 2));
        em.flush();
        em.clear();

        Page<CandidateTerm> result = candidateTermRepository.search(
                draftDictionaryId, null, "검색", null, PageRequest.of(0, 1, Sort.by("occurrenceCount")));

        assertThat(result.getTotalElements()).isOne();
        assertThat(result.getContent()).extracting(CandidateTerm::getForm).containsExactly("검색 대상");
    }

    @DisplayName("최소 출현 횟수 필터는 출현 횟수가 없는 기존 용어를 안전하게 제외한다.")
    @Test
    void search_minOccurrenceCountExcludesExistingTerm() {
        Long draftDictionaryId = saveDraftDictionary();
        candidateTermRepository.save(candidate(draftDictionaryId, "추출어", 3));
        candidateTermRepository.save(CandidateTerm.createExisting(draftDictionaryId, 100L, "기존어", "기존 정의", null, 2L));
        em.flush();
        em.clear();

        Page<CandidateTerm> result = candidateTermRepository.search(
                draftDictionaryId, null, null, 1, PageRequest.of(0, 20, Sort.by("occurrenceCount")));

        assertThat(result.getContent()).extracting(CandidateTerm::getForm).containsExactly("추출어");
    }

    @DisplayName("후보어 목록의 두 컬렉션을 후보어 건수에 비례한 N+1 없이 조회한다.")
    @Test
    void search_doesNotCauseCollectionNPlusOne() {
        Long draftDictionaryId = saveDraftDictionary();
        candidateTermRepository.save(candidate(draftDictionaryId, "후보어1", 3));
        candidateTermRepository.save(candidate(draftDictionaryId, "후보어2", 2));
        candidateTermRepository.save(candidate(draftDictionaryId, "후보어3", 1));
        em.flush();
        em.clear();
        Statistics statistics =
                entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        Page<CandidateTerm> result = candidateTermRepository.search(
                draftDictionaryId,
                null,
                null,
                null,
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "occurrenceCount")));
        result.getContent().forEach(candidate -> {
            candidate.getOccurredDocumentIds().size();
            candidate.getContextSnippets().size();
        });

        assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(4);
    }

    @DisplayName("기존 용어 후보는 nullable 출현 횟수와 출처 정보를 유지한다.")
    @Test
    void save_existingTerm() {
        Long draftDictionaryId = saveDraftDictionary();
        CandidateTerm saved = candidateTermRepository.save(
                CandidateTerm.createExisting(draftDictionaryId, 100L, "기존어", "기존 정의", "existingTerm", 2L));
        em.flush();
        em.clear();

        CandidateTerm found = candidateTermRepository
                .findByIdAndDeletedAtIsNull(saved.getId())
                .orElseThrow();

        assertThat(found.getOccurrenceCount()).isNull();
        assertThat(found.getSourceTermId()).isEqualTo(100L);
    }

    private Long saveDraftDictionary() {
        DraftDictionary draft = draftDictionaryRepository.save(DraftDictionary.create(1L, null, List.of(10L), 2L));
        return draft.getId();
    }

    private CandidateTerm candidate(Long draftDictionaryId, String form, int occurrenceCount) {
        return CandidateTerm.create(
                draftDictionaryId, form, "정의", null, List.of(10L), occurrenceCount, List.of("문맥"), 2L);
    }
}
