package com.ubidict.backend.revisionlog.infra.event;

import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.revisionlog.service.RevisionLogEventHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/** 인메모리 수신은 애노테이션만 맡고 실제 처리는 공용 핸들러에 위임한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "in-memory", matchIfMissing = true)
public class InMemoryRevisionLogEventListener {

    private final RevisionLogEventHandler handler;

    @Async
    @TransactionalEventListener
    public void on(DictionaryRevisedEvent event) {
        handler.handle(event);
    }

    @Async
    @TransactionalEventListener
    public void on(DocumentEditedEvent event) {
        handler.handle(event);
    }

    @Async
    @TransactionalEventListener
    public void on(ReviewRequestRevisedEvent event) {
        handler.handle(event);
    }
}
