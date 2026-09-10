package com.ubidict.backend.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RuleSetTest {

    @DisplayName("초기 룰셋의 필수 리뷰어 수는 모두 0이다.")
    @Test
    void initial() {
        // when
        RuleSet ruleSet = RuleSet.initial();

        // then
        assertThat(ruleSet.requiredDocumentReviewerCount()).isZero();
        assertThat(ruleSet.requiredDictionaryReviewerCount()).isZero();
    }

    @DisplayName("필수 리뷰어 수가 0 이상이면 룰셋을 만들 수 있다.")
    @Test
    void create() {
        // when
        RuleSet ruleSet = new RuleSet(2, 3);

        // then
        assertThat(ruleSet.requiredDocumentReviewerCount()).isEqualTo(2);
        assertThat(ruleSet.requiredDictionaryReviewerCount()).isEqualTo(3);
    }

    @DisplayName("필수 문서 리뷰어 수가 음수라면 룰셋을 만들 수 없다.")
    @Test
    void create_documentReviewerCountIsNegative() {
        // when & then
        assertThatThrownBy(() -> new RuleSet(-1, 0))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVALID_REVIEWER_COUNT);
    }

    @DisplayName("필수 사전 리뷰어 수가 음수라면 룰셋을 만들 수 없다.")
    @Test
    void create_dictionaryReviewerCountIsNegative() {
        // when & then
        assertThatThrownBy(() -> new RuleSet(0, -1))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVALID_REVIEWER_COUNT);
    }
}
