package com.ubidict.backend.dictionary.domain;

import com.ubidict.backend.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 사전집의 한 확정 버전. 워크스페이스에 행이 쌓이고 ACTIVE인 행 하나가 가장 최근 확정본이자 문서 대조의 기준이다.
 *
 * <p>확정된 뒤에는 내용이 바뀌지 않는다. 상태 전환만 예외이며, 용어를 바꾸려면 새 버전을 반영해야 한다. 버전마다 용어가 함께 복제되므로 ARCHIVED 사전집에 매달린 Term이 곧 그
 * 버전의 스냅샷이 된다.
 *
 * <p>워크스페이스를 @ManyToOne으로 참조하지 않고 식별자로만 가리킨다. 애그리게잇 경계를 식별자로 넘어 지연 로딩 프록시가 상위 레이어로 새는 경로를 막는다. 같은 이유로 Term
 * 컬렉션도 매달지 않는다 — 용어 수백 개를 통째로 끌고 다니지 않기 위해 TermReader가 따로 읽는다.
 *
 * <p>삭제 경로가 없어 deletedAt이 필요하지 않으므로 AuditableEntity를 상속한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dictionary extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Embedded
    private DictionaryVersion version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DictionaryStatus status;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Dictionary(Long workspaceId, DictionaryVersion version, Long createdBy) {
        this.workspaceId = workspaceId;
        this.version = version;
        this.status = DictionaryStatus.ACTIVE;
        this.createdBy = createdBy;
    }

    /**
     * 워크스페이스의 첫 사전집을 만든다. 버전은 1이다.
     */
    public static Dictionary createFirst(Long workspaceId, Long createdBy) {
        return new Dictionary(workspaceId, DictionaryVersion.initial(), createdBy);
    }

    /**
     * 이전 버전을 이어 다음 버전을 만든다. 인자로 받은 이전 버전은 건드리지 않는다 — 보관 처리는 호출자가 archive()로 따로 한다.
     */
    public static Dictionary nextVersion(Dictionary previous, Long createdBy) {
        return new Dictionary(previous.workspaceId, previous.version.next(), createdBy);
    }

    /**
     * 지나간 버전으로 내린다. 이미 내려간 버전이면 아무 일도 하지 않는다.
     */
    public void archive() {
        if (!isActive()) {
            return;
        }
        status = DictionaryStatus.ARCHIVED;
    }

    public boolean isActive() {
        return status == DictionaryStatus.ACTIVE;
    }

    public int versionNo() {
        return version.versionNo();
    }

    public OffsetDateTime publishedAt() {
        return version.publishedAt();
    }
}
