package com.ubidict.backend.support;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ResourceLoader;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypes;
import org.springframework.orm.jpa.persistenceunit.PersistenceManagedTypesScanner;
import org.springframework.test.context.ActiveProfiles;

/**
 * Flyway가 만든 스키마와 JPA 엔티티 매핑이 어긋나지 않는지 검증한다(`D-108`).
 *
 * <p>다른 테스트 지원 클래스는 전부 {@code ddl-auto=create-drop} + {@code flyway.enabled=false}라 <b>엔티티가
 * 만든 스키마를 엔티티가 검증</b>한다 — 두 정의가 갈라져도 아무도 알아채지 못한다. 실제 기동 경로는 정반대로
 * <b>Flyway가 만든 스키마를 엔티티가 검증</b>하므로({@code ddl-auto=validate}, `D-107`) 그 조합을 재현하는
 * 테스트가 하나는 있어야 한다. 이것이 없으면 드리프트의 첫 검출 지점이 실행·배포 시점이 된다.
 *
 * <p>MySQL 컨테이너를 쓰는 이유는 컬럼 타입 정합이 방언에 달려 있기 때문이다 — H2의 {@code MODE=MySQL}은
 * {@code text}도 {@code longtext}도 {@code CLOB}으로 읽어 둘을 구분하지 못한다.
 *
 * <p><b>이 테스트가 잡는 것은 컬럼의 존재와 타입까지다.</b> {@code nullable}·기본값·인덱스·외래키·collation은
 * Hibernate의 검증 범위 밖이라 마이그레이션 리뷰가 유일한 방어선이다.
 */
@ActiveProfiles("test")
@Import({MySqlContainerConfiguration.class, SchemaValidationTest.ProductionEntitiesOnly.class})
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
            "spring.jpa.hibernate.ddl-auto=validate",
            "spring.flyway.enabled=true",
            "app.messaging.mode=in-memory",
            "app.ai.dispatch.mode=in-process"
        })
class SchemaValidationTest {

    /** 컨텍스트가 뜨면 통과다. 검증 실패는 {@code SchemaManagementException}으로 기동 자체를 막는다. */
    @Test
    void 엔티티_매핑이_Flyway_스키마와_일치한다() {}

    /**
     * 엔티티 스캔에서 <b>테스트 전용 엔티티를 뺀다.</b>
     *
     * <p>{@code BaseEntityAuditingTest}의 내부 엔티티는 마이그레이션 대상이 아니라 대응 테이블이 없다. 그대로 두면
     * 컬럼 타입 불일치에 닿기도 전에 {@code missing table}로 멈춘다.
     *
     * <p>{@code PersistenceManagedTypes} 빈을 직접 등록하는 이유는 <b>Boot 4.0.8에
     * {@code ManagedClassNameFilter}가 없기 때문이다.</b> 자동 구성의 같은 빈이
     * {@code @ConditionalOnMissingBean}이라 이쪽이 이긴다.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ProductionEntitiesOnly {

        private static final String ENTITY_BASE_PACKAGE = "com.ubidict.backend";
        private static final String TEST_ONLY_ENTITY_PREFIX =
                "com.ubidict.backend.common.domain.BaseEntityAuditingTest$";

        @Bean
        PersistenceManagedTypes persistenceManagedTypes(ResourceLoader resourceLoader) {
            PersistenceManagedTypes scanned =
                    new PersistenceManagedTypesScanner(resourceLoader).scan(ENTITY_BASE_PACKAGE);
            List<String> productionEntities = scanned.getManagedClassNames().stream()
                    .filter(name -> !name.startsWith(TEST_ONLY_ENTITY_PREFIX))
                    .toList();
            return PersistenceManagedTypes.of(productionEntities.toArray(String[]::new));
        }
    }
}
