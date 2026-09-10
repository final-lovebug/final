package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.member.domain.OAuthProvider;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/**
 * Google 로그인 성공 시 발급하는 1회용 교환 코드를 저장한다. 발급 후 30초 이내에만
 * 유효하고({@code docs/API.md}), 조회와 동시에 지워 재사용을 막는다({@link #findAndDelete}).
 */
@RequiredArgsConstructor
@Repository
public class OAuthExchangeCodeRedisRepository {

    private static final Duration TTL = Duration.ofSeconds(30);
    private static final String KEY_FORMAT = "auth:oauth-exchange:%s";

    private static final String FIELD_EMAIL = "email";
    private static final String FIELD_DISPLAY_NAME = "displayName";
    private static final String FIELD_PROVIDER = "provider";
    private static final String FIELD_PROVIDER_ID = "providerId";

    private final StringRedisTemplate redisTemplate;

    public void save(String code, OAuthExchangeEntry entry) {
        String key = key(code);
        redisTemplate
                .opsForHash()
                .putAll(
                        key,
                        Map.of(
                                FIELD_EMAIL, entry.email(),
                                FIELD_DISPLAY_NAME, entry.displayName(),
                                FIELD_PROVIDER, entry.provider().name(),
                                FIELD_PROVIDER_ID, entry.providerId()));
        redisTemplate.expire(key, TTL);
    }

    public Optional<OAuthExchangeEntry> findAndDelete(String code) {
        String key = key(code);
        Map<Object, Object> fields = redisTemplate.opsForHash().entries(key);
        if (fields.isEmpty()) {
            return Optional.empty();
        }
        redisTemplate.delete(key);

        return Optional.of(new OAuthExchangeEntry(
                (String) fields.get(FIELD_EMAIL),
                (String) fields.get(FIELD_DISPLAY_NAME),
                OAuthProvider.valueOf((String) fields.get(FIELD_PROVIDER)),
                (String) fields.get(FIELD_PROVIDER_ID)));
    }

    private String key(String code) {
        return KEY_FORMAT.formatted(code);
    }
}
