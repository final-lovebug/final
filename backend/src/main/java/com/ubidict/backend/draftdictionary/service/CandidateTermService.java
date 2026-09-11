package com.ubidict.backend.draftdictionary.service;

import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.draftdictionary.implement.CandidateTermReader;
import com.ubidict.backend.draftdictionary.implement.CandidateTermWriter;
import com.ubidict.backend.draftdictionary.service.model.AddCandidateTermCommand;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermResult;
import com.ubidict.backend.draftdictionary.service.model.CandidateTermSearchQuery;
import com.ubidict.backend.draftdictionary.service.model.EditCandidateTermCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CandidateTermService {
    private final CandidateTermReader reader;
    private final CandidateTermWriter writer;

    @Transactional
    public CandidateTermResult add(AddCandidateTermCommand c) {
        return CandidateTermResult.from(writer.add(c));
    }

    @Transactional(readOnly = true)
    public CandidateTermResult read(Long id) {
        return CandidateTermResult.from(reader.read(id));
    }

    @Transactional
    public CandidateTermResult edit(EditCandidateTermCommand c) {
        return CandidateTermResult.from(writer.edit(reader.read(c.candidateTermId()), c));
    }

    @Transactional
    public void delete(Long id) {
        reader.read(id).delete();
    }

    @Transactional(readOnly = true)
    public PageResult<CandidateTermResult> search(CandidateTermSearchQuery q) {
        return reader.search(q);
    }
}
