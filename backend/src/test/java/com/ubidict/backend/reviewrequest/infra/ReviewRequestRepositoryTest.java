package com.ubidict.backend.reviewrequest.infra;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.fixture.ReviewRequestFixture;
import com.ubidict.backend.support.RepositoryTestSupport;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReviewRequestRepositoryTest extends RepositoryTestSupport {

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @DisplayName("워크스페이스와 상태가 일치하는 리뷰 요청을 조회한다.")
    @Test
    void findByWorkspaceIdAndStatus() {
        // given
        ReviewRequest pending = reviewRequestRepository.save(
                ReviewRequestFixture.reviewRequest().workspaceId(10L).build());
        reviewRequestRepository.save(ReviewRequestFixture.reviewRequest()
                .workspaceId(20L)
                .title("다른 워크스페이스 리뷰")
                .build());
        em.flush();
        em.clear();

        // when
        List<ReviewRequest> reviewRequests =
                reviewRequestRepository.findByWorkspaceIdAndStatus(10L, ReviewRequestStatus.PENDING_REVIEW);

        // then
        assertThat(reviewRequests).extracting(ReviewRequest::getId).containsExactly(pending.getId());
    }

    @DisplayName("리뷰 요청을 저장하면 생성 시각과 수정 시각이 채워진다.")
    @Test
    void save_auditingFieldsAreSet() {
        // when
        ReviewRequest saved = reviewRequestRepository.save(
                ReviewRequestFixture.reviewRequest().build());
        em.flush();

        // then
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
        assertThat(saved.getDeletedAt()).isNull();
    }

    @DisplayName("삭제된 리뷰 요청은 조회되지 않는다.")
    @Test
    void findById_deleted() {
        // given
        ReviewRequest reviewRequest = reviewRequestRepository.save(
                ReviewRequestFixture.reviewRequest().build());
        reviewRequest.delete();
        em.flush();
        em.clear();

        // when
        Optional<ReviewRequest> found = reviewRequestRepository.findByIdAndDeletedAtIsNull(reviewRequest.getId());

        // then
        assertThat(found).isEmpty();
    }
}
