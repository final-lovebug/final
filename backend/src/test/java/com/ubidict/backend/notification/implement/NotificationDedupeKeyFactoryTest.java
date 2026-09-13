package com.ubidict.backend.notification.implement;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class NotificationDedupeKeyFactoryTest {

    private static final OffsetDateTime OCCURRED_AT = OffsetDateTime.of(2026, 9, 13, 10, 0, 0, 0, ZoneOffset.UTC);

    private final NotificationDedupeKeyFactory factory = new NotificationDedupeKeyFactory();

    @DisplayName("키 형식은 고정이다 — 바뀌면 이미 저장된 알림과 중복 판정이 어긋난다.")
    @Test
    void keyFormatIsFrozen() {
        // when & then — 이 값들은 DB에 그대로 저장된다. 형식을 바꾸려면 기존 행을 어떻게 할지 함께 정해야 한다.
        assertThat(factory.reviewRequestCreated(100L)).isEqualTo("RR_CREATED:100");
        assertThat(factory.reviewSubmitted(100L, 7L, 2)).isEqualTo("REVIEW_SUBMITTED:100:7:2");
        assertThat(factory.changesRequested(100L, OCCURRED_AT))
                .isEqualTo("RR_CHANGES:100:" + OCCURRED_AT.toEpochSecond());
        assertThat(factory.revised(100L, 6)).isEqualTo("RR_REVISED:100:6");
        assertThat(factory.canceled(100L)).isEqualTo("RR_CANCELED:100");
    }

    @DisplayName("서버 기본 로케일이 바뀌어도 키는 같다.")
    @Test
    void keyIsLocaleIndependent() {
        // given — 숫자를 ASCII가 아닌 글자로 렌더링하는 로케일
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("hi-IN-u-nu-deva"));

        try {
            // when & then
            assertThat(factory.revised(100L, 6)).isEqualTo("RR_REVISED:100:6");
        } finally {
            Locale.setDefault(original);
        }
    }

    @DisplayName("같은 이벤트는 항상 같은 키를 만든다.")
    @Test
    void deterministic() {
        // when
        String first = factory.revised(100L, 6);
        String second = factory.revised(100L, 6);

        // then
        assertThat(first).isEqualTo(second);
    }

    @DisplayName("결과 버전이 다르면 다른 키가 된다.")
    @Test
    void differsByResultVersion() {
        // when & then
        assertThat(factory.revised(100L, 6)).isNotEqualTo(factory.revised(100L, 7));
    }

    @DisplayName("리뷰 요청이 다르면 다른 키가 된다.")
    @Test
    void differsByReviewRequest() {
        // when & then
        assertThat(factory.canceled(100L)).isNotEqualTo(factory.canceled(101L));
    }

    @DisplayName("리뷰어가 다르면 판정 키가 갈린다.")
    @Test
    void reviewSubmitted_differsByReviewer() {
        // when & then
        assertThat(factory.reviewSubmitted(100L, 1L, 0)).isNotEqualTo(factory.reviewSubmitted(100L, 2L, 0));
    }

    @DisplayName("같은 리뷰어가 재교정 회차를 올려 다시 판정하면 다른 키가 된다.")
    @Test
    void reviewSubmitted_differsByTargetRound() {
        // when & then
        assertThat(factory.reviewSubmitted(100L, 1L, 0)).isNotEqualTo(factory.reviewSubmitted(100L, 1L, 1));
    }

    @DisplayName("같은 회차의 같은 판정이 재수신되면 같은 키가 된다.")
    @Test
    void reviewSubmitted_isDeterministic() {
        // when & then
        assertThat(factory.reviewSubmitted(100L, 1L, 0)).isEqualTo(factory.reviewSubmitted(100L, 1L, 0));
    }

    @DisplayName("변경요청은 발생 시각으로 회차를 구분하고, 재수신은 같은 시각이라 같은 키가 된다.")
    @Test
    void changesRequested() {
        // when & then
        assertThat(factory.changesRequested(100L, OCCURRED_AT)).isEqualTo(factory.changesRequested(100L, OCCURRED_AT));
        assertThat(factory.changesRequested(100L, OCCURRED_AT))
                .isNotEqualTo(factory.changesRequested(100L, OCCURRED_AT.plusDays(1)));
    }

    @DisplayName("유형이 다르면 같은 리뷰 요청이라도 키가 겹치지 않는다.")
    @Test
    void differsByType() {
        // when & then
        assertThat(factory.reviewRequestCreated(100L)).isNotEqualTo(factory.canceled(100L));
    }

    @DisplayName("키는 컬럼 상한을 넘지 않는다.")
    @Test
    void withinColumnLength() {
        // when
        String key = factory.reviewSubmitted(Long.MAX_VALUE, Long.MAX_VALUE, Integer.MAX_VALUE);

        // then
        assertThat(key.length()).isLessThanOrEqualTo(200);
    }
}
