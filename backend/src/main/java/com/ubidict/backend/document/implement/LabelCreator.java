package com.ubidict.backend.document.implement;

import com.ubidict.backend.document.domain.Label;
import com.ubidict.backend.document.infra.LabelRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 라벨을 <b>독립 트랜잭션</b>에서 만들고 읽는다(D-94).
 *
 * <p>문서 생성과 트랜잭션을 나누는 이유는 {@code uk_label_workspace_name} 위반을 복구 가능하게 만들기 위해서다. 같은 트랜잭션에서
 * 위반이 나면 Hibernate가 세션을 rollback-only로 찍어 그 뒤로 아무것도 할 수 없고, 문서 생성까지 함께 죽어 사용자에게는 500이 된다.
 * 트랜잭션을 나누면 오염되는 것이 이 안쪽 세션뿐이라 호출자가 「상대가 먼저 만든 행」을 다시 읽어 쓸 수 있다.
 *
 * <p><b>예외를 여기서 잡지 않는다.</b> 안에서 잡으면 이 트랜잭션이 rollback-only가 되어 같은 문제를 한 겹 안으로 옮기는 것뿐이다.
 * 그래서 <b>라벨 하나에 트랜잭션 하나</b>이고, 판단은 경계 바깥의 {@link LabelAppender}가 한다.
 *
 * <p>{@code REQUIRES_NEW}는 프록시를 거쳐야 걸리므로 {@code LabelAppender}와 별도 빈이어야 한다.
 */
@Component
@RequiredArgsConstructor
public class LabelCreator {

    private final LabelRepository labelRepository;

    /**
     * 만들어진 라벨을 <b>돌려준다.</b> 호출자가 다시 읽게 두지 않는 이유는 MySQL 기본 격리 수준이 REPEATABLE READ이기 때문이다 —
     * 바깥 트랜잭션은 자기가 시작할 때의 스냅샷에 묶여 있어, 그보다 늦게 커밋된 이 행을 조회로는 볼 수 없다.
     *
     * <p>돌아가는 엔티티는 이 트랜잭션이 끝나면 준영속이다. 호출자가 쓰는 것은 식별자와 이름뿐이라 문제되지 않는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Label create(Long workspaceId, String name, Long memberId) {
        return labelRepository.saveAndFlush(Label.create(workspaceId, name, memberId));
    }

    /**
     * 경합에 져서 만들지 못했을 때 상대가 만든 행을 읽는다. 위와 같은 이유로 <b>이 조회도 새 트랜잭션이어야 한다.</b>
     *
     * <p>이름 비교는 DB가 대소문자를 무시하고 하므로({@code utf8mb4_0900_ai_ci}) 표기가 달라도 상대의 행을 찾는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    public Optional<Label> read(Long workspaceId, String name) {
        return labelRepository.findAllByWorkspaceIdAndNameIn(workspaceId, List.of(name)).stream()
                .findFirst();
    }
}
