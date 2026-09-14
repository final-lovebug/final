package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
class RefreshTokenRedisRepositoryTest {

    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    private RefreshTokenRedisRepository refreshTokenRedisRepository;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        JwtProperties jwtProperties = new JwtProperties("secret", Duration.ofMinutes(30), Duration.ofDays(7));
        refreshTokenRedisRepository = new RefreshTokenRedisRepository(redisTemplate, jwtProperties);
    }

    @DisplayName("current 토큰을 저장하면 조회할 수 있다.")
    @Test
    void saveAndFindCurrent() {
        // given
        refreshTokenRedisRepository.saveCurrent(1L, "current-hash");

        // when & then
        assertThat(refreshTokenRedisRepository.findCurrent(1L)).contains("current-hash");
    }

    @DisplayName("grace 토큰을 저장하면 조회할 수 있다.")
    @Test
    void saveAndFindGrace() {
        // given
        refreshTokenRedisRepository.saveGrace(1L, "grace-hash");

        // when & then
        assertThat(refreshTokenRedisRepository.findGrace(1L)).contains("grace-hash");
    }

    @DisplayName("저장된 적 없는 회원의 토큰을 조회하면 비어 있다.")
    @Test
    void findCurrent_notSaved() {
        // when & then
        assertThat(refreshTokenRedisRepository.findCurrent(999L)).isEmpty();
    }

    @DisplayName("삭제하면 current와 grace 둘 다 조회되지 않는다.")
    @Test
    void deleteAll() {
        // given
        refreshTokenRedisRepository.saveCurrent(1L, "current-hash");
        refreshTokenRedisRepository.saveGrace(1L, "grace-hash");

        // when
        refreshTokenRedisRepository.deleteAll(1L);

        // then
        assertThat(refreshTokenRedisRepository.findCurrent(1L)).isEmpty();
        assertThat(refreshTokenRedisRepository.findGrace(1L)).isEmpty();
    }

    @DisplayName("같은 회원의 current를 다시 저장하면 새 값으로 덮어써진다(단일 세션).")
    @Test
    void saveCurrent_overwrites() {
        // given
        refreshTokenRedisRepository.saveCurrent(1L, "old-hash");

        // when
        refreshTokenRedisRepository.saveCurrent(1L, "new-hash");

        // then
        assertThat(refreshTokenRedisRepository.findCurrent(1L)).contains("new-hash");
    }
}
