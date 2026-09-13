package com.ubidict.backend.draftdictionary.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.draftdictionary.domain.CandidateTerm;
import com.ubidict.backend.draftdictionary.domain.CandidateTermStatus;
import com.ubidict.backend.draftdictionary.domain.DraftDictionary;
import com.ubidict.backend.draftdictionary.domain.event.CandidateTermDecidedEvent;
import com.ubidict.backend.draftdictionary.domain.event.DraftDictionaryCreatedEvent;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DraftDictionaryEventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private DraftDictionaryEventPublisher publisher;

    @DisplayName("사전 초안 생성 이벤트에는 초안과 요청자 식별 정보가 담긴다.")
    @Test
    void publishCreated() {
        DraftDictionary draft = draft();

        publisher.publishCreated(draft);

        DraftDictionaryCreatedEvent event = capture(DraftDictionaryCreatedEvent.class);
        assertThat(event.draftDictionaryId()).isEqualTo(10L);
        assertThat(event.workspaceId()).isEqualTo(1L);
        assertThat(event.createdBy()).isEqualTo(2L);
    }

    @DisplayName("후보어 판정 이벤트에는 판정 상태와 처리자가 담긴다.")
    @Test
    void publishCandidateDecided() {
        CandidateTerm term = CandidateTerm.create(10L, "결제", "정의", null, List.of(20L), 1, List.of("문맥"), 2L);
        ReflectionTestUtils.setField(term, "id", 30L);
        term.hold(3L);

        publisher.publishCandidateDecided(term);

        CandidateTermDecidedEvent event = capture(CandidateTermDecidedEvent.class);
        assertThat(event.candidateTermId()).isEqualTo(30L);
        assertThat(event.status()).isEqualTo(CandidateTermStatus.ON_HOLD);
        assertThat(event.handledBy()).isEqualTo(3L);
    }

    private DraftDictionary draft() {
        DraftDictionary draft = DraftDictionary.create(1L, null, List.of(20L), 2L);
        ReflectionTestUtils.setField(draft, "id", 10L);
        return draft;
    }

    private <T extends DomainEvent> T capture(Class<T> type) {
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        then(eventPublisher).should().publish(captor.capture());
        return type.cast(captor.getValue());
    }
}
