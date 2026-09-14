package com.ubidict.backend.draftdictionary.implement;

import com.ubidict.backend.common.infra.event.InProcessEventPublisher;
import com.ubidict.backend.draftdictionary.domain.ExtractionJob;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryExtractionRequestedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 작업 접수를 같은 프로세스 안에 알린다.
 *
 * <p><b>도메인 이벤트 버스({@code EventPublisher})가 아니라 {@link InProcessEventPublisher}를 쓴다</b> — 이 이벤트의 유일한
 * 구독자는 커밋 뒤에 워커로 요청을 발행하는 리스너이고, 그 리스너는 스프링 {@code ApplicationEvent}만 받는다. 버스로 보내면 배포 프로파일에서는 이벤트가
 * SQS로 나가 버려 리스너가 호출되지 않는다.
 */
@Component
@RequiredArgsConstructor
public class ExtractionJobEventPublisher {

    private final InProcessEventPublisher eventPublisher;

    public void publishRequested(ExtractionJob extractionJob) {
        eventPublisher.publish(
                new DraftDictionaryExtractionRequestedEvent(extractionJob.getId(), OffsetDateTime.now()));
    }
}
