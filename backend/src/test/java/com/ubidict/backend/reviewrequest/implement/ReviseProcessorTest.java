package com.ubidict.backend.reviewrequest.implement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ubidict.backend.reviewrequest.domain.ReviewRequest;
import com.ubidict.backend.reviewrequest.domain.ReviewRequestType;
import com.ubidict.backend.reviewrequest.domain.RevisionDictionary;
import com.ubidict.backend.reviewrequest.domain.RevisionDocument;
import com.ubidict.backend.reviewrequest.infra.port.ActiveDictionaryVersionQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DictionaryVersionPublishPort;
import com.ubidict.backend.reviewrequest.infra.port.DocumentVersionPublishPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionaryQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDictionarySnapshot;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentQueryPort;
import com.ubidict.backend.reviewrequest.infra.port.DraftDocumentSnapshot;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviseProcessorTest {

    @Mock
    private DraftDocumentQueryPort draftDocumentQueryPort;

    @Mock
    private DraftDictionaryQueryPort draftDictionaryQueryPort;

    @Mock
    private ActiveDictionaryVersionQueryPort activeDictionaryVersionQueryPort;

    @Mock
    private DocumentVersionPublishPort documentVersionPublishPort;

    @Mock
    private DictionaryVersionPublishPort dictionaryVersionPublishPort;

    private ReviseProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new ReviseProcessor(
                draftDocumentQueryPort,
                draftDictionaryQueryPort,
                activeDictionaryVersionQueryPort,
                documentVersionPublishPort,
                dictionaryVersionPublishPort);
    }

    @DisplayName("문서 개정안을 초안이 대조에 쓴 사전집 버전으로 발행하고 결과 버전을 기록한다(D-93).")
    @Test
    void processDocument() {
        // given
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰", null, 1L, 1L);
        RevisionDocument revision = RevisionDocument.create(1L, 20L, 1, 30L, "개정 본문", 1L);
        given(draftDocumentQueryPort.read(30L))
                .willReturn(Optional.of(new DraftDocumentSnapshot(30L, 20L, 10L, 1, 3, "초안 본문")));
        given(documentVersionPublishPort.publish(20L, 1, "개정 본문", 3, 1L)).willReturn(2);

        // when
        int resultVersionNo = processor.processDocument(request, revision, 1L);

        // then
        assertThat(resultVersionNo).isEqualTo(2);
        assertThat(revision.getResultVersionNo()).isEqualTo(2);
        verifyNoInteractions(activeDictionaryVersionQueryPort);
    }

    @DisplayName("기준 버전이 없는 옛 초안은 종전대로 발행 시점의 활성 사전집 버전을 따른다(G-7).")
    @Test
    void processDocument_fallsBackToActiveVersion() {
        // given
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DOCUMENT, "리뷰", null, 1L, 1L);
        RevisionDocument revision = RevisionDocument.create(1L, 20L, 1, 30L, "개정 본문", 1L);
        given(draftDocumentQueryPort.read(30L))
                .willReturn(Optional.of(new DraftDocumentSnapshot(30L, 20L, 10L, 1, null, "초안 본문")));
        given(activeDictionaryVersionQueryPort.activeVersionNo(10L)).willReturn(4);
        given(documentVersionPublishPort.publish(20L, 1, "개정 본문", 4, 1L)).willReturn(2);

        // when
        processor.processDocument(request, revision, 1L);

        // then
        verify(documentVersionPublishPort).publish(20L, 1, "개정 본문", 4, 1L);
    }

    @DisplayName("사전 개정안의 최종 용어 목록을 새 버전으로 발행하고 결과 버전을 기록한다.")
    @Test
    void processDictionary() {
        // given
        ReviewRequest request = ReviewRequest.create(10L, ReviewRequestType.DICTIONARY, "리뷰", null, 1L, 1L);
        RevisionDictionary revision = RevisionDictionary.create(1L, 20L, 1, 30L, 1L);
        List<NewTermSnapshot> terms = List.of(new NewTermSnapshot("회원", "Member", "가입한 주체"));
        given(draftDictionaryQueryPort.read(30L)).willReturn(Optional.of(new DraftDictionarySnapshot(30L, 10L, 20L)));
        given(draftDictionaryQueryPort.readFinalTerms(30L)).willReturn(terms);
        given(dictionaryVersionPublishPort.publish(10L, 1, terms, 1L)).willReturn(2);

        // when
        int resultVersionNo = processor.processDictionary(request, revision, 1L);

        // then
        assertThat(resultVersionNo).isEqualTo(2);
        assertThat(revision.getResultVersionNo()).isEqualTo(2);
    }
}
