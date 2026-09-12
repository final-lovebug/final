package com.ubidict.backend.reviewrequest.infra.port;

import java.util.List;
import java.util.Optional;

/**
 * 발행할 사전 개정안의 근거가 되는 초안을 읽는다(RR-4b).
 *
 * <p>{@code readFinalTerms}가 G-1의 통합 목록을 돌려준다 — 신규 후보어만이 아니라 차기 버전의 전체 용어다.
 */
public interface DraftDictionaryQueryPort {
    Optional<DraftDictionarySnapshot> read(Long draftDictionaryId);

    List<NewTermSnapshot> readFinalTerms(Long draftDictionaryId);
}
