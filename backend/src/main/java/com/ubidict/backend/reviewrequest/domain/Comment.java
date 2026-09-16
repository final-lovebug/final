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
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(
        indexes = {
            @Index(name = "idx_comment_review", columnList = "review_id, created_at, id"),
            @Index(name = "idx_comment_parent", columnList = "parent_id")
        })
// 앵커는 두 오프셋이 함께 있거나 함께 없다. 한쪽만 채워진 코멘트는 어디를 가리키는지 알 수 없다.
@Check(
        name = "ck_comment_anchor",
        constraints = "(start_offset is null and end_offset is null)"
                + " or (start_offset is not null and end_offset is not null"
                + " and start_offset >= 0 and start_offset <= end_offset)")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Comment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long reviewId;

    @Column(nullable = false, updatable = false)
    private Long authorId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
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
