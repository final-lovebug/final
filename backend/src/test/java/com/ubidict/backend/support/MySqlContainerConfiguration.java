package com.ubidict.backend.support;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * collation이 검증 대상인 테스트만 쓰는 MySQL 컨테이너.
 *
 * <p>테스트 DB는 H2가 기본이다({@code application-test.yml}). 하지만 라벨 대소문자 정합(`D-94`)은 <b>MySQL의
 * {@code utf8mb4_0900_ai_ci}가 대소문자를 접는다는 사실 자체가 검증 대상</b>이라, H2의 {@code MODE=MySQL}로는 재현되지
 * 않는다 — 문법만 흉내 낼 뿐 collation을 따라 하지 않아 유니크 제약도 이름 조회도 통과해 버린다.
 *
 * <p>컨테이너를 스프링 빈으로 두어 컨텍스트 종료 시점과 컨테이너 종료 시점을 맞춘다. {@link ServiceConnection}이 등록하는
 * 접속 정보가 {@code spring.datasource.url}의 H2 설정을 이긴다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class MySqlContainerConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}
