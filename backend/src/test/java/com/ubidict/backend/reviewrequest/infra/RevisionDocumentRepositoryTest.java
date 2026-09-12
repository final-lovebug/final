package com.ubidict.backend.reviewrequest.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.support.RepositoryTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RevisionDocumentRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private RevisionDocumentRepository revisionDocumentRepository;

    @DisplayName("리뷰 요청과 회차로 문서 개정안을 조회한다.")
    @Test
    void findByReviewRequestIdAndReexamineRound() {
        // given
        ReviewRequest request = reviewRequestRepository.save(
                ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰 요청", null, 1L, 1L));
        RevisionDocument revision =
                revisionDocumentRepository.save(RevisionDocument.create(request.getId(), 20L, 1, 30L, "개정 본문", 1L));

        // when & then
        assertThat(revisionDocumentRepository.findByReviewRequestId(request.getId()))
                .extracting(RevisionDocument::getId)
                .containsExactly(revision.getId());
        assertThat(revisionDocumentRepository.findByReviewRequestIdAndReexamineRound(request.getId(), 0))
                .contains(revision);
    }
}
