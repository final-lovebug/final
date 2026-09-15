package com.ubidict.backend.support;

import com.ubidict.backend.common.infra.persistence.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Repository 테스트의 공통 설정.
 *
 * <p>H2와 Hibernate가 테스트마다 독립 스키마를 생성한다.
 */
@ActiveProfiles("test")
@Import({DbCleaner.class, JpaAuditingConfig.class})
@DataJpaTest(
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "app.messaging.mode=in-memory"
        })
public abstract class RepositoryTestSupport {

    @Autowired
    protected TestEntityManager em;

    @Autowired
    private DbCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.clean();
    }
}
