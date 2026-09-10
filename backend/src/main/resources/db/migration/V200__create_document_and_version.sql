create table document (
    id                 bigint       not null auto_increment,
    workspace_id       bigint       not null,
    title              varchar(200) not null,
    -- 현재 본문은 이 번호가 가리키는 document_version 행이다.
    -- 본문(body)을 document에 두지 않는 이유는, 두 곳에 저장하면 반영 때 둘을 함께 갱신해야 하고 어긋나기 때문이다.
    current_version_no int          not null,
    updater_id         bigint       not null,
    created_by         bigint       not null,
    created_at         datetime(6)  not null,
    updated_at         datetime(6)  not null,
    deleted_at         datetime(6),
    primary key (id),
    constraint fk_document_workspace foreign key (workspace_id) references workspace (id)
);

create index idx_document_workspace on document (workspace_id, deleted_at);

create table document_version (
    id                    bigint      not null auto_increment,
    document_id           bigint      not null,
    version_no            int         not null,
    -- 문서 본문의 유일한 저장 위치. 확정 후 불변이라 deleted_at을 두지 않는다.
    -- text는 65,535바이트이고 본문 상한은 10,000자다. UTF-8 한글 3바이트 기준 최대 30,000바이트라 여유가 있다.
    body                  text        not null,
    published_at          datetime(6) not null,
    -- 이 버전이 통과한 사전집 버전. v1(업로드본)은 대조 전이므로 null이다.
    -- outdated는 이 값과 활성 사전집 버전의 비교로 판정하며 따로 저장하지 않는다.
    dictionary_version_no int,
    created_by            bigint      not null,
    created_at            datetime(6) not null,
    updated_at            datetime(6) not null,
    primary key (id),
    constraint fk_document_version_document foreign key (document_id) references document (id),
    constraint uk_document_version unique (document_id, version_no)
);
