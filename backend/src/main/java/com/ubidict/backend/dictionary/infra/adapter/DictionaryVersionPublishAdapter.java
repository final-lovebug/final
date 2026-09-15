package com.ubidict.backend.dictionary.infra.adapter;

import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.service.DictionaryService;
import com.ubidict.backend.reviewrequest.infra.port.DictionaryVersionPublishPort;
import com.ubidict.backend.reviewrequest.infra.port.NewTermSnapshot;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** 리뷰 승인 결과를 사전집 도메인의 발행 트랜잭션에 위임한다. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.crossdomain.dictionary-publish.mode", havingValue = "real")
public class DictionaryVersionPublishAdapter implements DictionaryVersionPublishPort {

    private final DictionaryService dictionaryService;

    @Override
    public int publish(Long workspaceId, int baseVersionNo, List<NewTermSnapshot> terms, Long publishedBy) {
        List<NewTerm> newTerms = terms.stream()
                .map(term -> new NewTerm(term.preferredForm(), term.englishName(), term.definition()))
                .toList();
        return dictionaryService.publish(workspaceId, baseVersionNo, newTerms, publishedBy);
    }
}
