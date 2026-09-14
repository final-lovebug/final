package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.service.DraftDocumentService;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentReviewReadinessPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 리뷰 요청 자격 판정을 이 도메인의 service에 위임한다(D-44).
 *
 * <p>어댑터가 제공 도메인에 있고 자기 도메인의 service를 참조하는 것은 발행 위임 어댑터와 같은 형태다(D-33). 판정 규칙과 그 ErrorCode가 이 도메인의 것이라
 * 소비 도메인에 두면 리포지토리만 보고 규칙을 다시 구현하게 된다.
 *
 * <p>제공 도메인이 이미 존재하므로 스텁과 {@code @ConditionalOnProperty}를 두지 않는다.
 */
@Component
@RequiredArgsConstructor
public class DraftDocumentReviewReadinessAdapter implements DraftDocumentReviewReadinessPort {

    private final DraftDocumentService draftDocumentService;

    @Override
    public void validateReviewReady(Long draftDocumentId) {
        draftDocumentService.validateReviewReadiness(draftDocumentId);
    }
}
