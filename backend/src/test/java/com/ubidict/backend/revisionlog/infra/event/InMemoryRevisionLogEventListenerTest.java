package com.ubidict.backend.revisionlog.infra.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.revisionlog.service.RevisionLogEventHandler;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 인메모리 수신 어댑터가 <b>위임만 한다</b>는 것을 확인한다. SQS 어댑터가 같은 핸들러의 같은 메서드를 부르므로
 * ({@code SqsRevisionLogEventListenerTest}) 두 경로의 로직이 한 벌로 유지된다.
 */
class InMemoryRevisionLogEventListenerTest {

    private final RevisionLogEventHandler handler = mock(RevisionLogEventHandler.class);
    private final InMemoryRevisionLogEventListener listener = new InMemoryRevisionLogEventListener(handler);

    @DisplayName("사전집 발행 이벤트를 공용 핸들러에 위임한다.")
    @Test
    void on_dictionaryRevised() {
        DictionaryRevisedEvent event = new DictionaryRevisedEvent(42L, 10L, 7, OffsetDateTime.now());

        listener.on(event);

        verify(handler).handle(event);
    }

    @DisplayName("문서 직접 편집 이벤트를 공용 핸들러에 위임한다.")
    @Test
    void on_documentEdited() {
        DocumentEditedEvent event = new DocumentEditedEvent(20L, 10L, 2, OffsetDateTime.now());

        listener.on(event);

        verify(handler).handle(event);
    }

    @DisplayName("리뷰 반영 이벤트를 공용 핸들러에 위임한다.")
    @Test
    void on_reviewRequestRevised() {
        ReviewRequestRevisedEvent event =
                new ReviewRequestRevisedEvent(1L, ReviewRequestType.DOCUMENT, 30L, 3, OffsetDateTime.now());

        listener.on(event);

        verify(handler).handle(event);
    }
}
