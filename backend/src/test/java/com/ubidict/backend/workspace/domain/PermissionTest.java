package com.ubidict.backend.workspace.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PermissionTest {

    @DisplayName("자기 자신과 같은 권한을 요구하면 충족한다.")
    @ParameterizedTest
    @CsvSource({"OWNER, OWNER", "ADMIN, ADMIN", "MEMBER, MEMBER"})
    void isAtLeast_samePermission(Permission permission, Permission required) {
        // when & then
        assertThat(permission.isAtLeast(required)).isTrue();
    }

    @DisplayName("요구 권한보다 서열이 높으면 충족한다.")
    @ParameterizedTest
    @CsvSource({"OWNER, ADMIN", "OWNER, MEMBER", "ADMIN, MEMBER"})
    void isAtLeast_higherPermission(Permission permission, Permission required) {
        // when & then
        assertThat(permission.isAtLeast(required)).isTrue();
    }

    @DisplayName("요구 권한보다 서열이 낮으면 충족하지 못한다.")
    @ParameterizedTest
    @CsvSource({"ADMIN, OWNER", "MEMBER, OWNER", "MEMBER, ADMIN"})
    void isAtLeast_lowerPermission(Permission permission, Permission required) {
        // when & then
        assertThat(permission.isAtLeast(required)).isFalse();
    }

    @DisplayName("권한 서열은 OWNER, ADMIN, MEMBER 순이다.")
    @Test
    void isAtLeast_order() {
        // when & then
        assertThat(Permission.OWNER.isAtLeast(Permission.ADMIN)).isTrue();
        assertThat(Permission.ADMIN.isAtLeast(Permission.MEMBER)).isTrue();
        assertThat(Permission.MEMBER.isAtLeast(Permission.ADMIN)).isFalse();
    }
}
