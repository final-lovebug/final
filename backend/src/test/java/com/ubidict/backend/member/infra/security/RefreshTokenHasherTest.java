package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RefreshTokenHasherTest {

    private final RefreshTokenHasher refreshTokenHasher = new RefreshTokenHasher();

    @DisplayName("같은 토큰을 해시하면 항상 같은 값이 나온다.")
    @Test
    void hash_deterministic() {
        // when
        String hash1 = refreshTokenHasher.hash("token-value");
        String hash2 = refreshTokenHasher.hash("token-value");

        // then
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 -> 32바이트 -> 16진수 64자
    }

    @DisplayName("다른 토큰을 해시하면 다른 값이 나온다.")
    @Test
    void hash_different() {
        // when
        String hash1 = refreshTokenHasher.hash("token-a");
        String hash2 = refreshTokenHasher.hash("token-b");

        // then
        assertThat(hash1).isNotEqualTo(hash2);
    }
}
