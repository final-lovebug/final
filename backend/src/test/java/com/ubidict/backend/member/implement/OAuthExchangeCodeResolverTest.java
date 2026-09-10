package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.security.OAuthExchangeCodeRedisRepository;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OAuthExchangeCodeResolverTest {

    private final OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository =
            mock(OAuthExchangeCodeRedisRepository.class);
    private final OAuthExchangeCodeResolver oAuthExchangeCodeResolver =
            new OAuthExchangeCodeResolver(oAuthExchangeCodeRedisRepository);

    @DisplayName("저장된 교환 코드면 회원 정보를 반환한다.")
    @Test
    void resolve() {
        // given
        OAuthExchangeEntry entry =
                new OAuthExchangeEntry("member@example.com", "member1", OAuthProvider.GOOGLE, "google-1");
        given(oAuthExchangeCodeRedisRepository.findAndDelete("valid-code")).willReturn(Optional.of(entry));

        // when
        OAuthExchangeEntry result = oAuthExchangeCodeResolver.resolve("valid-code");

        // then
        assertThat(result).isEqualTo(entry);
    }

    @DisplayName("만료됐거나 이미 쓰인 교환 코드면 예외가 발생한다.")
    @Test
    void resolve_notFound() {
        // given
        given(oAuthExchangeCodeRedisRepository.findAndDelete("invalid-code")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> oAuthExchangeCodeResolver.resolve("invalid-code"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID));
    }
}
