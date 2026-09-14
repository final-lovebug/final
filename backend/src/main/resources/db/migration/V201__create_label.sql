-- 라벨은 워크스페이스가 소유한다. 이름을 워크스페이스 안에서 유일하게 두어,
-- 오타로 같은 뜻의 라벨이 갈라지는 것을 DB가 막는다.
create table label (
    id           bigint      not null auto_increment,
    workspace_id bigint      not null,
    name         varchar(20) not null,
    created_by   bigint      not null,
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null,
    primary key (id),
    constraint fk_label_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_label_workspace_name unique (workspace_id, name)
);

create table document_label (
    id          bigint      not null auto_increment,
    document_id bigint      not null,
    label_id    bigint      not null,
    created_by  bigint      not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null,
    primary key (id),
    constraint fk_document_label_document foreign key (document_id) references document (id),
    constraint fk_document_label_label foreign key (label_id) references label (id),
    constraint uk_document_label unique (document_id, label_id)
);

create index idx_document_label_label on document_label (label_id);
