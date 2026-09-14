package com.ubidict.backend.workspace.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.workspace.exception.WorkspaceErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사전집과 문서를 공유하는 협업 단위.
 *
 * <p>생성 시 룰셋을 기본값(0/0)으로 함께 심는다. 룰셋만 따로 만들거나 지우는 경로는 없다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Workspace extends BaseEntity {

    private static final int NAME_MAX_LENGTH = 50;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Embedded
    private RuleSet ruleSet;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Workspace(String name, Long createdBy) {
        this.name = name;
        this.ruleSet = RuleSet.initial();
        this.createdBy = createdBy;
    }

    public static Workspace create(String name, Long createdBy) {
        validateName(name);

        return new Workspace(name, createdBy);
    }

    public void rename(String name) {
        validateName(name);

        this.name = name;
    }

    public void changeRuleSet(RuleSet ruleSet) {
        this.ruleSet = ruleSet;
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank() || name.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(WorkspaceErrorCode.WORKSPACE_INVALID_NAME);
        }
    }
}
