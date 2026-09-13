package com.ubidict.backend.notification.implement;

import static org.assertj.core.api.Assertions.assertThat;

import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationMessageFactoryTest {

    private final NotificationMessageFactory factory = new NotificationMessageFactory();

    @DisplayName("반영완료 알림은 제목에 요청 제목을, 메타에 발행된 버전을 담는다.")
    @Test
    void revised() {
        // when
        NotificationContent content = factory.revised("결제 문서 개정 반영", ReviewRequestType.DOCUMENT, 6);

        // then
        assertThat(content.title()).contains("결제 문서 개정 반영").contains("반영");
        assertThat(content.message()).contains("문서").contains("r6");
    }

    @DisplayName("사전집 반영완료는 메타에 사전집이라고 적는다.")
    @Test
    void revised_dictionary() {
        // when
        NotificationContent content = factory.revised("용어 추가", ReviewRequestType.DICTIONARY, 7);

        // then
        assertThat(content.message()).contains("사전집").contains("r7");
    }

    @DisplayName("리뷰 요청 도착 알림은 요청 제목을 담는다.")
    @Test
    void reviewRequestReceived() {
        // when
        NotificationContent content = factory.reviewRequestReceived("결제 문서 개정 반영", ReviewRequestType.DOCUMENT);

        // then
        assertThat(content.title()).contains("결제 문서 개정 반영");
        assertThat(content.message()).contains("문서");
    }

    @DisplayName("승인·변경요청·취소 알림은 모두 제목과 메타가 채워진다.")
    @Test
    void otherTypes() {
        // when & then
        assertThat(factory.approved("A").title()).isNotBlank();
        assertThat(factory.approved("A").message()).isNotBlank();
        assertThat(factory.changesRequested("A").title()).isNotBlank();
        assertThat(factory.changesRequested("A").message()).isNotBlank();
        assertThat(factory.canceled("A").title()).isNotBlank();
        assertThat(factory.canceled("A").message()).isNotBlank();
    }
}
