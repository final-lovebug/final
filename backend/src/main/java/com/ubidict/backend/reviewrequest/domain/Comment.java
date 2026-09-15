package com.ubidict.backend.reviewrequest.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reviewId;

    @Column(nullable = false, updatable = false)
    private Long authorId;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "startOffset", column = @Column(name = "start_offset")),
        @AttributeOverride(name = "endOffset", column = @Column(name = "end_offset"))
    })
    private TextRange anchor;

    private Long targetItemId;

    @Column(updatable = false)
    private Long parentId;

    @Column(nullable = false)
    private boolean resolved;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Comment(
            Long reviewId,
            Long authorId,
            String content,
            TextRange anchor,
            Long targetItemId,
            Long parentId,
            Long createdBy) {
        if (reviewId == null || authorId == null || createdBy == null) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_COMMENT_CONTENT_REQUIRED);
        }

        this.reviewId = reviewId;
        this.authorId = authorId;
        this.content = content.strip();
        this.anchor = anchor;
        this.targetItemId = targetItemId;
        this.parentId = parentId;
        this.createdBy = createdBy;
    }

    public static Comment create(
            Long reviewId,
            Long authorId,
            String content,
            TextRange anchor,
            Long targetItemId,
            Long parentId,
            Long createdBy) {
        return new Comment(reviewId, authorId, content, anchor, targetItemId, parentId, createdBy);
    }

    public void resolve() {
        resolved = true;
    }

    public void reopen() {
        resolved = false;
    }

    public void validateParent(Comment parent) {
        boolean isSelf = parent == this || (id != null && id.equals(parent.getId()));
        if (isSelf || !reviewId.equals(parent.getReviewId())) {
            throw new BusinessException(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_COMMENT_PARENT);
        }
    }
}
