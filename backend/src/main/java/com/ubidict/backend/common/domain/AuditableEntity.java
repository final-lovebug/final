package com.ubidict.backend.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 생성·수정 시각만 가지는 상위 클래스. <b>삭제 경로가 없는 엔티티</b>가 상속한다.
 *
 * <p>확정 후 불변인 스냅샷({@code DocumentVersion})이나 연결 행({@code DocumentLabel})은 지울 일이 없어 {@code deleted_at}
 * 컬럼을 만들지 않는다. 그런 엔티티가 {@link BaseEntity}를 상속하면 항상 null인 컬럼을 테이블마다 끌고 다니게 된다.
 *
 * <p>소프트 삭제가 필요한 엔티티는 {@link BaseEntity}를 상속한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class AuditableEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;
}
