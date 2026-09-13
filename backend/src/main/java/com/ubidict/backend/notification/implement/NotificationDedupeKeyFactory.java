package com.ubidict.backend.notification.implement;

import java.time.OffsetDateTime;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * 같은 사건으로 알림이 두 번 생기지 않게 하는 키를 만든다(D-51).
 *
 * <p><b>이벤트 내용에서만 파생한다.</b> 난수나 현재 시각을 섞지 않는 것이 핵심이다 — 같은 이벤트가 재수신되면 반드시 같은 키가 나와야 두 번째 insert가
 * {@code uq_notification_recipient_dedupe}에서 튕긴다. 표준 큐는 at-least-once라 재수신이 정상 동작이다(D-53).
 *
 * <p>이벤트에 {@code eventId}를 더하지 않고 이 방식을 쓴 이유 — 6개 도메인이 이미 만들어 둔 이벤트 record를 건드리지 않아도 된다.
 *
 * <p><b>형식 문자열은 상수로 모아 둔다.</b> 키는 DB에 그대로 저장돼 과거 행과 비교되는 값이므로, 형식이 바뀌면 같은 사건이 다른 키를 얻어 중복 판정이
 * 깨진다. 한자리에 모아 두면 그 위험을 눈으로 확인할 수 있다 — <b>이미 저장된 알림이 있는 상태에서 이 상수를 고치면 안 된다.</b>
 *
 * <p>포맷에 {@link Locale#ROOT}를 못박는 이유 — {@code String.format}은 기본 로케일을 따르고, 숫자 표기가 ASCII가 아닌 로케일에서는
 * {@code %d}가 다른 글자로 렌더링된다. 사람이 읽을 문구라면 그것이 옳지만 <b>이 값은 DB 유니크 키</b>라, 서버 로케일이 달라졌다는 이유로 같은 사건이
 * 다른 키를 얻으면 중복 방지가 조용히 깨진다.
 */
@Component
public class NotificationDedupeKeyFactory {

    private static final String REVIEW_REQUEST_CREATED_KEY = "RR_CREATED:%d";
    private static final String REVIEW_SUBMITTED_KEY = "REVIEW_SUBMITTED:%d:%d:%d";
    private static final String CHANGES_REQUESTED_KEY = "RR_CHANGES:%d:%d";
    private static final String REVISED_KEY = "RR_REVISED:%d:%d";
    private static final String CANCELED_KEY = "RR_CANCELED:%d";

    public String reviewRequestCreated(Long reviewRequestId) {
        return key(REVIEW_REQUEST_CREATED_KEY, reviewRequestId);
    }

    /**
     * 리뷰어별·회차별로 키가 갈린다. 한 사람이 재교정 회차를 올려 다시 승인하면 다른 사건이다.
     *
     * <p>{@code occurredAt} 대신 {@code targetRound}를 쓴다 — 회차가 사건의 정체를 그대로 나타내고, 시각과 달리 재발행돼도 흔들리지 않는다.
     */
    public String reviewSubmitted(Long reviewRequestId, Long reviewerMemberId, int targetRound) {
        return key(REVIEW_SUBMITTED_KEY, reviewRequestId, reviewerMemberId, targetRound);
    }

    /**
     * 변경요청은 회차마다 반복된다. 이벤트에 회차가 없어 {@code occurredAt}으로 구분한다 — 재수신은 같은 시각을 들고 오므로 멱등이 유지된다.
     */
    public String changesRequested(Long reviewRequestId, OffsetDateTime occurredAt) {
        return key(CHANGES_REQUESTED_KEY, reviewRequestId, epochSecond(occurredAt));
    }

    public String revised(Long reviewRequestId, int resultVersionNo) {
        return key(REVISED_KEY, reviewRequestId, resultVersionNo);
    }

    public String canceled(Long reviewRequestId) {
        return key(CANCELED_KEY, reviewRequestId);
    }

    private static String key(String format, Object... values) {
        return String.format(Locale.ROOT, format, values);
    }

    private static long epochSecond(OffsetDateTime occurredAt) {
        return occurredAt == null ? 0L : occurredAt.toEpochSecond();
    }
}
