package com.ubidict.backend.common.domain;

import jakarta.persistence.MappedSuperclass;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 생성·수정 시각에 더해 <b>소프트 삭제</b>를 가지는 상위 클래스.
 *
 * <p>생성/수정 시각은 Hibernate가 채우므로 애플리케이션 코드에서 직접 설정하지 않는다.
 * Spring Data Auditing(@CreatedDate)은 OffsetDateTime 변환을 지원하지 않아 Hibernate 타임스탬프를 사용한다.
 *
 * <p>삭제 경로가 없는 엔티티는 {@code deleted_at} 컬럼을 만들지 않으므로 {@link AuditableEntity}를 상속한다.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@MappedSuperclass
public abstract class BaseEntity extends AuditableEntity {

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
