package com.ubidict.backend.common.infra.schedule;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 주기 작업 활성화(D-73).
 *
 * <p>지금 이것을 쓰는 것은 AI 작업 타임아웃 스위퍼뿐이다. 스위퍼 자체는 {@code app.ai.timeout.enabled}로 끌 수 있지만, 스케줄러 인프라는
 * 프로퍼티와 무관하게 켜 둔다 — 끄고 켜는 단위를 작업 쪽에 두는 편이 빈 구성이 단순하다.
 */
@Configuration
@EnableScheduling
class SchedulingConfig {}
