package com.ubidict.backend.reviewrequest.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevisionDictionaryRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private RevisionDictionaryRepository revisionDictionaryRepository;

    @DisplayName("리뷰 요청과 회차로 사전 개정안을 조회한다.")
    @Test
    void findByReviewRequestIdAndReexamineRound() {
        // given
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(10L, ReviewRequestType.DICTIONARY, "리뷰 요청", null, 1L, 1L));
        RevisionDictionary revision =
                revisionDictionaryRepository.save(RevisionDictionary.create(request.getId(), 20L, 1, 30L, 1L));

        // when & then
        assertThat(revisionDictionaryRepository.findByReviewRequestId(request.getId()))
                .extracting(RevisionDictionary::getId)
                .containsExactly(revision.getId());
        assertThat(revisionDictionaryRepository.findByReviewRequestIdAndReexamineRound(request.getId(), 0))
                .contains(revision);
    }
}
