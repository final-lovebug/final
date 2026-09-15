package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 발행에 실릴 통합 용어 목록을 읽는다(G-1).
 *
 * <p><b>초안에 남아 있는 모든 후보어의 대표어가 차기 버전이 된다</b>({@code docs/plan/DRAFT_PLAN.md}). 예전에는 판정 상태가
 * {@code REGISTRATION_APPROVED}·{@code KEPT}인 것만 골랐지만, 초안 화면에서 판정을 걷어내면서 그 상태가 전부
 * {@code PENDING}으로 남게 됐다 — 상태로 거르면 발행 목록이 항상 비어 버린다.
 *
 * <p>대신 <b>초안에 남아 있다는 것이 곧 등재 의사</b>다. 빼고 싶은 후보어는 판정이 아니라 삭제로 뺀다(소프트 삭제라
 * {@code deletedAt}으로 걸러진다). 표기 묶음({@code variantForms})의 비대표 표현은 행이 되지 않는다 — 대표 표기
 * {@code form} 하나만 용어가 된다.
 */
// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("draftDictionaryQueryAdapterForReviewRequest")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "real")
public class DraftDictionaryQueryAdapter implements DraftDictionaryQueryPort {

    private final DraftDictionaryRepository draftDictionaryRepository;
    private final CandidateTermRepository candidateTermRepository;

    @Override
    public Optional<DraftDictionarySnapshot> read(Long draftDictionaryId) {
        return draftDictionaryRepository
                .findByIdAndDeletedAtIsNull(draftDictionaryId)
                .map(draft ->
                        new DraftDictionarySnapshot(draft.getId(), draft.getWorkspaceId(), draft.getDictionaryId()));
    }

    @Override
    public List<NewTermSnapshot> readFinalTerms(Long draftDictionaryId) {
        return candidateTermRepository
                .findAllByDraftDictionaryIdAndDeletedAtIsNullOrderByFormAsc(draftDictionaryId)
                .stream()
                .map(DraftDictionaryQueryAdapter::toSnapshot)
                .toList();
    }

    private static NewTermSnapshot toSnapshot(CandidateTerm candidate) {
        return new NewTermSnapshot(
                candidate.getForm(), candidate.getProposedEnglishName(), candidate.getProposedDefinition());
    }
}
