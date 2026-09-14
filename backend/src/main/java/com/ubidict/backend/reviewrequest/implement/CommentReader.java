package com.ubidict.backend.reviewrequest.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.domain.Comment;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.infra.CommentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentReader {

    private final CommentRepository commentRepository;

    public Comment read(Long commentId) {
        return commentRepository
                .findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_COMMENT_NOT_FOUND));
    }

    public List<Comment> readAll(Long reviewRequestId, Boolean resolved, Long targetItemId) {
        return commentRepository.findByReviewRequestId(reviewRequestId, resolved, targetItemId);
    }
}
