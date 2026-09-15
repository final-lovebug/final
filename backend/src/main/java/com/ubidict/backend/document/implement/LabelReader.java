package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.infra.LabelRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LabelReader {

    private final LabelRepository labelRepository;

    public List<Label> readAll(Long workspaceId) {
        return labelRepository.findAllByWorkspaceIdOrderByNameAsc(workspaceId);
    }

    public Map<Long, String> readNames(Collection<Long> labelIds) {
        if (labelIds.isEmpty()) {
            return Map.of();
        }

        return labelRepository.findAllById(labelIds).stream()
                .collect(Collectors.toMap(Label::getId, Label::getName, (first, second) -> first));
    }
}
