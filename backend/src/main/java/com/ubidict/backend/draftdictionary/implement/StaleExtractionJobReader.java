package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.ExtractionJobStatus;
import com.ubidict.backend.draftdictionary.infra.ExtractionJobRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 제한 시간이 지나도록 끝나지 않은 추출 작업을 찾는다(D-73). */
@Component
@RequiredArgsConstructor
public class StaleExtractionJobReader {

    private final ExtractionJobRepository extractionJobRepository;

    public List<ExtractionJob> readStale(Duration timeout) {
        return extractionJobRepository.findAllByStatusInAndUpdatedAtBeforeAndDeletedAtIsNull(
                EnumSet.of(ExtractionJobStatus.PENDING, ExtractionJobStatus.RUNNING),
                OffsetDateTime.now().minus(timeout));
    }
}
