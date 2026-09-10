package com.ubidict.backend.member.service;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.member.implement.TokenRevoker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LogoutServiceTest {

    private final TokenRevoker tokenRevoker = mock(TokenRevoker.class);
    private final LogoutService logoutService = new LogoutService(tokenRevoker);

    @DisplayName("로그아웃하면 리프레시 토큰을 폐기한다.")
    @Test
    void logout() {
        // when
        logoutService.logout("refresh-token");

        // then
        verify(tokenRevoker).revoke("refresh-token");
    }
}
