package com.ubidict.backend.reviewrequest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.fixture.DraftDictionaryFixture;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

class DraftDictionaryQueryAdapterTest extends RepositoryTestSupport {

    @Autowired
    private DraftDictionaryRepository draftDictionaryRepository;

    @Autowired
    private CandidateTermRepository candidateTermRepository;

    private DraftDictionaryQueryAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new DraftDictionaryQueryAdapter(draftDictionaryRepository, candidateTermRepository);
    }

    @DisplayName("초안의 워크스페이스와 사전집 식별자를 스냅샷으로 읽는다.")
    @Test
    void read_returnsSnapshot() {
        DraftDictionary draft = draftDictionaryRepository.save(DraftDictionaryFixture.draftDictionary()
                .workspaceId(7L)
                .dictionaryId(100L)
                .build());
        em.flush();
        em.clear();

        Optional<DraftDictionarySnapshot> snapshot = adapter.read(draft.getId());

        assertThat(snapshot)
                .get()
                .extracting(
                        DraftDictionarySnapshot::draftDictionaryId,
                        DraftDictionarySnapshot::workspaceId,
                        DraftDictionarySnapshot::dictionaryId)
                .containsExactly(draft.getId(), 7L, 100L);
    }

    @DisplayName("첫 회차 초안의 사전집 식별자는 null이다.")
    @Test
    void read_firstRoundHasNoDictionary() {
        DraftDictionary draft = draftDictionaryRepository.save(
                DraftDictionaryFixture.draftDictionary().build());
        em.flush();
        em.clear();

        assertThat(adapter.read(draft.getId()))
                .get()
                .extracting(DraftDictionarySnapshot::dictionaryId)
                .isNull();
    }

    @DisplayName("등재 승인과 유지 항목만 통합 목록에 실린다.")
    @Test
    void readFinalTerms_publishedStatusesOnly() {
        Long draftId = saveDraft();
        saveCandidate(draftId, "등재어", CandidateTermStatus.REGISTRATION_APPROVED);
        candidateTermRepository.save(CandidateTerm.createExisting(draftId, 100L, "유지어", "Kept", 2L));
        saveCandidate(draftId, "기각어", CandidateTermStatus.REJECTED);
        saveCandidate(draftId, "미처리어", CandidateTermStatus.PENDING);
        saveCandidate(draftId, "편입어", CandidateTermStatus.MERGED_AS_SYNONYM);
        em.flush();
        em.clear();

        List<NewTermSnapshot> terms = adapter.readFinalTerms(draftId);

        assertThat(terms)
                .extracting(NewTermSnapshot::preferredForm, NewTermSnapshot::englishName)
                .containsExactly(tuple("등재어", "Approved"), tuple("유지어", "Kept"));
    }

    @DisplayName("다른 초안의 후보어는 실리지 않는다.")
    @Test
    void readFinalTerms_otherDraft() {
        Long draftId = saveDraft();
        Long otherDraftId = saveDraft();
        saveCandidate(otherDraftId, "남의 등재어", CandidateTermStatus.REGISTRATION_APPROVED);
        em.flush();
        em.clear();

        assertThat(adapter.readFinalTerms(draftId)).isEmpty();
    }

    private Long saveDraft() {
        return draftDictionaryRepository
                .save(DraftDictionaryFixture.draftDictionary().build())
                .getId();
    }

    private void saveCandidate(Long draftDictionaryId, String form, CandidateTermStatus status) {
        CandidateTerm candidate =
                CandidateTerm.create(draftDictionaryId, form, "정의", "Approved", List.of(10L), 3, List.of("문맥"), 2L);
        ReflectionTestUtils.setField(candidate, "status", status);
        candidateTermRepository.save(candidate);
    }
}
