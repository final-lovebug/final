package com.ubidict.backend.member.domain;

/**
 * 회원의 사이트 레벨 권한.
 *
 * <p>워크스페이스 단위 권한인 {@code Workspace.Participant.permission}
 * (Owner/Admin/Regular)과는 범위가 다른 별개의 enum이다.
 */
public enum MemberRole {
    REGULAR,
    ADMIN
}
