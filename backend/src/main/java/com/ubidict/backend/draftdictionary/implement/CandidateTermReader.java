package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.exception.DraftDictionaryErrorCode;
import com.ubidict.backend.draftdictionary.infra.CandidateTermRepository;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermSearchQuery;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CandidateTermReader {
    private final CandidateTermRepository repository;

    public CandidateTerm read(Long id) {
        return repository
                .findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() ->
                        new BusinessException(DraftDictionaryErrorCode.DRAFT_DICTIONARY_CANDIDATE_TERM_NOT_FOUND));
    }

    public PageResult<CandidateTermResult> search(CandidateTermSearchQuery q) {
        String[] sortParts = q.sort().split(",");
        Sort.Direction direction = Sort.Direction.fromString(sortParts[1]);
        PageRequest pageRequest = PageRequest.of(q.page(), q.size(), Sort.by(direction, sortParts[0]));
        Page<CandidateTerm> page =
                repository.search(q.draftDictionaryId(), q.status(), q.form(), q.minOccurrenceCount(), pageRequest);
        return new PageResult<>(
                page.getContent().stream().map(CandidateTermResult::from).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements());
    }

    public List<CandidateTerm> readAll(Long draftDictionaryId) {
        return repository.findAllByDraftDictionaryIdAndDeletedAtIsNull(draftDictionaryId);
    }
}
