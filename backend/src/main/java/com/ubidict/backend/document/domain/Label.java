package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.domain.AuditableEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.LabelErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서를 분류하는 워크스페이스 소유의 이름표.
 *
 * <p>이름을 워크스페이스 안에서 유일하게 두어 오타로 같은 뜻의 라벨이 갈라지는 것을 막는다. 같은 뜻 다른 표기를 없애자는 제품에서 라벨이 그 문제를 일으키면 곤란하다.
 *
 * <p>비교 기준은 앞뒤 공백을 제거한 뒤 대소문자를 구분하는 것이며, 사전집의 표준어({@code Term.preferredForm})와 같다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Label extends AuditableEntity {

    public static final int NAME_MAX_LENGTH = 20;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(nullable = false, updatable = false, length = NAME_MAX_LENGTH)
    private String name;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Label(Long workspaceId, String name, Long createdBy) {
        this.workspaceId = workspaceId;
        this.name = name;
        this.createdBy = createdBy;
    }

    public static Label create(Long workspaceId, String name, Long memberId) {
        return new Label(workspaceId, normalizeName(name), memberId);
    }

    /**
     * 저장 전에 이름을 맞춰 보거나 중복을 걸러낼 때 쓴다. 검증 규칙을 호출자가 다시 쓰지 않게 한다.
     */
    public static String normalizeName(String name) {
        if (name == null) {
            throw new BusinessException(LabelErrorCode.LABEL_INVALID_NAME);
        }

        String normalized = name.strip();
        if (normalized.isEmpty() || normalized.length() > NAME_MAX_LENGTH) {
            throw new BusinessException(LabelErrorCode.LABEL_INVALID_NAME);
        }

        return normalized;
    }
}
