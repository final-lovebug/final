create table extraction_job (
    id                  bigint        not null auto_increment,
    workspace_id        bigint        not null,
    dictionary_id       bigint,
    requested_by        bigint        not null,
    status              varchar(20)   not null,
    draft_dictionary_id bigint,
    failure_reason      varchar(1000),
    created_by          bigint        not null,
    created_at          datetime(6)   not null,
    updated_at          datetime(6)   not null,
    deleted_at          datetime(6),
    primary key (id),
    constraint fk_extraction_job_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id)
);

create table extraction_job_source_document (
    extraction_job_id bigint not null,
    document_id       bigint not null,
    constraint fk_extraction_job_source_document_job
        foreign key (extraction_job_id) references extraction_job (id)
);

create index idx_extraction_job_workspace_status
    on extraction_job (workspace_id, status);

create index idx_extraction_job_source_document_job
    on extraction_job_source_document (extraction_job_id);
