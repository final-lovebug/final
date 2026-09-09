package com.ubidict.backend.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Repository 테스트의 공통 설정.
 *
 * <p>스키마는 Flyway 마이그레이션이 만든다. 실제 운영 스키마와 같은 형태에서 매핑을 검증하기 위해 Hibernate 생성(ddl-auto)에 기대지 않는다.
 */
@ActiveProfiles("test")
@Import({DbCleaner.class, RepositoryTestSupport.MySqlContainerConfiguration.class})
@AutoConfigureTestDatabase(replace = Replace.NONE)
@DataJpaTest
public abstract class RepositoryTestSupport {

    @Autowired
    protected TestEntityManager em;

    @Autowired
    private DbCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.clean();
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
}
