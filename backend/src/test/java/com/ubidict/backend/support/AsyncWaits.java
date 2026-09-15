package com.ubidict.backend.support;

import java.time.Duration;
import org.awaitility.Awaitility;
import org.awaitility.core.ConditionFactory;

/**
 * 비동기 경로를 기다리는 공통 상한.
 *
 * <p>{@code docs/TEST.md}가 「대기 상한을 두고 넘으면 실패시킨다」를 요구하는데, 상한이 테스트마다 복제돼 있으면 한 번에 조정할 수 없다. 한 곳에 둔다.
 *
 * <p>AI 작업이 외부 워커로 나가면서 대기 구간이 「같은 JVM의 {@code @Async} 한 홉」에서 「커밋 → 큐 발행 → 롱폴 수신 → 워커 → 콜백 → 커밋」으로
 * 길어졌다. 손으로 쓴 폴링 루프 대신 Awaitility를 쓰는 이유는 타임아웃 시 <b>무엇을 기다리다 실패했는지</b>가 메시지에 남기 때문이다.
 */
public final class AsyncWaits {

    /** 인프로세스 대역이 곧바로 끝내는 경로. */
    public static final Duration IN_PROCESS = Duration.ofSeconds(10);

    /** 큐를 거쳐 워커가 콜백까지 돌려주는 경로. */
    public static final Duration LLM_ROUND_TRIP = Duration.ofSeconds(30);

    private AsyncWaits() {}

    public static ConditionFactory awaitInProcess() {
        return await(IN_PROCESS);
    }

    public static ConditionFactory awaitRoundTrip() {
        return await(LLM_ROUND_TRIP);
    }

    private static ConditionFactory await(Duration timeout) {
        return Awaitility.await().atMost(timeout).pollDelay(Duration.ZERO).pollInterval(Duration.ofMillis(200));
    }
}
