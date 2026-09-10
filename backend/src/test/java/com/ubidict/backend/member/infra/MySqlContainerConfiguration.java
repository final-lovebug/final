package com.ubidict.backend.member.infra;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * 컨테이너를 Spring 빈으로 두어 컨텍스트 종료 시점과 컨테이너 종료 시점을 맞춘다.
 */
@TestConfiguration(proxyBeanMethods = false)
class MySqlContainerConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}
