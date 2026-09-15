package com.ubidict.backend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link IntegrationTestSupport}와 같되 <b>H2 대신 실제 MySQL</b> 위에서 돈다.
 *
 * <p>DB의 collation이 검증 대상인 테스트에만 쓴다 — 지금은 라벨 대소문자 정합(`D-94`) 하나다. 그 밖의 테스트는
 * {@link IntegrationTestSupport}(H2)를 그대로 쓴다. 컨테이너를 띄우는 비용이 있으므로 <b>기본으로 삼지 않는다.</b>
 *
 * <p>왜 필요한지는 {@link MySqlContainerConfiguration}에 적었다.
 */
@ActiveProfiles("test")
@Import(MySqlContainerConfiguration.class)
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.flyway.enabled=false",
            "app.messaging.mode=in-memory",
            "app.ai.dispatch.mode=in-process"
        })
public abstract class MySqlIntegrationTestSupport {

    @Autowired
    private DbCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.clean();
    }
}
