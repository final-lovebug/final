-- 사전집은 삭제하지 않는다. 모든 행이 보존해야 할 버전 이력이고 상태 전환만 예외다.
-- BaseEntity를 상속해 만들어진 컬럼이지만 항상 null이라, AuditableEntity로 바꾸면서 함께 지운다.
alter table dictionary
    drop column deleted_at;
