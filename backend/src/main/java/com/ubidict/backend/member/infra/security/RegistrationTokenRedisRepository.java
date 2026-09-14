package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.member.domain.OAuthProvider;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * 신규 식별자(최초 로그인)의 닉네임 온보딩이 끝날 때까지 신원(email/provider/providerId)을
 * 담아두는 1회용 등록 토큰. {@code Member} row는 닉네임이 확정되기 전까지 만들지 않는다
 * (REQ-USR-001 9/11 번복, {@code docs/API.md} "닉네임 등록 완료"). 닉네임을 입력할 시간을
 * 줘야 해서 {@link OAuthExchangeCodeRedisRepository}(30초)보다 TTL을 길게 둔다.
 *
 * <p>{@link OAuthExchangeCodeRedisRepository}와 완전히 같은 구조(find-and-delete로 1회용
 * 보장)라 페이로드도 같은 {@link OAuthExchangeEntry}를 재사용한다.
 */
@RequiredArgsConstructor
@Repository
public class RegistrationTokenRedisRepository {

    private static final Duration TTL = Duration.ofMinutes(10);
    private static final String KEY_FORMAT = "auth:oauth-registration:%s";

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_PROVIDER = "provider";
    private static final String FIELD_PROVIDER_ID = "providerId";

    private final StringRedisTemplate redisTemplate;

    public void save(String registrationToken, OAuthExchangeEntry entry) {
        String key = key(registrationToken);
        redisTemplate
                .opsForHash()
                .putAll(
                        key,
                        Map.of(
                                FIELD_EMAIL, entry.email(),
                                FIELD_PROVIDER, entry.provider().name(),
                                FIELD_PROVIDER_ID, entry.providerId()));
        redisTemplate.expire(key, TTL);
    }

    public Optional<OAuthExchangeEntry> findAndDelete(String registrationToken) {
        String key = key(registrationToken);
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        redisTemplate.delete(key);

        return Optional.of(new OAuthExchangeEntry(
                (String) fields.get(FIELD_EMAIL), OAuthProvider.valueOf((String) fields.get(FIELD_PROVIDER)), (String)
                        fields.get(FIELD_PROVIDER_ID)));
    }

    private String key(String registrationToken) {
        return KEY_FORMAT.formatted(registrationToken);
    }
}
