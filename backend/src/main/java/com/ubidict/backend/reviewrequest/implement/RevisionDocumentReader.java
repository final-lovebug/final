package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionDocumentReader {
    private final RevisionDocumentRepository repository;

    public List<RevisionDocument> read(Long id) {
        return repository.findByReviewRequestId(id);
    }

    public List<RevisionDocument> read(Long reviewRequestId, Integer round) {
        if (round == null) {
            return read(reviewRequestId);
        }
        return List.of(repository
                .findByReviewRequestIdAndReexamineRound(reviewRequestId, round)
                .orElseThrow(() -> new com.ubidict.backend.common.exception.BusinessException(
                        com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode
                                .REVIEW_REQUEST_REVISION_NOT_FOUND)));
    }

    public boolean exists(Long id, int round) {
        return repository.findByReviewRequestIdAndReexamineRound(id, round).isPresent();
    }
}
