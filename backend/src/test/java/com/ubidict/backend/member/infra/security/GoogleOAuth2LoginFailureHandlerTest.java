package com.ubidict.backend.member.infra.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;

class GoogleOAuth2LoginFailureHandlerTest {

    private final GoogleOAuth2LoginFailureHandler failureHandler =
            new GoogleOAuth2LoginFailureHandler("http://localhost:3000/oauth/callback");

    @DisplayName("Google 로그인에 실패하면 error 파라미터를 붙여 프론트엔드로 리다이렉트한다.")
    @Test
    void onAuthenticationFailure() throws Exception {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        failureHandler.onAuthenticationFailure(request, response, new BadCredentialsException("denied"));

        // then
        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:3000/oauth/callback?error=oauth_failed");
    }
}
