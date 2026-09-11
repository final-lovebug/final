package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.infra.RevisionDictionaryRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RevisionDictionaryReader {
    private final RevisionDictionaryRepository repository;

    public List<RevisionDictionary> read(Long id) {
        return repository.findByReviewRequestId(id);
    }

    public List<RevisionDictionary> read(Long reviewRequestId, Integer round) {
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
