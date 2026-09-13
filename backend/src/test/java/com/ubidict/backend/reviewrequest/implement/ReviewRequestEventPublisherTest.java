package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.ubidict.backend.common.domain.event.DomainEvent;
import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.reviewrequest.domain.Review;
import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.ReviewVerdict;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCanceledEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestChangesRequestedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestCreatedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewRequestRevisedEvent;
import com.ubidict.backend.reviewrequest.domain.event.ReviewSubmittedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewRequestEventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @DisplayName("리뷰 요청 흐름의 이벤트는 식별자와 원시값만 전달한다.")
    @Test
    void publish_eventsContainImmutablePayload() {
        // given
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰", null, 1L, 1L);
        ReflectionTestUtils.setField(request, "id", 20L);
        Review review = Review.submit(20L, 2L, 0, ReviewVerdict.CHANGES_REQUESTED, 2L);
        ReflectionTestUtils.setField(review, "id", 30L);
        ReviewRequest canceledRequest = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "취소", null, 1L, 1L);
        ReflectionTestUtils.setField(canceledRequest, "id", 21L);
        ReviewRequestEventPublisher publisher = new ReviewRequestEventPublisher(eventPublisher);

        // when
        publisher.publishCreated(request, 40L);
        publisher.publishSubmitted(review);
        publisher.publishChangesRequested(request, 40L);
        request.markRevised(java.time.OffsetDateTime.now());
        publisher.publishRevised(request, 40L, 2);
        publisher.publishCanceled(canceledRequest, 40L);

        // then
        ArgumentCaptor<DomainEvent> captor = ArgumentCaptor.forClass(DomainEvent.class);
        verify(eventPublisher, org.mockito.Mockito.times(5)).publish(captor.capture());
        assertThat(captor.getAllValues())
                .satisfiesExactly(
                        event -> assertThat(event).isInstanceOfSatisfying(ReviewRequestCreatedEvent.class, created -> {
                            assertThat(created.reviewRequestId()).isEqualTo(20L);
                            assertThat(created.targetDraftId()).isEqualTo(40L);
                            assertThat(created.requesterId()).isEqualTo(1L);
                        }),
                        event -> assertThat(event).isInstanceOfSatisfying(ReviewSubmittedEvent.class, submitted -> {
                            assertThat(submitted.reviewId()).isEqualTo(30L);
                            assertThat(submitted.verdict()).isEqualTo(ReviewVerdict.CHANGES_REQUESTED);
                            assertThat(submitted.targetRound()).isZero();
                        }),
                        event -> assertThat(event)
                                .isInstanceOfSatisfying(ReviewRequestChangesRequestedEvent.class, changes -> {
                                    assertThat(changes.type()).isEqualTo(ReviewRequestType.DOCUMENT);
                                    assertThat(changes.targetDraftId()).isEqualTo(40L);
                                }),
                        event -> assertThat(event).isInstanceOfSatisfying(ReviewRequestRevisedEvent.class, revised -> {
                            assertThat(revised.type()).isEqualTo(ReviewRequestType.DOCUMENT);
                            assertThat(revised.resultVersionNo()).isEqualTo(2);
                        }),
                        event -> assertThat(event)
                                .isInstanceOfSatisfying(ReviewRequestCanceledEvent.class, canceled -> {
                                    assertThat(canceled.reviewRequestId()).isEqualTo(21L);
                                    assertThat(canceled.type()).isEqualTo(ReviewRequestType.DOCUMENT);
                                    assertThat(canceled.targetDraftId()).isEqualTo(40L);
                                }));
    }
}
