package com.ubidict.backend.member.infra.security;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.member.exception.AuthErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/**
 * 리프레시 토큰을 SHA-256으로 해시한다. Redis에는 토큰 원문이 아니라 이 해시값만
 * 저장해, 저장소가 유출돼도 토큰 자체는 복구되지 않게 한다.
 *
 * <p>정상 배포 환경에서는 SHA-256을 항상 지원하므로 {@link NoSuchAlgorithmException}은
 * 사실상 발생하지 않는 방어적 케이스다({@code docs/API.md} 참고).
 */
@Component
public class RefreshTokenHasher {

    private static final String ALGORITHM = "SHA-256";

    public String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
            byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new BusinessException(AuthErrorCode.AUTH_TOKEN_HASH_FAILED, e.getMessage(), e);
        }
    }
}
