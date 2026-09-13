package com.ubidict.backend.member.infra;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemberFieldEncryptorTest {

    private final MemberFieldEncryptor memberFieldEncryptor = new MemberFieldEncryptor("test-encryption-key");

    @DisplayName("암호화한 값을 복호화하면 원래 평문이 나온다.")
    @Test
    void encryptAndDecrypt() {
        // given
        String plainText = "member@example.com";

        // when
        String encrypted = memberFieldEncryptor.encrypt(plainText);
        String decrypted = memberFieldEncryptor.decrypt(encrypted);

        // then
        assertThat(encrypted).isNotEqualTo(plainText);
        assertThat(decrypted).isEqualTo(plainText);
    }

    @DisplayName("같은 평문은 항상 같은 암호문이 된다(결정적 암호화 — 유니크 제약·동등 조회 지원 목적).")
    @Test
    void encrypt_isDeterministic() {
        // given
        String plainText = "member@example.com";

        // when
        String first = memberFieldEncryptor.encrypt(plainText);
        String second = memberFieldEncryptor.encrypt(plainText);

        // then
        assertThat(first).isEqualTo(second);
    }

    @DisplayName("다른 평문은 다른 암호문이 된다.")
    @Test
    void encrypt_differentPlainTextsProduceDifferentCipherTexts() {
        assertThat(memberFieldEncryptor.encrypt("member1@example.com"))
                .isNotEqualTo(memberFieldEncryptor.encrypt("member2@example.com"));
    }

    @DisplayName("한글 등 멀티바이트 문자도 정상적으로 왕복한다.")
    @Test
    void encryptAndDecrypt_multiByteCharacters() {
        // given
        String plainText = "닉네임";

        // when
        String decrypted = memberFieldEncryptor.decrypt(memberFieldEncryptor.encrypt(plainText));

        // then
        assertThat(decrypted).isEqualTo(plainText);
    }
}
