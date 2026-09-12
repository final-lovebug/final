package com.ubidict.backend.reviewrequest.infra.adapter;

import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.infra.DraftDictionaryRepository;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 발행에 실릴 통합 용어 목록을 읽는다(G-1).
 *
 * <p>차기 버전에 남는 것은 <b>등재 승인된 신규 후보어와 이전 버전에서 유지되는 항목</b>이다. 동의어로 편입된 항목은 대표어에 합쳐지므로
 * 별도 용어가 되지 않고, 기각·보류·미처리는 빠진다(D-20·D-21). <b>이 필터는 DI-3이 판정 전이를 구현할 때 확정한다</b> —
 * 그때까지는 상태 이름이 곧 규칙이다.
 */
// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("draftDictionaryQueryAdapterForReviewRequest")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.draft-dictionary.mode", havingValue = "real")
public class DraftDictionaryQueryAdapter implements DraftDictionaryQueryPort {
    private static final Set<CandidateTermStatus> PUBLISHED_STATUSES =
            Set.of(CandidateTermStatus.REGISTRATION_APPROVED, CandidateTermStatus.KEPT);

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
                .findAllByDraftDictionaryIdAndStatusInAndDeletedAtIsNullOrderByFormAsc(
                        draftDictionaryId, PUBLISHED_STATUSES)
                .stream()
                .map(DraftDictionaryQueryAdapter::toSnapshot)
                .toList();
    }

    private static NewTermSnapshot toSnapshot(CandidateTerm candidate) {
        return new NewTermSnapshot(
                candidate.getForm(), candidate.getProposedEnglishName(), candidate.getProposedDefinition());
    }
}
