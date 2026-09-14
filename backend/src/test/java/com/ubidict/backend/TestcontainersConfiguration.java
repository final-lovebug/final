package com.ubidict.backend;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.grafana.LgtmStackContainer;
import org.testcontainers.localstack.LocalStackContainer;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    LgtmStackContainer grafanaLgtmContainer() {
        return new LgtmStackContainer(DockerImageName.parse("grafana/otel-lgtm:latest"));
    }

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }

    @Bean
    @ServiceConnection(name = "redis")
    GenericContainer<?> redisContainer() {
        return new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);
    }

    /**
     * AI 워커 요청 큐(SQS)의 로컬 대체(D-70).
     *
     * <p><b>별도 {@code @TestConfiguration}으로 떼지 않는다.</b> 떼면 「SQS가 필요한 테스트」와 아닌 테스트의 Spring 컨텍스트가 갈리고,
     * 컨텍스트가 갈리면 MySQL·Redis·LGTM까지 한 벌 더 뜬다. 컨테이너 하나를 더 띄우는 비용이 훨씬 싸다.
     *
     * <p>SQS 하나만 올리고 즉시 로딩해 기동 시간을 줄인다. {@code withReuse}는 켜지 않는다 — 머신마다 opt-in이라 CI와 로컬이 갈리고, 무엇보다
     * 큐가 실행 사이에 살아남아 지난 실행의 메시지가 다음 실행의 첫 테스트를 때린다.
     */
    @Bean
    LocalStackContainer localStackContainer() {
        return new LocalStackContainer(DockerImageName.parse("localstack/localstack:4"))
                .withEnv("SERVICES", "sqs")
                .withEnv("EAGER_SERVICE_LOADING", "1")
                .withEnv("DEBUG", "0");
    }

    /**
     * {@code @ServiceConnection}은 LocalStack을 모른다 — 접속 정보를 직접 넣는다.
     *
     * <p>큐는 미리 만들지 않고 첫 접근에 만든다(D-74). 초기화 스크립트와 리스너 컨테이너 기동 사이의 순서 경합을 없애기 위해서다.
     */
    @Bean
    DynamicPropertyRegistrar localStackProperties(LocalStackContainer localStack) {
        return registry -> {
            registry.add(
                    "spring.cloud.aws.endpoint", () -> localStack.getEndpoint().toString());
            registry.add("spring.cloud.aws.region.static", localStack::getRegion);
            registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
            registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
            registry.add("spring.cloud.aws.sqs.queue-not-found-strategy", () -> "create");
        };
    }
}
