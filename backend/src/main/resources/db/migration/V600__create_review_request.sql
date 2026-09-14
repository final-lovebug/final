create table review_request (
    id           bigint       not null auto_increment,
    workspace_id bigint       not null,
    type         varchar(20)  not null,
    title        varchar(255) not null,
    description  text,
    requester_id bigint       not null,
    status       varchar(30)  not null,
    approved_at  datetime(6),
    revised_at   datetime(6),
    created_by   bigint       not null,
    created_at   datetime(6)  not null,
    updated_at   datetime(6)  not null,
    deleted_at   datetime(6),
    primary key (id)
);

create index idx_review_request_workspace_status on review_request (workspace_id, status);
create index idx_review_request_type_status on review_request (type, status);
