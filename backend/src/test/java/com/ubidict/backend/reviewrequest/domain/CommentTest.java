package com.ubidict.backend.reviewrequest.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.reviewrequest.exception.ReviewRequestErrorCode;
import com.ubidict.backend.reviewrequest.fixture.CommentFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CommentTest {

    @DisplayName("전체 대상 코멘트는 위치가 비어 있을 수 있다.")
    @Test
    void create() {
        // when
        Comment comment = Comment.create(1L, 2L, "전체 의견", null, null, null, 2L);

        // then
        assertThat(comment.getAnchor()).isNull();
        assertThat(comment.getTargetItemId()).isNull();
        assertThat(comment.isResolved()).isFalse();
    }

    @DisplayName("코멘트 내용이 비어 있으면 예외가 발생한다.")
    @Test
    void create_contentIsBlank() {
        // when & then
        assertThatThrownBy(() -> Comment.create(1L, 2L, " ", null, null, null, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_COMMENT_CONTENT_REQUIRED));
    }

    @DisplayName("자기 자신을 상위 코멘트로 지정하면 예외가 발생한다.")
    @Test
    void create_parentIsSelf() {
        // given
        Comment comment = CommentFixture.comment().id(1L).build();

        // when & then
        assertThatThrownBy(() -> comment.validateParent(comment))
                .isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.errorCode())
                        .isEqualTo(ReviewRequestErrorCode.REVIEW_REQUEST_INVALID_COMMENT_PARENT));
    }

    @DisplayName("코멘트를 해결하고 다시 연다.")
    @Test
    void resolve() {
        // given
        Comment comment = Comment.create(1L, 2L, "의견", null, null, null, 2L);

        // when
        comment.resolve();

        // then
        assertThat(comment.isResolved()).isTrue();

        // when
        comment.reopen();

        // then
        assertThat(comment.isResolved()).isFalse();
    }
}
