package com.ubidict.backend.workspace.fixture;

import com.ubidict.backend.workspace.domain.RuleSet;
import com.ubidict.backend.workspace.domain.Workspace;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * 저장 전 식별자는 null이므로, 식별자가 검증에 필요한 테스트에서만 builder로 주입한다.
 */
public class WorkspaceFixture {

    public static WorkspaceBuilder workspace() {
        return new WorkspaceBuilder();
    }

    public static class WorkspaceBuilder {

        private Long id;
        private String name = "개발팀";
        private Long createdBy = 1L;
        private RuleSet ruleSet;

        public WorkspaceBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public WorkspaceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public WorkspaceBuilder createdBy(Long createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public WorkspaceBuilder ruleSet(RuleSet ruleSet) {
            this.ruleSet = ruleSet;
            return this;
        }

        public WorkspaceBuilder ruleSet(int requiredDocumentReviewerCount, int requiredDictionaryReviewerCount) {
            return ruleSet(new RuleSet(requiredDocumentReviewerCount, requiredDictionaryReviewerCount));
        }

        public Workspace build() {
            Workspace workspace = Workspace.create(name, createdBy);
            if (id != null) {
                ReflectionTestUtils.setField(workspace, "id", id);
            }
            if (ruleSet != null) {
                ReflectionTestUtils.setField(workspace, "ruleSet", ruleSet);
            }

            return workspace;
        }
    }
}
