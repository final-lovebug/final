-- 사전집 행 하나가 확정된 버전 하나다. 워크스페이스에 행이 쌓이고 활성중인 행 하나가 가장 최근 확정본이다.
-- 버전(version_no, published_at)은 값 객체라 별도 테이블 없이 컬럼으로 둔다.
create table dictionary (
    id           bigint      not null auto_increment,
    workspace_id bigint      not null,
    version_no   int         not null,
    published_at datetime(6) not null,
    status       varchar(20) not null,
    created_by   bigint      not null,
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null,
    deleted_at   datetime(6),
    -- "활성 사전집은 워크스페이스당 1개"를 DB로 보장하기 위한 컬럼이다.
    -- status를 그대로 유니크에 넣으면 보관 행끼리 충돌한다. MySQL은 UNIQUE에서 NULL을 서로 다른 값으로
    -- 보므로, 보관 행은 NULL이라 몇 개든 쌓이고 활성 행은 1이라 두 개째가 막힌다.
    active_flag  tinyint generated always as (case when status = 'ACTIVE' then 1 else null end) stored,
    primary key (id),
    constraint fk_dictionary_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_dictionary_workspace_version unique (workspace_id, version_no),
    constraint uk_dictionary_workspace_active unique (workspace_id, active_flag)
);

-- 용어는 소속 사전집 버전과 함께 얼어붙는다. 새 버전을 반영할 때 그 버전의 용어가 통째로 새로 쌓인다.
-- 개별 수정·삭제 경로가 없으므로 deleted_at을 두지 않는다.
create table term (
    id             bigint       not null auto_increment,
    dictionary_id  bigint       not null,
    preferred_form varchar(100) not null,
    english_name   varchar(100),
    definition     text         not null,
    created_by     bigint       not null,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    primary key (id),
    constraint fk_term_dictionary foreign key (dictionary_id) references dictionary (id),
    constraint uk_term_dictionary_preferred_form unique (dictionary_id, preferred_form)
);
