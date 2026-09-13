package com.ubidict.backend.member.infra;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * {@code member.email}/{@code member.display_name} 컬럼을 AES-256-GCM으로 암·복호화한다
 * (개인정보 처리 방침, 9/13). {@code app.member.encryption-key}(환경변수 {@code MEMBER_ENCRYPTION_KEY})
 * 문자열을 SHA-256으로 해시해 256비트 키를 만든다 — {@code JwtProvider}가 JWT 서명 키를 만드는
 * 방식과 같은 패턴이다.
 *
 * <p><b>일부러 결정적(deterministic) 암호화를 쓴다.</b> GCM의 nonce(IV)를 무작위로 뽑지 않고
 * 평문의 HMAC-SHA256을 잘라 만든다 — 그래야 같은 평문이 항상 같은 암호문이 되어
 * {@code uk_member_email} 유니크 제약과 {@code existsByEmail}/{@code findByProviderAndProviderId}
 * 같은 동등 비교 조회가 암호화 이후에도 그대로 동작한다. 대가로 같은 이메일을 가진 두 행이
 * 있다는 사실 자체는 암호문 비교로 드러난다(빈도 분석 가능) — 유니크 제약을 유지하기 위한
 * 의도적인 트레이드오프다.
 *
 * <p>JPA가 {@link EncryptedStringConverter}를 리플렉션으로 직접 생성해 Spring 빈 주입이
 * 안 되므로, 이 클래스가 대신 Spring 빈으로 키를 주입받고 정적 인스턴스로 다리를 놓는다.
 */
@Component
public class MemberFieldEncryptor {

    private static final String AES_ALGORITHM = "AES/GCM/NoPadding";
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;

    private static volatile MemberFieldEncryptor instance;

    private final SecretKey key;

    public MemberFieldEncryptor(@Value("${app.member.encryption-key}") String secret) {
        this.key = deriveKey(secret);
    }

    @PostConstruct
    void register() {
        instance = this;
    }

    static MemberFieldEncryptor instance() {
        MemberFieldEncryptor current = instance;
        if (current == null) {
            throw new IllegalStateException("MemberFieldEncryptor가 아직 초기화되지 않았습니다 — Spring 컨텍스트에 이 빈이 포함돼 있는지 확인하세요"
                    + "(@DataJpaTest 등 슬라이스 테스트는 명시적으로 @Import해야 합니다).");
        }
        return current;
    }

    public String encrypt(String plainText) {
        try {
            byte[] nonce = deriveNonce(plainText);
            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            byte[] combined = new byte[nonce.length + cipherBytes.length];
            System.arraycopy(nonce, 0, combined, 0, nonce.length);
            System.arraycopy(cipherBytes, 0, combined, nonce.length, cipherBytes.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("개인정보 컬럼 암호화 중 오류가 발생했습니다.", e);
        }
    }

    public String decrypt(String encoded) {
        try {
            byte[] combined = Base64.getDecoder().decode(encoded);
            byte[] nonce = Arrays.copyOfRange(combined, 0, GCM_NONCE_LENGTH);
            byte[] cipherBytes = Arrays.copyOfRange(combined, GCM_NONCE_LENGTH, combined.length);

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce));
            return new String(cipher.doFinal(cipherBytes), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("개인정보 컬럼 복호화 중 오류가 발생했습니다.", e);
        }
    }

    private byte[] deriveNonce(String plainText) throws GeneralSecurityException {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(key.getEncoded(), HMAC_ALGORITHM));
        byte[] hash = mac.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Arrays.copyOf(hash, GCM_NONCE_LENGTH);
    }

    private static SecretKey deriveKey(String secret) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = sha256.digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(keyBytes, "AES");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("암호화 키 생성 중 오류가 발생했습니다.", e);
        }
    }
}
