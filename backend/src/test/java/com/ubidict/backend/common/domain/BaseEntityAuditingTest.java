package com.ubidict.backend.common.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.common.infra.persistence.JpaAuditingConfig;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 테스트 전용 엔티티를 쓰므로 마이그레이션 대상이 아니다. 스키마는 Flyway 대신 Hibernate가 생성한다.
 */
@Import({JpaAuditingConfig.class, BaseEntityAuditingTest.MySqlContainerConfiguration.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"})
class BaseEntityAuditingTest {

    @Autowired
    private TestEntityManager em;

    @DisplayName("엔티티를 저장하면 생성 시각과 수정 시각이 자동으로 채워진다.")
    @Test
    void persist() {
        // given
        AuditingTestEntity entity = new AuditingTestEntity("word");

        // when
        em.persistAndFlush(entity);

        // then
        assertThat(entity.getCreatedAt()).isNotNull();
        assertThat(entity.getUpdatedAt()).isNotNull();
        assertThat(entity.getCreatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(entity.getUpdatedAt().getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(entity.getCreatedAt().getNano() % 1_000).isZero();
        assertThat(entity.getUpdatedAt().getNano() % 1_000).isZero();
    }

    @DisplayName("엔티티를 저장하면 삭제 시각은 비어 있다.")
    @Test
    void persist_notDeleted() {
        // given
        AuditingTestEntity entity = new AuditingTestEntity("word");

        // when
        em.persistAndFlush(entity);

        // then
        assertThat(entity.getDeletedAt()).isNull();
        assertThat(entity.isDeleted()).isFalse();
    }

    @DisplayName("엔티티를 수정하면 수정 시각만 갱신된다.")
    @Test
    void update() {
        // given
        AuditingTestEntity entity = em.persistFlushFind(new AuditingTestEntity("word"));
        OffsetDateTime createdAt = entity.getCreatedAt();
        OffsetDateTime updatedAt = entity.getUpdatedAt();

        // when
        entity.changeName("term");
        em.flush();

        // then
        assertThat(entity.getCreatedAt()).isEqualTo(createdAt);
        assertThat(entity.getUpdatedAt()).isAfter(updatedAt);
    }

    @DisplayName("저장한 시각을 다시 조회하면 같은 시점으로 복원된다.")
    @Test
    void find_keepsInstant() {
        // given
        AuditingTestEntity entity = em.persistFlushFind(new AuditingTestEntity("word"));
        OffsetDateTime createdAt = entity.getCreatedAt();
        em.clear();

        // when
        AuditingTestEntity found = em.find(AuditingTestEntity.class, entity.getId());

        // then
        assertThat(found.getCreatedAt().toInstant()).isEqualTo(createdAt.toInstant());
    }

    @DisplayName("삭제한 엔티티를 다시 조회하면 삭제 시각이 유지된다.")
    @Test
    void find_deleted() {
        // given
        AuditingTestEntity entity = em.persistFlushFind(new AuditingTestEntity("word"));
        entity.delete();
        em.flush();
        em.clear();

        // when
        AuditingTestEntity found = em.find(AuditingTestEntity.class, entity.getId());

        // then
        assertThat(found.isDeleted()).isTrue();
        assertThat(found.getDeletedAt().toInstant())
                .isEqualTo(entity.getDeletedAt().toInstant());
    }

    /**
     * 컨테이너를 Spring 빈으로 두어 컨텍스트 종료 시점과 컨테이너 종료 시점을 맞춘다.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class MySqlContainerConfiguration {

        @Bean
        @ServiceConnection
        MySQLContainer mysqlContainer() {
            return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
        }
    }

    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @Entity
    static class AuditingTestEntity extends BaseEntity {

        @GeneratedValue(strategy = GenerationType.IDENTITY)
        @Id
        private Long id;

        private String name;

        AuditingTestEntity(String name) {
            this.name = name;
        }

        Long getId() {
            return id;
        }

        void changeName(String name) {
            this.name = name;
        }
    }
}
