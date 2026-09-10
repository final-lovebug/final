package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.member.domain.OAuthProvider;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

class GoogleOAuth2LoginSuccessHandlerTest {

    private final OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository =
            mock(OAuthExchangeCodeRedisRepository.class);
    private final GoogleOAuth2LoginSuccessHandler successHandler = new GoogleOAuth2LoginSuccessHandler(
            oAuthExchangeCodeRedisRepository, "http://localhost:3000/oauth/callback");

    @DisplayName("Google 로그인에 성공하면 교환 코드를 저장하고 프론트엔드로 리다이렉트한다.")
    @Test
    void onAuthenticationSuccess() throws Exception {
        // given
        OAuth2User oAuth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("email", "member@example.com", "name", "member1", "sub", "google-1"),
                "sub");
        OAuth2AuthenticationToken authentication =
                new OAuth2AuthenticationToken(oAuth2User, oAuth2User.getAuthorities(), "google");

        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        successHandler.onAuthenticationSuccess(request, response, authentication);

        // then
        verify(oAuthExchangeCodeRedisRepository)
                .save(
                        anyString(),
                        eq(new OAuthExchangeEntry("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1")));
        assertThat(response.getRedirectedUrl()).startsWith("http://localhost:3000/oauth/callback?code=");
    }
}
