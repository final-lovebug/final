package com.ubidict.backend.common.infra.ai;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * AI 워커로 나가는 작업 요청. <b>이 record의 JSON 모양이 곧 외부 계약</b>이다({@code docs/AI_CONTRACT.md}).
 *
 * <p><b>{@code EventEnvelope}를 쓰지 않는 이유</b>(D-63·R-28) — 봉투는 {@code eventType}에 Java 클래스의 단순 이름을 싣고 본문은
 * 해석하지 않은 채 넘기는 규약이라, 소비자가 우리 코드일 때만 성립한다. 여기 소비자는 FastAPI라 클래스 이름을 리팩터링하는 순간 외부가 깨진다. 그래서 타입 이름 대신
 * {@link #contractVersion}과 {@link #jobType}으로 계약을 명시한다.
 *
 * <p><b>본문을 싣지 않는 이유</b>(D-65) — 문서 본문 상한이 10,000자라 여러 건을 담으면 SQS 표준 메시지 한도 256KB를 넘는다. 워커는 식별자만 받고
 * 본문은 DB에서 직접 읽는다. 덧붙여, 발행 트랜잭션이 롤백돼 유령 메시지가 남아도 워커의 콜백이 404로 끝날 뿐 아무것도 오염시키지 않는다.
 *
 * <p>작업 종류에 따라 채워지는 필드가 다르다. 잘못 조합되지 않도록 <b>팩토리로만 만든다</b>.
 *
 * @param requestId 발행마다 새로 만드는 UUIDv4. <b>콜백 인증 토큰을 겸한다</b>(D-66) — 워커는 결과를 돌려줄 때 이 값을 그대로 실어야 하고,
 *     백엔드는 작업 행에 저장해 둔 값과 대조한다. 동시에 {@code docs/LOG.md}가 요구하는 correlation id이기도 하다
 * @param dictionaryId {@code TERM_EXTRACTION} 전용. 첫 회차 추출이면 {@code null}이다
 * @param sourceDocumentIds {@code TERM_EXTRACTION} 전용. 추출 대상으로 이미 걸러진 문서들이다(G-12)
 * @param documentId {@code DOCUMENT_CHECK} 전용
 * @param documentVersionNo {@code DOCUMENT_CHECK} 전용. 워커가 읽어야 할 문서 버전이며, 결과 앵커가 어느 본문 기준인지 대조하는 데 쓴다
 */
public record LlmJobRequest(
        int contractVersion,
        String requestId,
        LlmJobType jobType,
        Long jobId,
        Long workspaceId,
        Long dictionaryId,
        List<Long> sourceDocumentIds,
        Long documentId,
        Integer documentVersionNo,
        LlmMode mode,
        OffsetDateTime requestedAt) {

    public static final int CONTRACT_VERSION = 1;

    public static String newRequestId() {
        return UUID.randomUUID().toString();
    }

    public static LlmJobRequest termExtraction(
            String requestId,
            Long extractionJobId,
            Long workspaceId,
            Long dictionaryId,
            List<Long> sourceDocumentIds,
            LlmMode mode) {
        return new LlmJobRequest(
                CONTRACT_VERSION,
                requestId,
                LlmJobType.TERM_EXTRACTION,
                extractionJobId,
                workspaceId,
                dictionaryId,
                List.copyOf(sourceDocumentIds),
                null,
                null,
                mode,
                OffsetDateTime.now());
    }

    public static LlmJobRequest documentCheck(
            String requestId, Long checkJobId, Long workspaceId, Long documentId, int documentVersionNo, LlmMode mode) {
        return new LlmJobRequest(
                CONTRACT_VERSION,
                requestId,
                LlmJobType.DOCUMENT_CHECK,
                checkJobId,
                workspaceId,
                null,
                null,
                documentId,
                documentVersionNo,
                mode,
                OffsetDateTime.now());
    }
}
