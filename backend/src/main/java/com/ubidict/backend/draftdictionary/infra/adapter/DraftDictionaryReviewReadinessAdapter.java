package com.ubidict.backend.draftdictionary.infra.adapter;

import com.ubidict.backend.draftdictionary.service.DraftDictionaryService;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryReviewReadinessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리뷰 요청 자격 판정을 이 도메인의 service에 위임한다(D-44).
 *
 * <p>배치 근거는 {@code DraftDocumentReviewReadinessAdapter}와 같다 — 「교정 완료」·「활성 사전집과 달라진 항목 1건 이상」·「등재 대상의 정의
 * 필수」가 모두 이 도메인의 규칙이다(D-21·Y-29).
 */
@Component
@RequiredArgsConstructor
public class DraftDictionaryReviewReadinessAdapter implements DraftDictionaryReviewReadinessPort {

    private final DraftDictionaryService draftDictionaryService;

    @Override
    public void validateReviewReady(Long draftDictionaryId) {
        draftDictionaryService.validateReviewReadiness(draftDictionaryId);
    }
}
