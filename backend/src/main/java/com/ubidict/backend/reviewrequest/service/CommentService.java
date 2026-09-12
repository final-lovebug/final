package com.ubidict.backend.reviewrequest.service;

import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.implement.CommentReader;
import com.ubidict.backend.reviewrequest.implement.CommentWriter;
import com.ubidict.backend.reviewrequest.implement.ReviewReader;
import com.ubidict.backend.reviewrequest.implement.ReviewRequestReader;
import com.ubidict.backend.reviewrequest.service.model.AddCommentCommand;
import com.ubidict.backend.reviewrequest.service.model.CommentResult;
import com.ubidict.backend.reviewrequest.service.model.ResolveCommentCommand;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentReader commentReader;
    private final CommentWriter commentWriter;
    private final ReviewReader reviewReader;
    private final ReviewRequestReader reviewRequestReader;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    @Transactional
    public CommentResult add(AddCommentCommand command) {
        Review review = readAccessibleReview(command.reviewId(), command.authorId());
        Comment comment = Comment.create(
                review.getId(),
                command.authorId(),
                command.content(),
                command.anchor(),
                command.targetItemId(),
                command.parentId(),
                command.authorId());
        validateParent(command.parentId(), comment);
        comment = commentWriter.write(comment);

        log.info(
                "[CommentService.add] Review comment added. reviewRequestId={}, reviewId={}, commentId={}, authorId={}",
                review.getReviewRequestId(),
                review.getId(),
                comment.getId(),
                command.authorId());

        return CommentResult.from(comment);
    }

    @Transactional(readOnly = true)
    public List<CommentResult> list(Long reviewRequestId, Long memberId, Boolean resolved, Long targetItemId) {
        ReviewRequest reviewRequest = reviewRequestReader.read(reviewRequestId);
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);

        return toTree(commentReader.readAll(reviewRequestId, resolved, targetItemId));
    }

    @Transactional
    public CommentResult resolve(ResolveCommentCommand command) {
        Comment comment = commentReader.read(command.commentId());
        Review review = readAccessibleReview(comment.getReviewId(), command.actorId());
        if (command.resolved()) {
            comment.resolve();
        } else {
            comment.reopen();
        }

        log.info(
                "[CommentService.resolve] Review comment resolution changed. reviewRequestId={}, commentId={}, resolved={}, actorId={}",
                review.getReviewRequestId(),
                comment.getId(),
                comment.isResolved(),
                command.actorId());

        return CommentResult.from(comment);
    }

    private Review readAccessibleReview(Long reviewId, Long memberId) {
        Review review = reviewReader.read(reviewId);
        ReviewRequest reviewRequest = reviewRequestReader.read(review.getReviewRequestId());
        workspaceAccessValidator.validateParticipant(reviewRequest.getWorkspaceId(), memberId);
        return review;
    }

    private void validateParent(Long parentId, Comment comment) {
        if (parentId == null) {
            return;
        }
        Comment parent = commentReader.read(parentId);
        comment.validateParent(parent);
    }

    private static List<CommentResult> toTree(List<Comment> comments) {
        Map<Long, Comment> commentsById = new LinkedHashMap<>();
        Map<Long, List<Comment>> childrenByParent = new LinkedHashMap<>();
        for (Comment comment : comments) {
            commentsById.put(comment.getId(), comment);
            if (comment.getParentId() != null) {
                childrenByParent
                        .computeIfAbsent(comment.getParentId(), ignored -> new ArrayList<>())
                        .add(comment);
            }
        }

        return comments.stream()
                .filter(comment -> comment.getParentId() == null || !commentsById.containsKey(comment.getParentId()))
                .map(comment -> toTree(comment, childrenByParent))
                .toList();
    }

    private static CommentResult toTree(Comment comment, Map<Long, List<Comment>> childrenByParent) {
        List<CommentResult> children = childrenByParent.getOrDefault(comment.getId(), List.of()).stream()
                .map(child -> toTree(child, childrenByParent))
                .toList();
        return CommentResult.from(comment, children);
    }
}
