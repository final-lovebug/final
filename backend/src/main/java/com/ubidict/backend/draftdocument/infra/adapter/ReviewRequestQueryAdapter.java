package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("reviewRequestQueryAdapterForDraftDocument")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.review-request.mode", havingValue = "real", matchIfMissing = true)
public class ReviewRequestQueryAdapter implements ReviewRequestQueryPort {

    private final ReviewRequestRepository reviewRequestRepository;

    @Override
    public boolean hasOngoingDocumentReview(Long documentId) {
        return reviewRequestRepository.existsOngoingDocumentReview(documentId);
    }
}
