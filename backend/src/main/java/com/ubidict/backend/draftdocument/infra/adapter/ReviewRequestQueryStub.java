package com.ubidict.backend.draftdocument.infra.adapter;

import com.ubidict.backend.draftdocument.infra.port.ReviewRequestQueryPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component("reviewRequestQueryStubForDraftDocument")
@ConditionalOnProperty(name = "app.crossdomain.review-request.mode", havingValue = "stub")
public class ReviewRequestQueryStub implements ReviewRequestQueryPort {

    @Override
    public boolean hasOngoingDocumentReview(Long documentId) {
        return false;
    }
}
