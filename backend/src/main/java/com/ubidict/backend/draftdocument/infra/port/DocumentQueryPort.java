package com.ubidict.backend.draftdocument.infra.port;

import java.util.Optional;

/**
 * 초안이 근거로 삼는 문서 본문을 읽는다.
 *
 * <p>수동 생성 경로가 사라지면 초안 생성이 이 포트로 본문을 채운다(D-36).
 */
public interface DocumentQueryPort {
    Optional<DocumentSnapshot> read(Long documentId);
}
