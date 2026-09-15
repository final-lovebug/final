package com.ubidict.backend.reviewrequest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestStatus;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.ReexamineRepository;
import com.ubidict.backend.reviewrequest.infra.ReviewRequestRepository;
import com.ubidict.backend.reviewrequest.infra.RevisionDocumentRepository;
import com.ubidict.backend.reviewrequest.service.model.PerformReexamineCommand;
import com.ubidict.backend.support.IntegrationTestSupport;
import com.ubidict.backend.workspace.domain.Participant;
import com.ubidict.backend.workspace.domain.Workspace;
import com.ubidict.backend.workspace.infra.ParticipantRepository;
import com.ubidict.backend.workspace.infra.WorkspaceRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReexamineServiceTest extends IntegrationTestSupport {

    private static final Long REQUESTER_ID = 1L;

    @Autowired
    private ReexamineService reexamineService;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ParticipantRepository participantRepository;

    @Autowired
    private ReviewRequestRepository reviewRequestRepository;

    @Autowired
    private RevisionDocumentRepository revisionDocumentRepository;

    @Autowired
    private ReexamineRepository reexamineRepository;

    @DisplayName("재교정하면 회차가 1 증가하고 리뷰중으로 돌아간다.")
    @Test
    void perform() {
        // given
        ReviewRequest request = saveRequest();

        // when
        var result = reexamineService.perform(
                new PerformReexamineCommand(request.getId(), "재교정 본문", List.of(), REQUESTER_ID));

        // then
        assertThat(result.round()).isEqualTo(1);
        assertThat(reviewRequestRepository
                        .findById(request.getId())
                        .orElseThrow()
                        .getStatus())
                .isEqualTo(ReviewRequestStatus.IN_REVIEW);
        assertThat(revisionDocumentRepository.findByReviewRequestIdAndReexamineRound(request.getId(), 1))
                .get()
                .extracting(RevisionDocument::getProposedBody)
                .isEqualTo("재교정 본문");
        assertThat(reexamineRepository.findByReviewRequestIdOrderByRoundAsc(request.getId()))
                .hasSize(1);
    }

    @DisplayName("변경요청 상태가 아니면 재교정할 수 없다.")
    @Test
    void perform_notChangesRequested() {
        // given
        ReviewRequest request = savePendingRequest();

        // when & then
        assertThatThrownBy(() -> reexamineService.perform(
                        new PerformReexamineCommand(request.getId(), "재교정 본문", List.of(), REQUESTER_ID)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_NOT_REEXAMINABLE));
    }

    private ReviewRequest saveRequest() {
        ReviewRequest request = savePendingRequest();
        request.startReview();
        request.requestChanges();
        return reviewRequestRepository.save(request);
    }

    private ReviewRequest savePendingRequest() {
        Workspace workspace = workspaceRepository.save(Workspace.create("개발팀", REQUESTER_ID));
        participantRepository.save(Participant.owner(workspace.getId(), REQUESTER_ID));
        ReviewRequest request = reviewRequestRepository.save(ReviewRequest.create(
                workspace.getId(), ReviewRequestType.DOCUMENT, "리뷰 요청", null, REQUESTER_ID, REQUESTER_ID));
        revisionDocumentRepository.save(RevisionDocument.create(request.getId(), 10L, 1, 20L, "개정 본문", REQUESTER_ID));
        return request;
    }
}
