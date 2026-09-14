-- AuditableEntity 제거에 따라 문서 라벨 엔티티도 BaseEntity의 공통 규약을 사용한다.
-- 기존 행과 삭제 유스케이스가 없는 엔티티의 값은 null로 유지한다.
alter table label
    add column deleted_at datetime(6);

alter table document_label
    add column deleted_at datetime(6);
