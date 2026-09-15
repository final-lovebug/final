package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ExtractionJobWriter {

    private final ExtractionJobRepository extractionJobRepository;

    public ExtractionJob append(Long workspaceId, Long dictionaryId, List<Long> sourceDocumentIds, Long requestedBy) {
        return extractionJobRepository.save(
                ExtractionJob.create(workspaceId, dictionaryId, sourceDocumentIds, requestedBy));
    }
}
