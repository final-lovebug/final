package com.ubidict.backend.member.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.infra.security.RegistrationTokenRedisRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RegistrationTokenIssuerTest {

    private final RegistrationTokenRedisRepository registrationTokenRedisRepository =
            mock(RegistrationTokenRedisRepository.class);
    private final RegistrationTokenIssuer registrationTokenIssuer =
            new RegistrationTokenIssuer(registrationTokenRedisRepository);

    @DisplayName("등록 토큰을 발급하고 신원 정보를 저장한다.")
    @Test
    void issue() {
        // when
        String registrationToken =
                registrationTokenIssuer.issue("member@example.com", OAuthProvider.GOOGLE, "google-1");

        // then
        assertThat(registrationToken).isNotBlank();
        verify(registrationTokenRedisRepository)
                .save(
                        eq(registrationToken),
                        eq(new OAuthExchangeEntry("member@example.com", OAuthProvider.GOOGLE, "google-1")));
    }
}
