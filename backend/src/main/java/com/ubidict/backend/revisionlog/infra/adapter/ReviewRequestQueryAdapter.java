package com.ubidict.backend.revisionlog.infra.adapter;

import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.ReviseRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import com.ubidict.backend.revisionlog.infra.port.DocumentRevisionSnapshot;
import com.ubidict.backend.revisionlog.infra.port.ReviewRequestQueryPort;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 문서 개정 이력이 필요한 최소한의 반영 결과만 리뷰 요청 저장소에서 읽는다. */
@Component("reviewRequestQueryAdapterForRevisionLog")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.review-request.mode", havingValue = "real")
public class ReviewRequestQueryAdapter implements ReviewRequestQueryPort {

    private final RevisionDocumentRepository revisionDocumentRepository;
    private final ReviewRequestRepository reviewRequestRepository;
    private final ReviseRepository reviseRepository;

    @Override
    public Optional<DocumentRevisionSnapshot> findDocumentRevision(Long reviewRequestId) {
        return revisionDocumentRepository
                .findTopByReviewRequestIdOrderByReexamineRoundDesc(reviewRequestId)
                .filter(revision -> revision.getResultVersionNo() != null)
                .flatMap(revision -> reviewRequestRepository
                        .findById(reviewRequestId)
                        .flatMap(request -> reviseRepository
                                .findByReviewRequestId(reviewRequestId)
                                .map(revise -> new DocumentRevisionSnapshot(
                                        request.getWorkspaceId(),
                                        revision.getDocumentId(),
                                        revision.getDraftDocumentId(),
                                        revision.getResultVersionNo(),
                                        revise.getPerformedBy()))));
    }
}
