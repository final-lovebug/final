package com.ubidict.backend.workspace.implement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class InvitationTokenGeneratorTest {

    private final InvitationTokenGenerator tokenGenerator = new InvitationTokenGenerator();

    @DisplayName("초대 토큰은 서로 다른 64자 URL-safe 문자열이다.")
    @Test
    void generate_returnsUniqueUrlSafeToken() {
        String first = tokenGenerator.generate();
        String second = tokenGenerator.generate();

        assertThat(first).matches("[0-9a-f]{64}");
        assertThat(second).matches("[0-9a-f]{64}").isNotEqualTo(first);
    }
}
