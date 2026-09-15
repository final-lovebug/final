package com.ubidict.backend.member.infra.security;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * OAuth2 authorization request를 Redis에 저장한다.
 *
 * <p>기본 {@code HttpSessionOAuth2AuthorizationRequestRepository}는 요청을 세션에 저장한다. 배포 환경에서
 * OAuth 시작 요청과 callback 요청이 서로 다른 인스턴스로 가면 세션을 찾지 못하므로, callback의 state를
 * Redis 키로 사용해 인스턴스 간 공유한다.
 */
@Slf4j
@Repository
public class RedisOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final Duration TTL = Duration.ofMinutes(5);
    private static final String KEY_FORMAT = "auth:oauth-authorization:%s";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisOAuth2AuthorizationRequestRepository(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Assert.notNull(request, "request cannot be null");
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (!StringUtils.hasText(state)) {
            return null;
        }

        String serialized = redisTemplate.opsForValue().get(key(state));
        return deserialize(serialized, state);
    }

    @Override
    public void saveAuthorizationRequest(
            OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        String state = authorizationRequest.getState();
        Assert.hasText(state, "authorizationRequest.state cannot be empty");
        try {
            redisTemplate
                    .opsForValue()
                    .set(
                            key(state),
                            objectMapper.writeValueAsString(StoredAuthorizationRequest.from(authorizationRequest)),
                            TTL);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("OAuth2 authorization request를 직렬화할 수 없습니다.", exception);
        }
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(
            HttpServletRequest request, HttpServletResponse response) {
        Assert.notNull(request, "request cannot be null");
        Assert.notNull(response, "response cannot be null");

        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (!StringUtils.hasText(state)) {
            return null;
        }

        String serialized = redisTemplate.opsForValue().getAndDelete(key(state));
        return deserialize(serialized, state);
    }

    private OAuth2AuthorizationRequest deserialize(String serialized, String state) {
        if (!StringUtils.hasText(serialized)) {
            return null;
        }
        try {
            return objectMapper
                    .readValue(serialized, StoredAuthorizationRequest.class)
                    .toAuthorizationRequest();
        } catch (JsonProcessingException exception) {
            log.warn("OAuth2 authorization request 역직렬화에 실패했습니다. state={}", state, exception);
            redisTemplate.delete(key(state));
            return null;
        }
    }

    private String key(String state) {
        return KEY_FORMAT.formatted(state);
    }

    private record StoredAuthorizationRequest(
            String authorizationUri,
            String clientId,
            String redirectUri,
            java.util.Set<String> scopes,
            String state,
            String authorizationRequestUri,
            java.util.Map<String, Object> additionalParameters,
            java.util.Map<String, Object> attributes) {

        private static StoredAuthorizationRequest from(OAuth2AuthorizationRequest request) {
            return new StoredAuthorizationRequest(
                    request.getAuthorizationUri(),
                    request.getClientId(),
                    request.getRedirectUri(),
                    request.getScopes(),
                    request.getState(),
                    request.getAuthorizationRequestUri(),
                    request.getAdditionalParameters(),
                    request.getAttributes());
        }

        private OAuth2AuthorizationRequest toAuthorizationRequest() {
            return OAuth2AuthorizationRequest.authorizationCode()
                    .authorizationUri(authorizationUri)
                    .clientId(clientId)
                    .redirectUri(redirectUri)
                    .scopes(scopes)
                    .state(state)
                    .authorizationRequestUri(authorizationRequestUri)
                    .additionalParameters(additionalParameters)
                    .attributes(attributes)
                    .build();
        }
    }
}
