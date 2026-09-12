alter table candidate_term add column reject_reason text;
alter table candidate_term add column merge_target_term_id bigint;
alter table candidate_term add column handled_by bigint;
alter table candidate_term add column result_term_id bigint;
