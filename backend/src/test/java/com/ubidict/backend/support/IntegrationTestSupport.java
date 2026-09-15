package com.ubidict.backend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.test.context.ActiveProfiles;

/**
 * Service 테스트의 공통 설정. H2가 생성한 인메모리 스키마 위에서 검증한다.
 *
 * <p>테스트마다 DbCleaner로 테이블을 비운다. @Transactional 롤백에 기대지 않는 이유는, 서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써 경계 자체를
 * 검증하지 못하게 되기 때문이다.
 */
@ActiveProfiles("test")
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "app.messaging.mode=in-memory",
            "app.ai.dispatch.mode=in-process"
        })
public abstract class IntegrationTestSupport {
    @Autowired
    private DbCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.clean();
    }
}
