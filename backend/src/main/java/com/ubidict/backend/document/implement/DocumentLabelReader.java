package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentLabel;
import com.ubidict.backend.document.infra.DocumentLabelRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DocumentLabelReader {

    private final DocumentLabelRepository documentLabelRepository;

    public List<Long> readLabelIds(Document document) {
        return documentLabelRepository.findAllByDocumentId(document.getId()).stream()
                .map(DocumentLabel::getLabelId)
                .toList();
    }

    /**
     * 문서별 라벨 식별자를 한 번에 읽는다. 목록 조회가 문서 수만큼 질의하지 않게 한다.
     */
    public Map<Long, List<Long>> readLabelIds(List<Long> documentIds) {
        if (documentIds.isEmpty()) {
            return Map.of();
        }

        return documentLabelRepository.findAllByDocumentIdIn(documentIds).stream()
                .collect(Collectors.groupingBy(
                        DocumentLabel::getDocumentId,
                        Collectors.mapping(DocumentLabel::getLabelId, Collectors.toList())));
    }
}
