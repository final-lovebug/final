package com.ubidict.backend.reviewrequest.infra.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 발행 위임 스텁이 값을 지어내지 않고 예외를 던지는지 본다.
 *
 * <p>조회 스텁과 갈리는 지점이다. 버전 번호를 0으로 돌려주면 리뷰 요청이 존재하지 않는 버전을 결과로 기록하고 그대로 넘어간다.
 */
class PublishStubTest {

    @DisplayName("문서 버전 발행 스텁은 배선이 없음을 예외로 알린다.")
    @Test
    void documentVersionPublishStub_throws() {
        DocumentVersionPublishStub stub = new DocumentVersionPublishStub();

        assertThatThrownBy(() -> stub.publish(1L, 1, "본문", 1))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("DOC-6")
                .hasMessageContaining("app.crossdomain.document-publish.mode");
    }

    @DisplayName("사전집 버전 발행 스텁은 배선이 없음을 예외로 알린다.")
    @Test
    void dictionaryVersionPublishStub_throws() {
        DictionaryVersionPublishStub stub = new DictionaryVersionPublishStub();

        assertThatThrownBy(() -> stub.publish(1L, 1, List.of()))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessageContaining("DIC-3")
                .hasMessageContaining("app.crossdomain.dictionary-publish.mode");
    }
}
