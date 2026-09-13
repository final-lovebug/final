package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.exception.AuthErrorCode;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.infra.security.RegistrationTokenRedisRepository;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationTokenResolverTest {

    private final RegistrationTokenRedisRepository registrationTokenRedisRepository =
            mock(RegistrationTokenRedisRepository.class);
    private final RegistrationTokenResolver registrationTokenResolver =
            new RegistrationTokenResolver(registrationTokenRedisRepository);

    @DisplayName("저장된 등록 토큰이면 신원 정보를 반환한다.")
    @Test
    void resolve() {
        // given
        OAuthExchangeEntry entry = new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1");
        given(registrationTokenRedisRepository.findAndDelete("valid-token")).willReturn(Optional.of(entry));

        // when
        OAuthExchangeEntry result = registrationTokenResolver.resolve("valid-token");

        // then
        assertThat(result).isEqualTo(entry);
    }

    @DisplayName("만료됐거나 이미 쓰인 등록 토큰이면 예외가 발생한다.")
    @Test
    void resolve_notFound() {
        // given
        given(registrationTokenRedisRepository.findAndDelete("invalid-token")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> registrationTokenResolver.resolve("invalid-token"))
                .isInstanceOfSatisfying(BusinessException.class, e -> assertThat(e.errorCode())
                        .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID));
    }
}
