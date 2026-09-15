alter table review_request
    add column version bigint not null default 0;
