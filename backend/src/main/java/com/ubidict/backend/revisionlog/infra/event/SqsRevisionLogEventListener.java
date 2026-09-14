package com.ubidict.backend.revisionlog.infra.event;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.MessagingErrorCode;
import com.ubidict.backend.common.infra.event.sqs.EventEnvelope;
import com.ubidict.backend.common.infra.event.sqs.EventEnvelopeCodec;
import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.document.domain.event.DocumentEditedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.revisionlog.service.RevisionLogEventHandler;
import io.awspring.cloud.sqs.annotation.SqsListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * SQS 수신 어댑터(D-24). {@link InMemoryRevisionLogEventListener}와 마찬가지로 <b>수신만 담당하고 처리는 공용 핸들러에 위임한다</b> —
 * 두 경로가 같은 로직을 쓴다는 것이 {@code ARCHITECTURE.md} 규약의 요점이다.
 *
 * <p>본문을 {@code String}으로 받는 이유 — spring-cloud-aws 기본 컨버터가 쓰는 {@code ObjectMapper}에 기대지 않고
 * {@link EventEnvelopeCodec}이 애플리케이션의 것으로 직접 푼다. 발행 측과 같은 규칙이 보장된다.
 *
 * <p>SQS는 at-least-once라 같은 메시지가 두 번 온다. 핸들러가 멱등이므로({@code (workspaceId, targetType, targetId,
 * versionNo)} 유니크, D-58) 재수신이 개정 이력을 늘리지 않는다.
 *
 * <p><b>구독하지 않는 이벤트는 조용히 넘긴다.</b> 이 큐에는 7개 도메인의 이벤트가 모두 흐르고 개정 이력이 반응하는 것은 셋뿐이다. 예외를 던지면 남의 이벤트 때문에
 * 메시지가 재시도되다 DLQ로 간다.
 *
 * <p>{@code type == DICTIONARY}인 {@code ReviewRequestRevisedEvent}를 걸러내는 일은 <b>여기서 하지 않는다</b> — 핸들러가 판단한다.
 * 수신 기술이 도메인 규칙을 알면 인메모리 경로와 갈라진다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.messaging.mode", havingValue = "sqs")
public class SqsRevisionLogEventListener {

    private final EventEnvelopeCodec codec;
    private final RevisionLogEventHandler handler;

    @SqsListener("${app.messaging.sqs.queue}")
    public void on(String message) {
        EventEnvelope envelope = codec.decode(message);

        SubscribedEvents.find(envelope.eventType())
                .map(type -> codec.payloadAs(envelope, type))
                .ifPresentOrElse(
                        this::dispatch,
                        () -> log.debug(
                                "[SqsRevisionLogEventListener.on] Event not subscribed. eventType={}",
                                envelope.eventType()));
    }

    private void dispatch(DomainEvent event) {
        switch (event) {
            case DictionaryRevisedEvent revised -> handler.handle(revised);
            case DocumentEditedEvent edited -> handler.handle(edited);
            case ReviewRequestRevisedEvent revised -> handler.handle(revised);
            default ->
                throw new BusinessException(
                        MessagingErrorCode.MESSAGING_EVENT_HANDLER_MISSING,
                        "구독 목록에 있으나 처리 분기 없음. eventType=" + event.getClass().getSimpleName());
        }
    }
}
