-- 확정된 버전의 차이를 발행 시점에 고정한다. 다른 도메인 테이블에는 FK를 걸지 않아
-- 대상이 소프트 삭제돼도 이력은 보존하고, 도메인 대역 간 마이그레이션 순서 의존도 만들지 않는다.
create table revision_log (
    id                         bigint       not null auto_increment,
    workspace_id               bigint       not null,
    target_type                varchar(20)  not null,
    target_id                  bigint       not null,
    version_no                 int          not null,
    previous_version_no        int,
    origin                     varchar(20)  not null,
    summary                    varchar(255) not null,
    added_count                int          not null,
    changed_count              int          not null,
    removed_count              int          not null,
    published_by               bigint       not null,
    published_at               datetime(6)  not null,
    grade                      varchar(20),
    affected_document_count    int          not null default 0,
    base_dictionary_version_no int,
    created_at                 datetime(6)  not null,
    updated_at                 datetime(6)  not null,
    deleted_at                 datetime(6),
    primary key (id),
    constraint uq_revision_log_target_version unique (workspace_id, target_type, target_id, version_no)
);

-- 축 필터와 대상별 최신순 타임라인 조회가 모두 이 앞쪽 컬럼을 탄다.
create index idx_revision_log_timeline on revision_log (workspace_id, target_type, target_id, published_at);

create table revision_log_entry (
    id                   bigint       not null auto_increment,
    revision_log_id      bigint       not null,
    change_type          varchar(20)  not null,
    subject              varchar(100) not null,
    subject_english_name varchar(100),
    replacement          varchar(100),
    detail               varchar(255),
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    deleted_at           datetime(6),
    primary key (id),
    constraint fk_revision_log_entry_log foreign key (revision_log_id) references revision_log (id)
);

create index idx_revision_log_entry_log on revision_log_entry (revision_log_id);
