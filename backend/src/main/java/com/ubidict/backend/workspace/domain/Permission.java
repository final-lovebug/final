package com.ubidict.backend.workspace.domain;

import lombok.RequiredArgsConstructor;

/**
 * 워크스페이스 참여자의 권한 서열을 정의한다. OWNER > ADMIN > REGULAR 순이다.
 *
 * <p>서열은 선언 순서(ordinal)가 아니라 level 값으로 판단한다. 사이에 상수를 끼워 넣어도 서열이 뒤집히지 않는다.
 */
@RequiredArgsConstructor
public enum Permission {
    OWNER(3),
    ADMIN(2),
    REGULAR(1);

    private final int level;

    public boolean isAtLeast(Permission required) {
        return level >= required.level;
    }
}
