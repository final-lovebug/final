package com.ubidict.backend.document.implement;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.domain.Document;
import com.ubidict.backend.document.domain.DocumentLabel;
import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import com.ubidict.backend.document.infra.DocumentLabelRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 문서의 라벨 집합을 통째로 교체한다.
 *
 * <p>부분 추가·삭제 대신 전체 교체를 쓰는 이유는 문서당 5개뿐이라 클라이언트가 중간 상태를 신경 쓸 필요가 없기 때문이다. 다만 그대로 유지되는 라벨의 연결 행은
 * 지웠다 다시 만들지 않는다 — 연결이 언제 생겼는지가 남는다.
 */
@Component
@RequiredArgsConstructor
public class DocumentLabelWriter {

    private final DocumentLabelRepository documentLabelRepository;

    public void replace(Document document, List<Label> labels, Long memberId) {
        validateCount(labels);

        Set<Long> target = labels.stream().map(Label::getId).collect(Collectors.toSet());
        List<DocumentLabel> current = documentLabelRepository.findAllByDocumentId(document.getId());

        List<DocumentLabel> removed = current.stream()
                .filter(link -> !target.contains(link.getLabelId()))
                .toList();
        documentLabelRepository.deleteAll(removed);

        Set<Long> kept = current.stream()
                .map(DocumentLabel::getLabelId)
                .filter(target::contains)
                .collect(Collectors.toSet());
        List<DocumentLabel> added = target.stream()
                .filter(labelId -> !kept.contains(labelId))
                .map(labelId -> DocumentLabel.of(document.getId(), labelId, memberId))
                .toList();
        documentLabelRepository.saveAll(added);
    }

    private static void validateCount(List<Label> labels) {
        if (labels.size() > DocumentLabel.MAX_LABELS_PER_DOCUMENT) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_LABEL_LIMIT_EXCEEDED);
        }
    }
}
