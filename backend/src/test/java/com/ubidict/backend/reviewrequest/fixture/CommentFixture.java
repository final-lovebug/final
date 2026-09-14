package com.ubidict.backend.reviewrequest.fixture;

import com.ubidict.backend.common.domain.TextRange;
import com.ubidict.backend.reviewrequest.domain.Comment;
import org.springframework.test.util.ReflectionTestUtils;

public class CommentFixture {

    public static CommentBuilder comment() {
        return new CommentBuilder();
    }

    public static class CommentBuilder {

        private Long id;
        private Long reviewId = 1L;
        private Long authorId = 1L;
        private String content = "검토 의견입니다.";
        private TextRange anchor;
        private Long targetItemId;
        private Long parentId;
        private boolean resolved;

        public CommentBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public CommentBuilder reviewId(Long reviewId) {
            this.reviewId = reviewId;
            return this;
        }

        public CommentBuilder anchor(TextRange anchor) {
            this.anchor = anchor;
            return this;
        }

        public CommentBuilder targetItemId(Long targetItemId) {
            this.targetItemId = targetItemId;
            return this;
        }

        public CommentBuilder parentId(Long parentId) {
            this.parentId = parentId;
            return this;
        }

        public CommentBuilder resolved(boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        public Comment build() {
            Comment comment = Comment.create(reviewId, authorId, content, anchor, targetItemId, parentId, authorId);
            if (id != null) {
                ReflectionTestUtils.setField(comment, "id", id);
            }
            if (resolved) {
                comment.resolve();
            }
            return comment;
        }
    }
}
