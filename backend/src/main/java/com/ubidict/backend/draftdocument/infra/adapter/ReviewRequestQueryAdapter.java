package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 문서에 진행 중인 리뷰가 있는지 본다 — 같은 문서에 초안을 두 개 만들지 못하게 하는 생성 정책의 근거다. */
// 빈 이름을 명시한다 — 소비 도메인마다 같은 이름의 포트·어댑터를 각자 정의하므로
// Spring 기본 빈 이름(단순 클래스명)이 전역에서 충돌한다.
@Component("reviewRequestQueryAdapterForDraftDocument")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.review-request.mode", havingValue = "real")
public class ReviewRequestQueryAdapter implements ReviewRequestQueryPort {

    private final ReviewRequestRepository reviewRequestRepository;

    @Override
    public boolean hasOngoingDocumentReview(Long documentId) {
        return reviewRequestRepository.existsOngoingDocumentReview(documentId);
    }
}
