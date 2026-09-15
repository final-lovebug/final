-- 기존 버전은 직접 편집본이 아니므로 0으로 채운다.
-- 이후 행도 생성 경로에서 명시하지만 안전한 기본값을 유지한다.
alter table document_version
    add column edited tinyint(1) not null default 0;
