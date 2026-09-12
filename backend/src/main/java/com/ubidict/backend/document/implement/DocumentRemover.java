package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Document;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 소프트 삭제만 한다. 버전 행과 라벨 연결 행은 남긴다 — 모든 조회가 문서에서 먼저 막히기 때문이다.
 *
 * <p>초안·리뷰 같은 파생 데이터 정리는 이 도메인이 하지 않는다. document가 초안 도메인을 참조하면 의존 방향이 뒤집히므로,
 * 삭제 이벤트를 각 도메인이 구독해 스스로 정리한다. 이벤트 발행은 구독자가 생기는 draftdocument 도메인 작업에서 함께 넣는다.
 */
@Component
@RequiredArgsConstructor
public class DocumentRemover {

    public void remove(Document document) {
        document.delete();
    }
}
