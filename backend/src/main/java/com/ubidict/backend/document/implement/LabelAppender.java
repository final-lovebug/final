package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.infra.LabelRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * 이름으로 라벨을 찾아 없으면 만든다. 별도의 라벨 생성 절차를 두지 않고 문서에 붙일 때 생기게 한다.
 *
 * <p><b>이름 비교는 대소문자를 구분하지 않는다</b>(D-94). {@code label.name}의 collation이 {@code utf8mb4_0900_ai_ci}이고
 * {@code uk_label_workspace_name}이 그 기준으로 막으므로, 자바 쪽 판정도 {@link Label#matchKey(String)}로 맞춘다.
 * 표시명은 최초 생성 시 입력한 표기를 유지한다 — {@code api}가 있으면 {@code API} 요청은 그 행을 재사용한다.
 */
@Component
@RequiredArgsConstructor
public class LabelAppender {

    private final LabelRepository labelRepository;
    private final LabelCreator labelCreator;

    /**
     * 이름 목록에 해당하는 라벨을 모두 돌려준다. 없는 이름은 새로 만든다.
     *
     * <p>생성은 {@link LabelCreator}가 독립 트랜잭션에서 하므로 여기서 제약 위반을 받아도 바깥 트랜잭션은 멀쩡하다. 같은 이름을
     * 동시에 만들면 {@code uk_label_workspace_name}이 한쪽을 막는데, 그 경우 상대가 만든 행을 읽어 쓴다 — 결과가 같으므로
     * 사용자에게 실패로 보일 이유가 없다.
     *
     * <p><b>만들어진 라벨은 다시 조회하지 않고 {@code LabelCreator}가 돌려준 것을 그대로 쓴다.</b> 바깥 트랜잭션이
     * REPEATABLE READ 스냅샷에 묶여 있어 자기보다 늦게 커밋된 행을 조회로는 보지 못하기 때문이다.
     */
    public List<Label> appendMissing(Long workspaceId, List<String> names, Long memberId) {
        Map<String, String> requested = requested(names);
        if (requested.isEmpty()) {
            return List.of();
        }

        Map<String, Label> resolved = findByMatchKey(workspaceId, requested.values());
        for (Map.Entry<String, String> entry : requested.entrySet()) {
            if (resolved.containsKey(entry.getKey())) {
                continue;
            }
            append(workspaceId, entry.getValue(), memberId)
                    .ifPresent(label -> resolved.put(Label.matchKey(label.getName()), label));
        }

        return requested.keySet().stream()
                .map(resolved::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /**
     * 만들거나, 경합에 졌으면 상대가 만든 것을 읽는다. 둘 다 실패하는 경우(만드는 사이 지워진 경우 등)는 라벨만 조용히 빠진다 —
     * 라벨은 문서의 부속이라 문서 생성을 막을 이유가 없다.
     */
    private Optional<Label> append(Long workspaceId, String name, Long memberId) {
        try {
            return Optional.of(labelCreator.create(workspaceId, name, memberId));
        } catch (DataIntegrityViolationException concurrentlyCreated) {
            return labelCreator.read(workspaceId, name);
        }
    }

    /**
     * 요청 이름을 비교 키로 접는다. 한 요청에 {@code ["API", "api"]}가 와도 라벨은 하나이며, 표시명은 <b>먼저 온 표기</b>가 이긴다.
     *
     * <p>{@link LinkedHashMap}을 쓰는 이유는 응답의 라벨 순서가 요청과 어긋나지 않게 하기 위해서다.
     */
    private static Map<String, String> requested(List<String> names) {
        if (names == null) {
            return Map.of();
        }

        Map<String, String> requested = new LinkedHashMap<>();
        for (String name : names) {
            requested.putIfAbsent(Label.matchKey(name), Label.normalizeName(name));
        }

        return requested;
    }

    /**
     * 조회 자체는 DB가 대소문자를 무시하고 해 주므로 이름을 그대로 넘기고, <b>결과만 비교 키로 다시 묶는다.</b> 자바 {@code Map}의 키 비교는
     * 대소문자를 구분하기 때문에 여기서 접지 않으면 DB가 찾아 준 행을 「없는 이름」으로 오인한다 — 그것이 Y-36이었다.
     */
    private Map<String, Label> findByMatchKey(Long workspaceId, Collection<String> names) {
        Map<String, Label> byMatchKey = new LinkedHashMap<>();
        for (Label label : labelRepository.findAllByWorkspaceIdAndNameIn(workspaceId, names)) {
            byMatchKey.putIfAbsent(Label.matchKey(label.getName()), label);
        }

        return byMatchKey;
    }
}
