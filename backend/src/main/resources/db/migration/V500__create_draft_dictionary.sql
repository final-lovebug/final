create table draft_dictionary (
    id            bigint      not null auto_increment,
    workspace_id  bigint      not null,
    dictionary_id bigint null,
    status        varchar(20) not null,
    created_by    bigint      not null,
    created_at    datetime(6) not null,
    updated_at    datetime(6) not null,
    deleted_at    datetime(6),
    primary key (id)
);

create index idx_draft_dictionary_dictionary on draft_dictionary (dictionary_id);
create index idx_draft_dictionary_workspace on draft_dictionary (workspace_id);

create table draft_dictionary_source_document (
    draft_dictionary_id bigint not null,
    document_id         bigint not null,
    primary key (draft_dictionary_id, document_id),
    constraint fk_dd_source_document_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id)
);
