package com.ubidict.backend.support;

import com.ubidict.backend.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Service 테스트의 공통 설정. 실제 컨테이너와 Flyway 스키마 위에서 검증한다.
 *
 * <p>테스트마다 DbCleaner로 테이블을 비운다. @Transactional 롤백에 기대지 않는 이유는, 서비스가 선언한 트랜잭션 경계를 테스트 트랜잭션이 덮어써 경계 자체를
 * 검증하지 못하게 되기 때문이다.
 */
@ActiveProfiles("test")
@Import({TestcontainersConfiguration.class, DbCleaner.class})
@SpringBootTest(webEnvironment = WebEnvironment.NONE)
public abstract class IntegrationTestSupport {

    @Autowired
    private DbCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.clean();
    }
}
