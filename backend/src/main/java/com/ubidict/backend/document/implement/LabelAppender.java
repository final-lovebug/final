package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.infra.LabelRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 이름으로 라벨을 찾아 없으면 만든다. 별도의 라벨 생성 절차를 두지 않고 문서에 붙일 때 생기게 한다.
 */
@Component
@RequiredArgsConstructor
public class LabelAppender {

    private final LabelRepository labelRepository;

    /**
     * 이름 목록에 해당하는 라벨을 모두 돌려준다. 없는 이름은 새로 만든다.
     *
     * <p>같은 이름을 동시에 만들면 {@code uk_label_workspace_name}이 한쪽을 막는다. 그 경우 상대가 만든 행을 다시 읽어 쓴다 —
     * 결과가 같으므로 사용자에게 실패로 보일 이유가 없다.
     */
    public List<Label> appendMissing(Long workspaceId, List<String> names, Long memberId) {
        Set<String> normalized = normalize(names);
        if (normalized.isEmpty()) {
            return List.of();
        }

        Map<String, Label> existing = findByNames(workspaceId, normalized);
        List<Label> created = normalized.stream()
                .filter(name -> !existing.containsKey(name))
                .map(name -> Label.create(workspaceId, name, memberId))
                .toList();

        if (created.isEmpty()) {
            return List.copyOf(existing.values());
        }

        try {
            labelRepository.saveAll(created);
        } catch (DataIntegrityViolationException concurrentlyCreated) {
            return List.copyOf(findByNames(workspaceId, normalized).values());
        }

        return List.copyOf(findByNames(workspaceId, normalized).values());
    }

    private Map<String, Label> findByNames(Long workspaceId, Set<String> names) {
        return labelRepository.findAllByWorkspaceIdAndNameIn(workspaceId, names).stream()
                .collect(Collectors.toMap(Label::getName, Function.identity()));
    }

    /**
     * 검증과 중복 제거를 한자리에서 한다. 입력 순서를 유지해 응답의 라벨 순서가 요청과 어긋나지 않게 한다.
     */
    private static Set<String> normalize(List<String> names) {
        if (names == null) {
            return Set.of();
        }

        return names.stream().map(Label::normalizeName).collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
