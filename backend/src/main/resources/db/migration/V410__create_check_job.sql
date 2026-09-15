create table check_job (
    id                bigint        not null auto_increment,
    document_id       bigint        not null,
    requested_by      bigint        not null,
    status            varchar(20)   not null,
    draft_document_id bigint,
    failure_reason    varchar(1000),
    created_by        bigint        not null,
    created_at        datetime(6)   not null,
    updated_at        datetime(6)   not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_check_job_draft_document
        foreign key (draft_document_id) references draft_document (id)
);

create index idx_check_job_document_status
    on check_job (document_id, status);
