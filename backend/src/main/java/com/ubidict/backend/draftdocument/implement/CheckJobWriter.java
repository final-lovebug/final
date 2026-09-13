package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckJobWriter {

    private final CheckJobRepository checkJobRepository;

    public CheckJob append(Long documentId, Long requestedBy) {
        return checkJobRepository.save(CheckJob.create(documentId, requestedBy));
    }
}
