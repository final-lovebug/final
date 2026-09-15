package com.ubidict.backend.member.implement;

import com.ubidict.backend.member.domain.OAuthProvider;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.infra.security.RegistrationTokenRedisRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 신규 식별자(최초 로그인)의 닉네임 온보딩용 1회용 등록 토큰을 발급한다
 * ({@code docs/API.md} "콜백 및 토큰 교환" — 신규 가입 분기).
 */
@RequiredArgsConstructor
@Component
public class RegistrationTokenIssuer {

    private final RegistrationTokenRedisRepository registrationTokenRedisRepository;

    public String issue(String email, OAuthProvider provider, String providerId) {
        String registrationToken = UUID.randomUUID().toString();
        registrationTokenRedisRepository.save(registrationToken, new OAuthExchangeEntry(email, provider, providerId));
        return registrationToken;
    }
}
