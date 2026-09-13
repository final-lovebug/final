package com.ubidict.backend.draftdocument.infra.port;

public interface ReviewRequestQueryPort {

    boolean hasOngoingDocumentReview(Long documentId);
}
