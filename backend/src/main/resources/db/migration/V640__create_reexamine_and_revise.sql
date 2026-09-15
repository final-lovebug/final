create table reexamine (
    id                bigint      not null auto_increment,
    review_request_id bigint      not null,
    round             int         not null,
    performed_by      bigint      not null,
    performed_at      datetime(6) not null,
    created_by        bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint uk_reexamine_request_round unique (review_request_id, round),
    constraint fk_reexamine_review_request foreign key (review_request_id) references review_request (id)
);

create table reexamine_addressed_comment (
    reexamine_id bigint not null,
    comment_id   bigint not null,
    constraint fk_reexamine_addressed_comment_reexamine
        foreign key (reexamine_id) references reexamine (id),
    constraint fk_reexamine_addressed_comment_comment
        foreign key (comment_id) references comment (id)
);

create table revise (
    id                bigint      not null auto_increment,
    review_request_id bigint      not null,
    result_version_no int         not null,
    performed_by      bigint      not null,
    performed_at      datetime(6) not null,
    created_by        bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint uk_revise_review_request unique (review_request_id),
    constraint fk_revise_review_request foreign key (review_request_id) references review_request (id)
);
