create table workspace (
    id                                 bigint       not null auto_increment,
    name                               varchar(50)  not null,
    required_document_reviewer_count   int          not null default 0,
    required_dictionary_reviewer_count int          not null default 0,
    created_by                         bigint       not null,
    created_at                         datetime(6)  not null,
    updated_at                         datetime(6)  not null,
    deleted_at                         datetime(6),
    primary key (id)
);

create table participant (
    id           bigint      not null auto_increment,
    workspace_id bigint      not null,
    member_id    bigint      not null,
    permission   varchar(20) not null,
    joined_at    datetime(6) not null,
    created_by   bigint      not null,
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null,
    deleted_at   datetime(6),
    primary key (id),
    constraint fk_participant_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_participant_workspace_member unique (workspace_id, member_id)
);

create index idx_participant_member on participant (member_id);
