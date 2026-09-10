package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.member.domain.OAuthProvider;
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
class OAuthExchangeCodeRedisRepositoryTest {

    @Container
    private static final GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:latest")).withExposedPorts(6379);

    private OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory connectionFactory =
                new LettuceConnectionFactory(redis.getHost(), redis.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();

        oAuthExchangeCodeRedisRepository = new OAuthExchangeCodeRedisRepository(redisTemplate);
    }

    @DisplayName("교환 코드를 저장하면 조회와 동시에 지워진다(1회용).")
    @Test
    void saveAndFindAndDelete() {
        // given
        OAuthExchangeEntry entry =
                new OAuthExchangeEntry("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        oAuthExchangeCodeRedisRepository.save("code-1", entry);

        // when
        var first = oAuthExchangeCodeRedisRepository.findAndDelete("code-1");
        var second = oAuthExchangeCodeRedisRepository.findAndDelete("code-1");

        // then
        assertThat(first).contains(entry);
        assertThat(second).isEmpty();
    }

    @DisplayName("저장된 적 없는 코드를 조회하면 비어 있다.")
    @Test
    void findAndDelete_notSaved() {
        // when & then
        assertThat(oAuthExchangeCodeRedisRepository.findAndDelete("unknown-code"))
                .isEmpty();
    }
}
