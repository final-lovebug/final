-- 사람이 후보어를 직접 등록할 때 고르는 분류(SYNONYM·HOMOGRAPH·VARIANT).
-- 추출이 만든 후보어에는 없어 null 을 허용한다(T-INT-11 결정 1).
--
-- 원래 V550 이었으나 같은 번호를 AI 워커 전환(V550__add_extraction_job_request_id)이
-- 먼저 쓰고 있어 V560 으로 옮겼다 — 두 브랜치가 각자 V550 을 집었고 파일명이 달라
-- 머지에서 충돌로 드러나지 않았다. 대역 규칙(500–599, 10 단위)은 그대로다.
alter table candidate_term add column type varchar(20) null;
