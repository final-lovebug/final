package com.ubidict.backend.support;

import com.ubidict.backend.common.infra.persistence.JpaAuditingConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * {@link RepositoryTestSupport}와 같되 <b>H2 대신 실제 MySQL</b> 위에서 돈다.
 *
 * <p>유니크 제약이 대소문자를 접는지처럼 <b>DB가 판정 주체인 검증</b>에만 쓴다. 이유는
 * {@link MySqlContainerConfiguration} 참고.
 *
 * <p><b>{@code @AutoConfigureTestDatabase}를 꺼야 한다.</b> {@code @DataJpaTest}는 기본으로 데이터소스를 내장 DB로
 * 바꾸는데, 이제 H2가 테스트 클래스패스에 있으므로 그대로 두면 컨테이너 대신 H2가 붙어 이 클래스를 쓰는 의미가 사라진다.
 */
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({DbCleaner.class, JpaAuditingConfig.class, MySqlContainerConfiguration.class})
@DataJpaTest(properties = {"spring.jpa.hibernate.ddl-auto=create-drop", "app.messaging.mode=in-memory"})
public abstract class MySqlRepositoryTestSupport {

    @Autowired
    protected TestEntityManager em;

    @Autowired
    private DbCleaner dbCleaner;

    @BeforeEach
    void setUp() {
        dbCleaner.clean();
    }
}
