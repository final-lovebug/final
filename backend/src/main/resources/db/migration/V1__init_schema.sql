-- 초기 스키마. 도메인별로 나뉘어 있던 마이그레이션 31개(V1~V900)를 하나로 합쳤다.
-- 뒤따르던 ALTER 는 전부 최종 컬럼 정의에 반영했으므로 여기 CREATE TABLE 이 곧 현재 스키마다.
-- 테이블 순서는 외래 키 의존 순서다.

-- ── member ────────────────────────────────────────────────────────────────────
-- email/display_name 은 AES-GCM 으로 암호화해 저장하므로 평문보다 길다
-- (nonce 12B + GCM 태그 16B + Base64, 개인정보 처리 방침 9/13). 그래서 VARCHAR(500) 이다.
CREATE TABLE member (
    id BIGINT NOT NULL AUTO_INCREMENT,
    email VARCHAR(500) NOT NULL,
    display_name VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL,
    role VARCHAR(20) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    provider_id VARCHAR(191) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    deleted_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_member_email UNIQUE (email),
    CONSTRAINT uk_member_provider UNIQUE (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── workspace ─────────────────────────────────────────────────────────────────
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

-- 초대 링크(토큰)로 참여자를 등록한다. 이메일 발송은 후순위이고 링크 복사 방식으로 시작한다.
-- 토큰을 전역 유일로 두는 이유는 수락 요청이 워크스페이스를 모른 채 토큰만 갖고 오기 때문이다.
create table invitation (
    id                       bigint       not null auto_increment,
    workspace_id             bigint       not null,
    -- 링크 복사 방식이면 null이다. 이메일 발송이 붙으면 채워진다.
    invitee_email            varchar(320),
    token                    varchar(64)  not null,
    -- 수락 시 부여할 권한. ADMIN / REGULAR만 온다 — Owner는 초대로 부여하지 않는다.
    permission               varchar(20)  not null,
    status                   varchar(20)  not null,
    expires_at               datetime(6)  not null,
    accepted_at              datetime(6),
    accepted_participant_id  bigint,
    created_by               bigint       not null,
    created_at               datetime(6)  not null,
    updated_at               datetime(6)  not null,
    deleted_at               datetime(6),
    primary key (id),
    constraint fk_invitation_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_invitation_token unique (token)
);

-- 「같은 워크스페이스·같은 대상에 대기 상태 초대는 1개」는 부분 유니크가 필요해
-- MySQL 8.4에서 DB로 지킬 수 없다. 애플리케이션에서 검증한다.
create index idx_invitation_workspace_status on invitation (workspace_id, status);

-- ── document ──────────────────────────────────────────────────────────────────
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
    id                    bigint       not null auto_increment,
    document_id           bigint       not null,
    version_no            int          not null,
    -- 문서 본문의 유일한 저장 위치.
    -- text는 65,535바이트이고 본문 상한은 10,000자다. UTF-8 한글 3바이트 기준 최대 30,000바이트라 여유가 있다.
    body                  text         not null,
    published_at          datetime(6)  not null,
    -- 이 버전이 통과한 사전집 버전. v1(업로드본)은 대조 전이므로 null이다.
    -- outdated는 이 값과 활성 사전집 버전의 비교로 판정하며 따로 저장하지 않는다.
    dictionary_version_no int,
    created_by            bigint       not null,
    created_at            datetime(6)  not null,
    updated_at            datetime(6)  not null,
    -- 직접 편집으로 만들어진 버전인지. 대조 결과 반영본은 0이다.
    edited                tinyint(1)   not null default 0,
    -- 삭제 경로는 없지만 BaseEntity 공통 규약을 따른다.
    deleted_at            datetime(6),
    primary key (id),
    constraint fk_document_version_document foreign key (document_id) references document (id),
    constraint uk_document_version unique (document_id, version_no)
);

-- 라벨은 워크스페이스가 소유한다. 이름을 워크스페이스 안에서 유일하게 두어,
-- 오타로 같은 뜻의 라벨이 갈라지는 것을 DB가 막는다.
create table label (
    id           bigint      not null auto_increment,
    workspace_id bigint      not null,
    name         varchar(20) not null,
    created_by   bigint      not null,
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null,
    deleted_at   datetime(6),
    primary key (id),
    constraint fk_label_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_label_workspace_name unique (workspace_id, name)
);

create table document_label (
    id          bigint      not null auto_increment,
    document_id bigint      not null,
    label_id    bigint      not null,
    created_by  bigint      not null,
    created_at  datetime(6) not null,
    updated_at  datetime(6) not null,
    deleted_at  datetime(6),
    primary key (id),
    constraint fk_document_label_document foreign key (document_id) references document (id),
    constraint fk_document_label_label foreign key (label_id) references label (id),
    constraint uk_document_label unique (document_id, label_id)
);

create index idx_document_label_label on document_label (label_id);

-- ── dictionary ────────────────────────────────────────────────────────────────
-- 사전집 행 하나가 확정된 버전 하나다. 워크스페이스에 행이 쌓이고 활성중인 행 하나가 가장 최근 확정본이다.
-- 버전(version_no, published_at)은 값 객체라 별도 테이블 없이 컬럼으로 둔다.
create table dictionary (
    id           bigint      not null auto_increment,
    workspace_id bigint      not null,
    version_no   int         not null,
    published_at datetime(6) not null,
    status       varchar(20) not null,
    created_by   bigint      not null,
    created_at   datetime(6) not null,
    updated_at   datetime(6) not null,
    deleted_at   datetime(6),
    -- "활성 사전집은 워크스페이스당 1개"를 DB로 보장하기 위한 컬럼이다.
    -- status를 그대로 유니크에 넣으면 보관 행끼리 충돌한다. MySQL은 UNIQUE에서 NULL을 서로 다른 값으로
    -- 보므로, 보관 행은 NULL이라 몇 개든 쌓이고 활성 행은 1이라 두 개째가 막힌다.
    active_flag  tinyint generated always as (case when status = 'ACTIVE' then 1 else null end) stored,
    primary key (id),
    constraint fk_dictionary_workspace foreign key (workspace_id) references workspace (id),
    constraint uk_dictionary_workspace_version unique (workspace_id, version_no),
    constraint uk_dictionary_workspace_active unique (workspace_id, active_flag)
);

-- 용어는 소속 사전집 버전과 함께 얼어붙는다. 새 버전을 반영할 때 그 버전의 용어가 통째로 새로 쌓인다.
-- 개별 수정·삭제 경로는 없지만 BaseEntity 공통 규약을 따라 deleted_at을 둔다.
create table term (
    id             bigint       not null auto_increment,
    dictionary_id  bigint       not null,
    preferred_form varchar(100) not null,
    english_name   varchar(100),
    definition     text         not null,
    created_by     bigint       not null,
    created_at     datetime(6)  not null,
    updated_at     datetime(6)  not null,
    deleted_at     datetime(6),
    primary key (id),
    constraint fk_term_dictionary foreign key (dictionary_id) references dictionary (id),
    constraint uk_term_dictionary_preferred_form unique (dictionary_id, preferred_form)
);

-- ── draft_document ────────────────────────────────────────────────────────────
create table draft_document (
    id                    bigint      not null auto_increment,
    document_id           bigint      not null,
    base_version_no       int         not null,
    draft_body            text        not null,
    status                varchar(20) not null,
    requested_by          bigint,
    created_by            bigint      not null,
    created_at            datetime(6) not null,
    updated_at            datetime(6) not null,
    deleted_at            datetime(6),
    -- 대조에 쓴 사전집 버전을 초안에 고정한다(D-93). 사전집 초안이 진행 중이어도 문서를 갱신할 수 있게 되면서,
    -- 발행 시점의 활성 버전을 찍으면 대조하지 않은 버전을 기준으로 표시하게 된다. 이 컬럼이 그 기준을 얼려 둔다.
    dictionary_version_no int,
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

create table check_job (
    id                bigint        not null auto_increment,
    document_id       bigint        not null,
    requested_by      bigint        not null,
    status            varchar(20)   not null,
    draft_document_id bigint,
    -- AI 워커 콜백 인증용 상관 식별자(D-70). 발행 시점에 만든 UUIDv4 를 담고,
    -- 콜백이 같은 값을 돌려주는지로 호출자를 확인한다.
    request_id        varchar(36)   null,
    failure_reason    varchar(1000),
    created_by        bigint        not null,
    created_at        datetime(6)   not null,
    updated_at        datetime(6)   not null,
    deleted_at        datetime(6),
    -- "문서당 진행 중 대조 작업은 1개"를 DB로 보장하기 위한 컬럼이다. 진행 중이면 1, 끝났으면 NULL 이다.
    -- 애플리케이션의 사전 검사(CheckJobCreationPolicyValidator)는 락 없는 스냅샷 읽기라 동시 요청 둘을
    -- 모두 통과시키고, 그러면 작업이 둘 생겨 AI 워커가 LLM 을 두 번 호출한다. 그 마지막 방어선이 아래 유니크다.
    -- MySQL 은 UNIQUE 에서 NULL 을 서로 다른 값으로 보므로 끝난 작업은 몇 개든 쌓인다.
    --
    -- dictionary.active_flag 와 달리 생성 컬럼이 아니다 — 같은 식을 생성 컬럼으로 두면 H2 위에서 도는
    -- 테스트가 삽입 시점에 깨진다. 값은 CheckJob.changeStatus 가 상태와 함께 채운다.
    in_progress_flag  tinyint,
    primary key (id),
    constraint fk_check_job_draft_document
        foreign key (draft_document_id) references draft_document (id),
    constraint uk_check_job_document_in_progress unique (document_id, in_progress_flag)
);

create index idx_check_job_document_status
    on check_job (document_id, status);

-- 타임아웃 스위퍼가 1분마다 도는 조회(D-77): status IN (...) AND updated_at < ?
create index idx_check_job_status_updated_at
    on check_job (status, updated_at);

-- Spring DB 커밋과 LLM 요청 SQS 발행을 원자적으로 잇는 transactional outbox.
-- 현재 모든 환경은 V1으로 새 스키마를 만들므로 별도 migration 버전을 만들지 않는다.
create table llm_job_outbox (
    id                bigint        not null auto_increment,
    job_type          varchar(30)   not null,
    job_id            bigint        not null,
    request_id        varchar(36)   not null,
    payload           text          not null,
    status            varchar(20)   not null,
    attempt_count     int           not null default 0,
    next_attempt_at   datetime(6)   not null,
    lease_token       varchar(36),
    lease_expires_at  datetime(6),
    last_error        varchar(1000),
    published_at      datetime(6),
    created_at        datetime(6)   not null,
    updated_at        datetime(6)   not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint uk_llm_job_outbox_job unique (job_type, job_id),
    constraint uk_llm_job_outbox_request unique (request_id)
);

create index idx_llm_job_outbox_dispatch
    on llm_job_outbox (status, next_attempt_at);

create index idx_llm_job_outbox_lease
    on llm_job_outbox (status, lease_expires_at);

-- ── draft_dictionary ──────────────────────────────────────────────────────────
create table draft_dictionary (
    id            bigint      not null auto_increment,
    workspace_id  bigint      not null,
    dictionary_id bigint      null,
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

create table candidate_term (
    id                    bigint       not null auto_increment,
    draft_dictionary_id   bigint       not null,
    created_by            bigint       not null,
    origin                varchar(20)  not null,
    source_term_id        bigint,
    form                  varchar(200) not null,
    proposed_definition   text,
    proposed_english_name varchar(200),
    occurrence_count      int,
    status                varchar(30)  not null,
    created_at            datetime(6)  not null,
    updated_at            datetime(6)  not null,
    deleted_at            datetime(6),
    reject_reason         text,
    merge_target_term_id  bigint,
    handled_by            bigint,
    result_term_id        bigint,
    -- 사람이 후보어를 직접 등록할 때 고르는 분류(SYNONYM·HOMOGRAPH·VARIANT).
    -- 추출이 만든 후보어에는 없어 null 을 허용한다(T-INT-11 결정 1).
    type                  varchar(20)  null,
    primary key (id),
    constraint uq_candidate_term_form unique (draft_dictionary_id, form),
    constraint fk_candidate_term_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id)
);

create index idx_candidate_term_draft_status on candidate_term (draft_dictionary_id, status);
create index idx_candidate_term_occurrence on candidate_term (draft_dictionary_id, occurrence_count);

create table candidate_term_occurred_document (
    candidate_term_id bigint not null,
    document_id       bigint not null,
    primary key (candidate_term_id, document_id),
    constraint fk_candidate_term_occurred foreign key (candidate_term_id) references candidate_term (id)
);

create table candidate_term_context_snippet (
    candidate_term_id bigint not null,
    snippet           text   not null,
    constraint fk_candidate_term_context foreign key (candidate_term_id) references candidate_term (id)
);

create table candidate_term_variant_form (
    candidate_term_id bigint       not null,
    variant_form      varchar(200) not null,
    constraint fk_candidate_term_variant_form foreign key (candidate_term_id) references candidate_term (id)
);

create table extraction_job (
    id                  bigint        not null auto_increment,
    workspace_id        bigint        not null,
    dictionary_id       bigint,
    requested_by        bigint        not null,
    status              varchar(20)   not null,
    draft_dictionary_id bigint,
    -- AI 워커 콜백 인증용 상관 식별자(D-70).
    request_id          varchar(36)   null,
    failure_reason      varchar(1000),
    created_by          bigint        not null,
    created_at          datetime(6)   not null,
    updated_at          datetime(6)   not null,
    deleted_at          datetime(6),
    -- "워크스페이스당 진행 중 추출 작업은 1개"를 DB로 보장하기 위한 컬럼이다. check_job.in_progress_flag 와 같다.
    -- 값은 ExtractionJob.changeStatus 가 상태와 함께 채운다.
    in_progress_flag    tinyint,
    primary key (id),
    constraint fk_extraction_job_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id),
    constraint uk_extraction_job_workspace_in_progress unique (workspace_id, in_progress_flag)
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

-- 타임아웃 스위퍼가 1분마다 도는 조회(D-77): status IN (...) AND updated_at < ?
create index idx_extraction_job_status_updated_at
    on extraction_job (status, updated_at);

-- ── review_request ────────────────────────────────────────────────────────────
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
    -- 낙관적 락 버전.
    version      bigint       not null default 0,
    primary key (id)
);

create index idx_review_request_workspace_status on review_request (workspace_id, status);
create index idx_review_request_type_status on review_request (type, status);

create table reviewer (
    id                bigint      not null auto_increment,
    review_request_id bigint      not null,
    member_id         bigint      not null,
    assigned_at       datetime(6) not null,
    created_by        bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_reviewer_request foreign key (review_request_id) references review_request (id)
);

create unique index uq_reviewer_request_member on reviewer (review_request_id, member_id);

create table revision_document (
    id                bigint      not null auto_increment,
    review_request_id bigint      not null,
    document_id       bigint      not null,
    base_version_no   int         not null,
    draft_document_id bigint      not null,
    proposed_body     text        not null,
    reexamine_round   int         not null default 0,
    result_version_no int,
    created_by        bigint      not null,
    created_at        datetime(6) not null,
    updated_at        datetime(6) not null,
    deleted_at        datetime(6),
    primary key (id),
    constraint fk_revision_document_request foreign key (review_request_id) references review_request (id)
);

create table revision_dictionary (
    id                  bigint      not null auto_increment,
    review_request_id   bigint      not null,
    dictionary_id       bigint,
    base_version_no     int         not null,
    draft_dictionary_id bigint      not null,
    reexamine_round     int         not null default 0,
    result_version_no   int,
    created_by          bigint      not null,
    created_at          datetime(6) not null,
    updated_at          datetime(6) not null,
    deleted_at          datetime(6),
    primary key (id),
    constraint fk_revision_dictionary_request foreign key (review_request_id) references review_request (id)
);

create index idx_revision_document_request on revision_document (review_request_id);
create index idx_revision_dictionary_request on revision_dictionary (review_request_id);

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

-- ── notification ──────────────────────────────────────────────────────────────
-- 알림. 수신자 한 명당 한 행이다 — 읽음 여부가 수신자마다 달라 행을 공유할 수 없다.
-- 다른 도메인 테이블에 FK를 걸지 않는다: 워크스페이스·회원이 소프트 삭제돼도 알림 이력은 남아야 한다.
create table notification (
    id           bigint       not null auto_increment,
    recipient_id bigint       not null,
    workspace_id bigint       not null,
    type         varchar(30)  not null,
    target_type  varchar(20)  not null,
    target_id    bigint       not null,
    title        varchar(255) not null,
    message      varchar(500) not null,
    -- 이벤트 내용에서 결정론적으로 파생한다. 아래 유니크 제약과 짝이다.
    dedupe_key   varchar(200) not null,
    -- 'read'가 MySQL 예약어라 컬럼명을 is_read로 둔다. read_at과 항상 함께 바뀐다.
    is_read      tinyint(1)   not null default 0,
    read_at      datetime(6),
    -- 알림을 유발한 행위자. 시스템 발행이면 null이라 nullable이다.
    created_by   bigint,
    created_at   datetime(6)  not null,
    updated_at   datetime(6)  not null,
    deleted_at   datetime(6),
    primary key (id)
);

-- 멱등의 근거(D-43). SQS는 at-least-once라 같은 이벤트가 두 번 도착하는 것이 정상이고,
-- 두 번째 insert가 여기서 튕겨 NFR-NTF-002의 「재수신 시 재발송 금지」가 지켜진다.
create unique index uq_notification_recipient_dedupe on notification (recipient_id, dedupe_key);

-- 목록(최신순 페이징)과 미읽음 수가 모두 이 앞쪽 컬럼을 탄다.
create index idx_notification_recipient on notification (recipient_id, workspace_id, is_read, deleted_at);

-- ── revision_log ──────────────────────────────────────────────────────────────
-- 확정된 버전의 차이를 발행 시점에 고정한다. 다른 도메인 테이블에는 FK를 걸지 않아
-- 대상이 소프트 삭제돼도 이력은 보존한다.
create table revision_log (
    id                         bigint       not null auto_increment,
    workspace_id               bigint       not null,
    target_type                varchar(20)  not null,
    target_id                  bigint       not null,
    version_no                 int          not null,
    previous_version_no        int,
    origin                     varchar(20)  not null,
    summary                    varchar(255) not null,
    added_count                int          not null,
    changed_count              int          not null,
    removed_count              int          not null,
    published_by               bigint       not null,
    published_at               datetime(6)  not null,
    grade                      varchar(20),
    affected_document_count    int          not null default 0,
    base_dictionary_version_no int,
    created_at                 datetime(6)  not null,
    updated_at                 datetime(6)  not null,
    deleted_at                 datetime(6),
    primary key (id),
    constraint uq_revision_log_target_version unique (workspace_id, target_type, target_id, version_no)
);

-- 축 필터와 대상별 최신순 타임라인 조회가 모두 이 앞쪽 컬럼을 탄다.
create index idx_revision_log_timeline on revision_log (workspace_id, target_type, target_id, published_at);

create table revision_log_entry (
    id                   bigint       not null auto_increment,
    revision_log_id      bigint       not null,
    change_type          varchar(20)  not null,
    subject              varchar(100) not null,
    subject_english_name varchar(100),
    replacement          varchar(100),
    detail               varchar(255),
    created_at           datetime(6)  not null,
    updated_at           datetime(6)  not null,
    deleted_at           datetime(6),
    primary key (id),
    constraint fk_revision_log_entry_log foreign key (revision_log_id) references revision_log (id)
);

create index idx_revision_log_entry_log on revision_log_entry (revision_log_id);
