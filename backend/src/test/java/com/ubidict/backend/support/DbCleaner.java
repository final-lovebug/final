package com.ubidict.backend.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 테스트 사이에 테이블을 비운다.
 *
 * <p>엔티티 메타모델이 아니라 information_schema에서 실제 테이블을 읽는다. 테스트 전용 엔티티처럼 마이그레이션에 없는 테이블을 지우려다 실패하는 것을 막는다.
 *
 * <p>Flyway 이력 테이블은 지우지 않는다. 지우면 다음 컨텍스트에서 마이그레이션이 다시 돌아 스키마가 어긋난다.
 */
@Component
public class DbCleaner {

    private static final String FLYWAY_HISTORY_TABLE = "flyway_schema_history";

    @PersistenceContext
    private EntityManager em;

    @Transactional
    public void clean() {
        em.flush();
        em.clear();

        em.createNativeQuery("set foreign_key_checks = 0").executeUpdate();
        tableNames().forEach(table -> em.createNativeQuery("truncate table `" + table + "`")
                .executeUpdate());
        em.createNativeQuery("set foreign_key_checks = 1").executeUpdate();
    }

    @SuppressWarnings("unchecked")
    private List<String> tableNames() {
        List<String> tables = em.createNativeQuery("select table_name from information_schema.tables "
                        + "where table_schema = database() and table_type = 'BASE TABLE'")
                .getResultList();

        return tables.stream()
                .filter(table -> !FLYWAY_HISTORY_TABLE.equalsIgnoreCase(table))
                .toList();
    }
}
