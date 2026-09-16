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
 * <p>엔티티 메타모델이 아니라 information_schema에서 실제 테이블을 읽는다. 컨텍스트마다 스캔되는 엔티티 집합이 달라 메타모델에 없는 테이블이 남을 수 있기 때문이다.
 */
@Component
public class DbCleaner {

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
        return em.createNativeQuery("select table_name from information_schema.tables "
                        + "where table_schema = schema() and table_type = 'BASE TABLE'")
                .getResultList();
    }
}
