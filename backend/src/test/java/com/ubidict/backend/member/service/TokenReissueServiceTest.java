package com.ubidict.backend.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.member.domain.MemberRole;
import com.ubidict.backend.member.implement.TokenPair;
import com.ubidict.backend.member.implement.TokenRefresher;
import com.ubidict.backend.member.service.model.TokenPairResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TokenReissueServiceTest {

    private final TokenRefresher tokenRefresher = mock(TokenRefresher.class);
    private final TokenReissueService tokenReissueService = new TokenReissueService(tokenRefresher);

    @DisplayName("재발급하면 새 토큰 쌍을 반환한다.")
    @Test
    void reissue() {
        // given
        given(tokenRefresher.refresh("old-refresh-token"))
                .willReturn(new TokenPair("new-access-token", "new-refresh-token", MemberRole.REGULAR));

        // when
        TokenPairResult result = tokenReissueService.reissue("old-refresh-token");

        // then
        assertThat(result.accessToken()).isEqualTo("new-access-token");
        assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
        assertThat(result.role()).isEqualTo(MemberRole.REGULAR);
    }
}
