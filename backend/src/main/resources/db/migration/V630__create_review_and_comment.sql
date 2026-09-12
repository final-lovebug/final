create table review (
    id                bigint      not null auto_increment,
    review_request_id bigint      not null,
    member_id         bigint      not null,
    target_round      int         not null,
    verdict           varchar(30) not null,
    submitted_at      datetime(6) not null,
    created_by        bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_review_review_request foreign key (review_request_id) references review_request (id)
);

create index idx_review_request_member_submitted
    on review (review_request_id, member_id, submitted_at desc);

create table comment (
    id             bigint  not null auto_increment,
    review_id      bigint  not null,
    author_id      bigint  not null,
    content        text    not null,
    start_offset   int,
    end_offset     int,
    target_item_id bigint,
    parent_id      bigint,
    resolved       boolean not null,
    created_by     bigint  not null,
    created_at     datetime(6) not null,
    updated_at     datetime(6) not null,
    deleted_at     datetime(6),
    primary key (id),
    constraint fk_comment_review foreign key (review_id) references review (id),
    constraint fk_comment_parent foreign key (parent_id) references comment (id),
    constraint ck_comment_anchor check (
        (start_offset is null and end_offset is null)
        or (start_offset is not null and end_offset is not null and start_offset >= 0 and start_offset <= end_offset)
    )
);

create index idx_comment_review on comment (review_id, created_at, id);
create index idx_comment_parent on comment (parent_id);
