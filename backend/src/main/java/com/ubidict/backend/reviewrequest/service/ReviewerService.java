package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.reviewrequest.domain.Reviewer;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.implement.ReviewerDuplicationValidator;
import com.ubidict.backend.reviewrequest.implement.ReviewerReader;
import com.ubidict.backend.reviewrequest.implement.ReviewerWriter;
import com.ubidict.backend.reviewrequest.service.model.AssignReviewerCommand;
import com.ubidict.backend.reviewrequest.service.model.ReviewerResult;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReviewerService {
    private final ReviewerReader reader;
    private final ReviewerWriter writer;
    private final ReviewerDuplicationValidator validator;
    private final ReviewRequestReader reviewRequestReader;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional
    public ReviewerResult assign(AssignReviewerCommand command) {
        var reviewRequest = reviewRequestReader.read(command.reviewRequestId());
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), command.actorId());
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), command.memberId());
        validator.validate(command.reviewRequestId(), command.memberId());
        Reviewer saved =
                writer.write(Reviewer.create(command.reviewRequestId(), command.memberId(), command.actorId()));
        log.info(
                "[ReviewerService.assign] Reviewer assigned. requestId={}, memberId={}",
                command.reviewRequestId(),
                command.memberId());
        return ReviewerResult.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewerResult> list(Long requestId) {
        return reader.readAll(requestId).stream().map(ReviewerResult::from).toList();
    }

    @Transactional
    public void remove(Long reviewerId) {
        writer.delete(reader.read(reviewerId));
        log.info("[ReviewerService.remove] Reviewer removed. reviewerId={}", reviewerId);
    }
}
