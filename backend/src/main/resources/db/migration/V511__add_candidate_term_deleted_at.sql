alter table candidate_term add column deleted_at datetime(6);

alter table candidate_term
    add constraint fk_candidate_term_draft_dictionary
        foreign key (draft_dictionary_id) references draft_dictionary (id);
