create table draft_document (
    id              bigint      not null auto_increment,
    document_id     bigint      not null,
    base_version_no int         not null,
    draft_body      text        not null,
    status          varchar(20) not null,
    requested_by    bigint,
    created_by      bigint      not null,
    created_at      datetime(6) not null,
    updated_at      datetime(6) not null,
    deleted_at      datetime(6),
    primary key (id)
);

create index idx_draft_document_document on draft_document (document_id);

create table suggestion_term (
    id                bigint       not null auto_increment,
    draft_document_id bigint       not null,
    start_offset      int          not null,
    end_offset        int          not null,
    origin_term       varchar(255) not null,
    suggestion_term   varchar(255) not null,
    status            varchar(20)  not null,
    handled_by        bigint,
    reject_reason     text,
    created_by        bigint       not null,
    created_at        datetime(6)  not null,
    updated_at        datetime(6)  not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_suggestion_term_draft_document
        foreign key (draft_document_id) references draft_document (id)
);

create index idx_suggestion_term_draft_document_status
    on suggestion_term (draft_document_id, status);
