package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class RedisOAuth2AuthorizationRequestRepositoryTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private RedisOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        repository = new RedisOAuth2AuthorizationRequestRepository(redisTemplate);
    }

    @Test
    void authorizationRequest를_저장하고_조회한다() {
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();

        repository.saveAuthorizationRequest(authorizationRequest, request, response);
        org.mockito.ArgumentCaptor<String> serialized = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations)
                .set(
                        org.mockito.ArgumentMatchers.eq("auth:oauth-authorization:state-1"),
                        serialized.capture(),
                        org.mockito.ArgumentMatchers.any());
        given(request.getParameter("state")).willReturn("state-1");
        given(valueOperations.get("auth:oauth-authorization:state-1")).willReturn(serialized.getValue());

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(request);

        assertThat(loaded).isEqualTo(authorizationRequest);
    }

    @Test
    void authorizationRequest를_조회하면서_삭제한다() {
        OAuth2AuthorizationRequest authorizationRequest = authorizationRequest();
        given(request.getParameter("state")).willReturn("state-1");
        repository.saveAuthorizationRequest(authorizationRequest, request, response);
        org.mockito.ArgumentCaptor<String> serialized = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(valueOperations)
                .set(
                        org.mockito.ArgumentMatchers.eq("auth:oauth-authorization:state-1"),
                        serialized.capture(),
                        org.mockito.ArgumentMatchers.any());
        given(valueOperations.getAndDelete("auth:oauth-authorization:state-1")).willReturn(serialized.getValue());

        OAuth2AuthorizationRequest removed = repository.removeAuthorizationRequest(request, response);

        assertThat(removed).isEqualTo(authorizationRequest);
        verify(valueOperations).getAndDelete("auth:oauth-authorization:state-1");
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .clientId("client-id")
                .redirectUri("https://api.example.com/login/oauth2/code/google")
                .scope("openid", "email")
                .state("state-1")
                .build();
    }
}
