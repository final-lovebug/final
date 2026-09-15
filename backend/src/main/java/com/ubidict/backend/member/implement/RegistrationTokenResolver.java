package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import com.ubidict.backend.member.infra.security.RegistrationTokenRedisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 등록 토큰을 신원 정보로 바꾼다. 토큰이 없으면(만료됐거나 이미 쓰인 경우)
 * {@link AuthErrorCode#AUTH_TOKEN_INVALID}로 거절한다 — {@link OAuthExchangeCodeResolver}와
 * 동일한 정책({@code docs/API.md}).
 */
@RequiredArgsConstructor
@Component
public class RegistrationTokenResolver {

    private final RegistrationTokenRedisRepository registrationTokenRedisRepository;

    public OAuthExchangeEntry resolve(String registrationToken) {
        return registrationTokenRedisRepository
                .findAndDelete(registrationToken)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID));
    }
}
