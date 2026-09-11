-- Term이 공통 BaseEntity 규약을 따르도록 삭제 시각 컬럼을 추가한다.
-- 현재 삭제 경로는 없으므로 기존 행과 신규 행의 기본값은 null이다.
alter table term
    add column deleted_at datetime(6);
