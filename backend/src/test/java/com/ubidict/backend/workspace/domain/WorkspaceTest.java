package com.ubidict.backend.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkspaceTest {

    private static final Long CREATED_BY = 1L;

    @DisplayName("워크스페이스를 만들면 이름과 생성자가 설정된다.")
    @Test
    void create() {
        // when
        Workspace workspace = Workspace.create("개발팀", CREATED_BY);

        // then
        assertThat(workspace.getName()).isEqualTo("개발팀");
        assertThat(workspace.getCreatedBy()).isEqualTo(CREATED_BY);
    }

    @DisplayName("워크스페이스를 만들면 룰셋이 기본값 0으로 함께 심어진다.")
    @Test
    void create_ruleSetIsInitial() {
        // when
        Workspace workspace = Workspace.create("개발팀", CREATED_BY);

        // then
        assertThat(workspace.getRuleSet()).isEqualTo(RuleSet.initial());
        assertThat(workspace.getRuleSet().requiredDocumentReviewerCount()).isZero();
        assertThat(workspace.getRuleSet().requiredDictionaryReviewerCount()).isZero();
    }

    @DisplayName("이름이 50자라면 워크스페이스를 만들 수 있다.")
    @Test
    void create_nameIsMaxLength() {
        // given
        String name = "가".repeat(50);

        // when
        Workspace workspace = Workspace.create(name, CREATED_BY);

        // then
        assertThat(workspace.getName()).isEqualTo(name);
    }

    @DisplayName("이름이 비어 있다면 워크스페이스를 만들 수 없다.")
    @Test
    void create_nameIsBlank() {
        // when & then
        assertThatThrownBy(() -> Workspace.create("   ", CREATED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVALID_NAME);
    }

    @DisplayName("이름이 50자를 넘으면 워크스페이스를 만들 수 없다.")
    @Test
    void create_nameIsTooLong() {
        // given
        String name = "가".repeat(51);

        // when & then
        assertThatThrownBy(() -> Workspace.create(name, CREATED_BY))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVALID_NAME);
    }

    @DisplayName("이름을 바꾸면 새 이름이 반영된다.")
    @Test
    void rename() {
        // given
        Workspace workspace = Workspace.create("개발팀", CREATED_BY);

        // when
        workspace.rename("플랫폼팀");

        // then
        assertThat(workspace.getName()).isEqualTo("플랫폼팀");
    }

    @DisplayName("바꿀 이름이 비어 있다면 이름을 바꿀 수 없다.")
    @Test
    void rename_nameIsBlank() {
        // given
        Workspace workspace = Workspace.create("개발팀", CREATED_BY);

        // when & then
        assertThatThrownBy(() -> workspace.rename(""))
                .isInstanceOf(BusinessException.class)
                .extracting(exception -> ((BusinessException) exception).errorCode())
                .isEqualTo(WorkspaceErrorCode.WORKSPACE_INVALID_NAME);
        assertThat(workspace.getName()).isEqualTo("개발팀");
    }

    @DisplayName("룰셋을 바꾸면 새 값이 반영된다.")
    @Test
    void changeRuleSet_replacesValues() {
        Workspace workspace = Workspace.create("개발팀", CREATED_BY);

        workspace.changeRuleSet(new RuleSet(2, 3));

        assertThat(workspace.getRuleSet()).isEqualTo(new RuleSet(2, 3));
    }
}
