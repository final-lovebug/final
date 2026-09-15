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

    @DisplayName("초안에 남아 있는 모든 후보어가 판정 상태와 무관하게 통합 목록에 실린다.")
    @Test
    void readFinalTerms_allRemainingCandidates() {
        // docs/plan/DRAFT_PLAN.md — 초안 화면에서 판정을 걷어냈으므로 상태로 거르지 않는다.
        // 예전 규칙(REGISTRATION_APPROVED·KEPT만)을 그대로 두면 새 후보어가 전부 PENDING 이라
        // 발행 목록이 항상 비어 버린다.
        Long draftId = saveDraft();
        saveCandidate(draftId, "등재어", CandidateTermStatus.REGISTRATION_APPROVED);
        candidateTermRepository.save(CandidateTerm.createExisting(draftId, 100L, "유지어", "유지 정의", "Kept", 2L));
        saveCandidate(draftId, "미처리어", CandidateTermStatus.PENDING);
        em.flush();
        em.clear();

        List<NewTermSnapshot> terms = adapter.readFinalTerms(draftId);

        assertThat(terms)
                .extracting(NewTermSnapshot::preferredForm, NewTermSnapshot::englishName, NewTermSnapshot::definition)
                .containsExactly(
                        tuple("등재어", "Approved", "정의"), tuple("미처리어", "Approved", "정의"), tuple("유지어", "Kept", "유지 정의"));
    }

    @DisplayName("삭제한 후보어는 통합 목록에서 빠진다.")
    @Test
    void readFinalTerms_excludesDeleted() {
        // 빼고 싶은 후보어는 판정이 아니라 삭제로 뺀다(소프트 삭제).
        Long draftId = saveDraft();
        saveCandidate(draftId, "남길어", CandidateTermStatus.PENDING);
        CandidateTerm removed = saveCandidateEntity(draftId, "지울어", CandidateTermStatus.PENDING);
        removed.delete();
        em.flush();
        em.clear();

        assertThat(adapter.readFinalTerms(draftId))
                .extracting(NewTermSnapshot::preferredForm)
                .containsExactly("남길어");
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
        saveCandidateEntity(draftDictionaryId, form, status);
    }

    private CandidateTerm saveCandidateEntity(Long draftDictionaryId, String form, CandidateTermStatus status) {
        CandidateTerm candidate =
                CandidateTerm.create(draftDictionaryId, form, "정의", "Approved", List.of(10L), 3, List.of("문맥"), 2L);
        ReflectionTestUtils.setField(candidate, "status", status);
        return candidateTermRepository.save(candidate);
    }
}
