package com.ubidict.backend.support;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.hibernate.Session;
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

        boolean mysql = isMySql();
        toggleConstraints(mysql, false);
        tableNames().forEach(table -> em.createNativeQuery("truncate table `" + table + "`")
                .executeUpdate());
        toggleConstraints(mysql, true);
    }

    /**
     * 외래키 검사를 끄는 문법이 DB마다 다르다. 테스트 DB는 H2가 기본이지만, collation이 검증 대상인 일부 테스트는
     * 실제 MySQL 위에서 돈다({@link MySqlContainerConfiguration}) — H2 문법을 그대로 보내면 문법 오류로 죽는다.
     */
    private void toggleConstraints(boolean mysql, boolean enabled) {
        String sql =
                mysql ? "set foreign_key_checks = " + (enabled ? "1" : "0") : "set referential_integrity " + enabled;
        em.createNativeQuery(sql).executeUpdate();
    }

    private boolean isMySql() {
        return em.unwrap(Session.class).doReturningWork(connection -> connection
                .getMetaData()
                .getDatabaseProductName()
                .toLowerCase()
                .contains("mysql"));
    }

    @SuppressWarnings("unchecked")
    private List<String> tableNames() {
        List<String> tables = em.createNativeQuery("select table_name from information_schema.tables "
                        + "where table_schema = schema() and table_type = 'BASE TABLE'")
                .getResultList();

        return tables.stream()
                .filter(table -> !FLYWAY_HISTORY_TABLE.equalsIgnoreCase(table))
                .toList();
    }
}
