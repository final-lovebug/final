package com.ubidict.backend.support;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * JPA 매핑이 <b>실제 MySQL</b>에서 DDL로 떨어지는지 검증한다.
 *
 * <p>스키마의 주인은 엔티티 매핑이다 — 마이그레이션 파일이 없고 {@code ddl-auto}가 스키마를 만든다. 그래서 잘못된 매핑은 배포 환경에서 <b>기동 시점에</b> 처음
 * 드러난다. 그 시점을 테스트로 당긴다.
 *
 * <p>MySQL 컨테이너를 쓰는 이유는 DDL 생성이 방언에 달려 있기 때문이다 — {@code columnDefinition}에 적은 타입명·생성 컬럼 식은 MySQL 문법이라 H2의
 * {@code MODE=MySQL}로는 검증되지 않는다. 나머지 테스트는 H2 위에서 돈다.
 */
@ActiveProfiles("test")
@Import(MySqlContainerConfiguration.class)
@SpringBootTest(
        webEnvironment = WebEnvironment.NONE,
        properties = {
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "app.messaging.mode=in-memory",
            "app.ai.dispatch.mode=in-process"
        })
class SchemaGenerationTest {

    /** 컨텍스트가 뜨면 통과다. DDL 생성 실패는 {@code SchemaManagementException}으로 기동 자체를 막는다. */
    @Test
    void 엔티티_매핑이_MySQL_스키마를_만든다() {}
}
