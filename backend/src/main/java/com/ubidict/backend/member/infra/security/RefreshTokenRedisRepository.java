package com.ubidict.backend.member.infra.security;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 리프레시 토큰(해시값)을 회원 단위로 저장한다. {@code current}는 현재 유효한 토큰,
 * {@code grace}는 재발급으로 막 교체된 직전 토큰이다 — 재발급 로직(rotation, 10초
 * 유예 판단)은 이 클래스가 아니라 상위 implement 계층이 담당한다.
 *
 * <p>새 로그인 시 {@code saveCurrent}를 그대로 호출해 덮어쓰는 것만으로 "계정당
 * 활성 세션 1개" 정책이 성립한다 — 별도의 세션 목록 관리가 필요 없다.
 */
@RequiredArgsConstructor
@Repository
public class RefreshTokenRedisRepository {

    private static final Duration GRACE_TTL = Duration.ofSeconds(10);
    private static final String KEY_FORMAT = "auth:refresh:%d:%s";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public void saveCurrent(Long memberId, String tokenHash) {
        redisTemplate.opsForValue().set(currentKey(memberId), tokenHash, jwtProperties.refreshTokenValidity());
    }

    public void saveGrace(Long memberId, String tokenHash) {
        redisTemplate.opsForValue().set(graceKey(memberId), tokenHash, GRACE_TTL);
    }

    public Optional<String> findCurrent(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(currentKey(memberId)));
    }

    public Optional<String> findGrace(Long memberId) {
        return Optional.ofNullable(redisTemplate.opsForValue().get(graceKey(memberId)));
    }

    public void deleteAll(Long memberId) {
        redisTemplate.delete(currentKey(memberId));
        redisTemplate.delete(graceKey(memberId));
    }

    private String currentKey(Long memberId) {
        return KEY_FORMAT.formatted(memberId, "current");
    }

    private String graceKey(Long memberId) {
        return KEY_FORMAT.formatted(memberId, "grace");
    }
}
