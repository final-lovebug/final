package com.ubidict.backend.common.infra.ai;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 워커 경계 설정(D-71·D-77).
 *
 * @param mode 워커가 실제 모델을 호출할지 정해 요청 메시지에 실어 보내는 값. <b>백엔드는 이 값으로 분기하지 않는다</b>
 * @param timeout 콜백이 오지 않는 작업을 회수하는 기준
 */
@ConfigurationProperties(prefix = "app.ai")
public record LlmProperties(LlmMode mode, Timeout timeout) {

    /**
     * @param job 이 시간이 지나도록 끝나지 않은 작업을 실패로 회수한다. <b>SQS 가시성 타임아웃 × maxReceiveCount 보다 커야 한다</b>
     *     ({@code NFR-MSG-005}) — 재배달 중인 작업을 스위퍼가 먼저 죽이면 워커의 콜백이 무의미해진다
     * @param sweepInterval 스위퍼가 도는 주기
     */
    public record Timeout(boolean enabled, Duration job, Duration sweepInterval) {}
}
