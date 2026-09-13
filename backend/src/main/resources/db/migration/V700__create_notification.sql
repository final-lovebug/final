-- 알림. 수신자 한 명당 한 행이다 — 읽음 여부가 수신자마다 달라 행을 공유할 수 없다.
-- 다른 도메인 테이블에 FK를 걸지 않는다: 워크스페이스·회원이 소프트 삭제돼도 알림 이력은 남아야 하고,
-- FK를 걸면 마이그레이션 대역을 넘나드는 순서 의존이 생긴다.
create table notification (
    id           bigint       not null auto_increment,
    recipient_id bigint       not null,
    workspace_id bigint       not null,
    type         varchar(30)  not null,
    target_type  varchar(20)  not null,
    target_id    bigint       not null,
    title        varchar(255) not null,
    message      varchar(500) not null,
    -- 이벤트 내용에서 결정론적으로 파생한다. 아래 유니크 제약과 짝이다.
    dedupe_key   varchar(200) not null,
    -- 'read'가 MySQL 예약어라 컬럼명을 is_read로 둔다. read_at과 항상 함께 바뀐다.
    is_read      tinyint(1)   not null default 0,
    read_at      datetime(6),
    -- 알림을 유발한 행위자. 시스템 발행이면 null이라 nullable이다.
    created_by   bigint,
    created_at   datetime(6)  not null,
    updated_at   datetime(6)  not null,
    deleted_at   datetime(6),
    primary key (id)
);

-- 멱등의 근거(D-43). SQS는 at-least-once라 같은 이벤트가 두 번 도착하는 것이 정상이고,
-- 두 번째 insert가 여기서 튕겨 NFR-NTF-002의 「재수신 시 재발송 금지」가 지켜진다.
create unique index uq_notification_recipient_dedupe on notification (recipient_id, dedupe_key);

-- 목록(최신순 페이징)과 미읽음 수가 모두 이 앞쪽 컬럼을 탄다.
create index idx_notification_recipient on notification (recipient_id, workspace_id, is_read, deleted_at);
