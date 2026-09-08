package com.ubidict.backend.common.infra;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BaseEntityTest {

    @DisplayName("삭제하면 삭제 시각이 기록된다.")
    @Test
    void delete() {
        // given
        TestEntity entity = new TestEntity();

        // when
        entity.delete();

        // then
        assertThat(entity.isDeleted()).isTrue();
        assertThat(entity.getDeletedAt()).isNotNull();
    }

    @DisplayName("이미 삭제된 엔티티를 다시 삭제해도 삭제 시각은 바뀌지 않는다.")
    @Test
    void delete_alreadyDeleted() {
        // given
        TestEntity entity = new TestEntity();
        entity.delete();
        OffsetDateTime deletedAt = entity.getDeletedAt();

        // when
        entity.delete();

        // then
        assertThat(entity.getDeletedAt()).isEqualTo(deletedAt);
    }

    @DisplayName("삭제하지 않은 엔티티는 삭제 상태가 아니다.")
    @Test
    void isDeleted_notDeleted() {
        // given
        TestEntity entity = new TestEntity();

        // when & then
        assertThat(entity.isDeleted()).isFalse();
        assertThat(entity.getDeletedAt()).isNull();
    }

    static class TestEntity extends BaseEntity {}
}
