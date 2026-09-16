package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.LabelErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.text.Normalizer;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 문서를 분류하는 워크스페이스 소유의 이름표.
 *
 * <p>이름을 워크스페이스 안에서 유일하게 두어 오타로 같은 뜻의 라벨이 갈라지는 것을 막는다. 같은 뜻 다른 표기를 없애자는 제품에서 라벨이 그 문제를 일으키면 곤란하다.
 *
 * <p>비교 기준은 앞뒤 공백을 제거한 뒤 <b>대소문자를 구분하지 않는 것</b>이다(D-94). 저장되는 이름은 최초 생성 시 입력한 표기를 그대로 유지하고,
 * 중복 판정만 {@link #matchKey(String)}로 한다 — {@code api}가 있는 워크스페이스에 {@code API}를 붙이면 새 라벨이 생기지 않고 기존 {@code api}가 재사용된다.
 *
 * <p>사전집의 표준어({@code Term.preferredForm})는 여기에 묶지 않는다. 같은 구조의 결함이 남아 있으나 정책이 갈릴 수 있어 따로 정한다(Y-37).
 */
@Getter
@Entity
@Table(
        uniqueConstraints =
                @UniqueConstraint(
                        name = "uk_label_workspace_name",
                        columnNames = {"workspace_id", "name"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Label extends BaseEntity {

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
     * 표시용 이름으로 다듬는다. <b>케이스는 건드리지 않는다</b> — 최초 생성 시 입력한 표기를 그대로 남기기 위해서다(D-94).
     *
     * <p>중복 판정에는 이 값이 아니라 {@link #matchKey(String)}를 쓴다.
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

    /**
     * 중복 판정 전용 키. 저장하지 않는다.
     *
     * <p>{@code label.name}의 collation이 {@code utf8mb4_0900_ai_ci}라 DB는 대소문자와 악센트를 무시하고 비교한다 —
     * MySQL 8의 서버 기본값이고 루트 {@code compose.yaml}의 {@code --collation-server}가 그 값을 못 박는다.
     * 여기서 그 기준에 최대한 맞춘다 — NFKC 정규화로 조합형/완성형 한글까지 접고 소문자로 내린다.
     *
     * <p><b>완전히 같지는 않다.</b> 악센트 폴딩은 따라가지 않으므로 이 검증은 최선 노력이고, 최종 판정은 언제나
     * {@code uk_label_workspace_name}이 한다. 빠져나간 경우는 {@code LabelAppender}가 제약 위반을 받아 기존 행 재사용으로 수렴시킨다.
     */
    public static String matchKey(String name) {
        return Normalizer.normalize(normalizeName(name), Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
    }
}
