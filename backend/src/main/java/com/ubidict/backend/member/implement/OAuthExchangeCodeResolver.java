package com.ubidict.backend.member.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.domain.AuthErrorCode;
import com.ubidict.backend.member.infra.security.OAuthExchangeCodeRedisRepository;
import com.ubidict.backend.member.infra.security.OAuthExchangeEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 1회용 교환 코드를 회원 정보로 바꾼다. 코드가 없으면(만료됐거나 이미 쓰인 경우)
 * {@code docs/API.md}에 따라 {@link AuthErrorCode#AUTH_TOKEN_INVALID}로 거절한다.
 */
@RequiredArgsConstructor
@Component
public class OAuthExchangeCodeResolver {

    private final OAuthExchangeCodeRedisRepository oAuthExchangeCodeRedisRepository;

    public OAuthExchangeEntry resolve(String code) {
        return oAuthExchangeCodeRedisRepository
                .findAndDelete(code)
                .orElseThrow(() -> new BusinessException(AuthErrorCode.AUTH_TOKEN_INVALID));
    }
}
