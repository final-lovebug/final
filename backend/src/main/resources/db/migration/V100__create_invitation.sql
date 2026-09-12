-- 초대 링크(토큰)로 참여자를 등록한다. 이메일 발송은 후순위이고 링크 복사 방식으로 시작한다.
-- 토큰을 전역 유일로 두는 이유는 수락 요청이 워크스페이스를 모른 채 토큰만 갖고 오기 때문이다.
create table invitation (
    id                       bigint       not null auto_increment,
    workspace_id             bigint       not null,
    -- 링크 복사 방식이면 null이다. 이메일 발송이 붙으면 채워진다.
    invitee_email            varchar(320),
    token                    varchar(64)  not null,
    -- 수락 시 부여할 권한. ADMIN / REGULAR만 온다 — Owner는 초대로 부여하지 않는다.
    permission               varchar(20)  not null,
    status                   varchar(20)  not null,
    expires_at               datetime(6)  not null,
    accepted_at              datetime(6),
    accepted_participant_id  bigint,
    created_by               bigint       not null,
    created_at               datetime(6)  not null,
    updated_at               datetime(6)  not null,
    deleted_at               datetime(6),
    primary key (id),
    constraint fk_invitation_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_invitation_token unique (token)
);

-- 「같은 워크스페이스·같은 대상에 대기 상태 초대는 1개」는 부분 유니크가 필요해
-- MySQL 8.4에서 DB로 지킬 수 없다. 애플리케이션에서 검증한다.
create index idx_invitation_workspace_status on invitation (workspace_id, status);
