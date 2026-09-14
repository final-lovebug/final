package com.ubidict.backend.draftdocument.implement;

import com.ubidict.backend.draftdocument.domain.CheckJob;
import com.ubidict.backend.draftdocument.domain.CheckJobStatus;
import com.ubidict.backend.draftdocument.infra.CheckJobRepository;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** 제한 시간이 지나도록 끝나지 않은 대조 작업을 찾는다(D-77). */
@Component
@RequiredArgsConstructor
public class StaleCheckJobReader {

    private final CheckJobRepository checkJobRepository;

    public List<CheckJob> readStale(Duration timeout) {
        return checkJobRepository.findAllByStatusInAndUpdatedAtBeforeAndDeletedAtIsNull(
                EnumSet.of(CheckJobStatus.PENDING, CheckJobStatus.RUNNING),
                OffsetDateTime.now().minus(timeout));
    }
}
