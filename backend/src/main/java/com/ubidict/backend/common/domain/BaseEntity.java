package com.ubidict.backend.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * 모든 도메인 엔티티가 공통으로 가지는 생성·수정·삭제 시각을 정의한다.
 *
 * <p>생성/수정 시각은 Hibernate가 채우므로 애플리케이션 코드에서 직접 설정하지 않는다.
 * Spring Data Auditing(@CreatedDate)은 OffsetDateTime 변환을 지원하지 않아 Hibernate 타임스탬프를 사용한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseEntity {

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    private OffsetDateTime deletedAt;

    /**
     * 이미 삭제된 엔티티의 삭제 시각은 최초 삭제 시점으로 유지한다.
     *
     * <p>DB의 시각 정밀도가 마이크로초이므로 같은 값으로 잘라 저장 전후 값이 달라지지 않게 한다.
     */
    public void delete() {
        if (isDeleted()) {
            return;
        }
        deletedAt = OffsetDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
