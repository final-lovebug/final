-- AI 워커 콜백 인증용 상관 식별자(D-66). 발행 시점에 만든 UUIDv4 를 담고,
-- 콜백이 같은 값을 돌려주는지로 호출자를 확인한다.
alter table extraction_job
    add column request_id varchar(36) null after draft_dictionary_id;

-- 타임아웃 스위퍼가 1분마다 도는 조회(D-73): status IN (...) AND updated_at < ?
create index idx_extraction_job_status_updated_at
    on extraction_job (status, updated_at);
