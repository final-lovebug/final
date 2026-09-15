package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.member.domain.OAuthProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

@Disabled("Redis 외부 저장소 테스트는 인메모리 대역 테스트로 대체한다.")
class OAuthExchangeCodeRedisRepositoryTest {

    private OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    @BeforeEach
    void setUp() {
        oAuthExchangeCodeRedisRepository = new OAuthExchangeCodeRedisRepository(null);
    }

    @DisplayName("교환 코드를 저장하면 조회와 동시에 지워진다(1회용).")
    @Test
    void saveAndFindAndDelete() {
        // given
        OAuthExchangeEntry entry = new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1");
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
