package com.ubidict.backend.common.domain.event;

/**
 * 도메인에서 발행하는 이벤트임을 나타내는 마커 인터페이스.
 *
 * <p>구현체는 직렬화 가능한 불변 record로 정의한다. 로컬에서는 인메모리 어댑터가 객체 참조를 그대로 전달하므로 Entity나 지연 로딩 대상을 담아도 동작하지만,
 * 외부 메시지 큐 어댑터로 교체하는 순간 직렬화 실패나 LazyInitializationException으로 드러난다.
 *
 * <p>자세한 규약은 docs/ARCHITECTURE.md의 "이벤트 발행 규약"을 따른다.
 */
public interface DomainEvent {}
