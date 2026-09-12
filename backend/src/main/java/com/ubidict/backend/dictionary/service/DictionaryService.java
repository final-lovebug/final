package com.ubidict.backend.dictionary.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.implement.DictionaryAppender;
import com.ubidict.backend.dictionary.implement.DictionaryReader;
import com.ubidict.backend.dictionary.implement.DictionaryUpdater;
import com.ubidict.backend.dictionary.implement.TermAppender;
import com.ubidict.backend.dictionary.implement.TermFormValidator;
import com.ubidict.backend.dictionary.implement.TermReader;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import com.ubidict.backend.dictionary.service.model.ReviseDictionaryCommand;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사전집 도메인의 공개 지점. 용어 추출·문서 대조는 readActive를, 리뷰 승인은 revise를 부른다.
 *
 * <p>워크스페이스 접근 검증은 workspace 도메인의 WorkspaceAccessValidator 하나만 참조한다. 리포지토리나 다른 implement를 직접 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
public class DictionaryService {

    private final DictionaryReader dictionaryReader;
    private final DictionaryAppender dictionaryAppender;
    private final DictionaryUpdater dictionaryUpdater;
    private final TermReader termReader;
    private final TermAppender termAppender;
    private final TermFormValidator termFormValidator;
    private final WorkspaceAccessValidator workspaceAccessValidator;

    /**
     * 새 사전집 버전을 반영한다. 첫 버전과 다음 버전이 같은 경로다 — 둘 다 리뷰 승인의 결과이기 때문이다.
     *
     * <p>이전 버전을 먼저 내리고 새 버전을 올린다. 순서를 뒤집으면 활성 사전집이 순간 둘이 되어 유니크 제약에 걸린다.
     *
     * <p>호출자가 반영을 직렬로 보낸다는 전제 위에 있다. 「초안 사전」 정책이 사전집당 진행 중인 등재 흐름을 1개로 제한하므로, 같은 버전을 기준으로 편집하는 주체가 둘이 될 수
     * 없다.
     */
    @Transactional
    public DictionaryResult revise(ReviseDictionaryCommand command) {
        workspaceAccessValidator.validateAtLeast(command.workspaceId(), command.memberId(), Permission.ADMIN);

        return revise(command.workspaceId(), command.memberId(), command.toNewTerms());
    }

    /** 리뷰 승인이 확정한 전체 용어 목록을 현재 활성 버전 다음 버전으로 발행한다. */
    @Transactional
    public int publish(Long workspaceId, int baseVersionNo, List<NewTerm> terms, Long publishedBy) {
        workspaceAccessValidator.validateAtLeast(workspaceId, publishedBy, Permission.ADMIN);
        int activeVersionNo = dictionaryReader
                .readActiveOptional(workspaceId)
                .map(Dictionary::versionNo)
                .orElse(0);
        if (activeVersionNo != baseVersionNo) {
            throw new BusinessException(DictionaryErrorCode.DICTIONARY_VERSION_CONFLICT);
        }

        return revise(workspaceId, publishedBy, terms).versionNo();
    }

    /**
     * 현재 확정본을 읽는다. 문서 대조가 기준으로 삼는 사전집이다.
     */
    @Transactional(readOnly = true)
    public DictionaryResult readActive(Long workspaceId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        Dictionary dictionary = dictionaryReader.readActive(workspaceId);

        return DictionaryResult.of(dictionary, termReader.readAll(dictionary.getId()));
    }

    /**
     * 버전 이력을 최신순으로 읽는다. 사전집을 만든 적 없는 워크스페이스는 빈 목록이다 — 조회의 빈 결과는 실패가 아니다.
     */
    @Transactional(readOnly = true)
    public List<DictionaryVersionResult> readVersions(Long workspaceId, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        List<Dictionary> versions = dictionaryReader.readAllVersions(workspaceId);
        Map<Long, Long> termCounts = termReader.countByDictionaryIds(
                versions.stream().map(Dictionary::getId).toList());

        return versions.stream()
                .map(version -> DictionaryVersionResult.of(version, termCounts.getOrDefault(version.getId(), 0L)))
                .toList();
    }

    @Transactional(readOnly = true)
    public DictionaryResult readVersion(Long workspaceId, int versionNo, Long memberId) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        Dictionary dictionary = dictionaryReader.readByVersionNo(workspaceId, versionNo);

        return DictionaryResult.of(dictionary, termReader.readAll(dictionary.getId()));
    }

    private Dictionary appendNextVersion(Long workspaceId, Long memberId) {
        return dictionaryReader
                .readActiveOptional(workspaceId)
                .map(current -> {
                    dictionaryUpdater.archive(current);
                    return dictionaryAppender.appendNext(current, memberId);
                })
                .orElseGet(() -> dictionaryAppender.appendFirst(workspaceId, memberId));
    }

    private DictionaryResult revise(Long workspaceId, Long memberId, List<NewTerm> newTerms) {
        termFormValidator.validateUnique(newTerms);
        Dictionary next = appendNextVersion(workspaceId, memberId);
        termAppender.appendAll(next.getId(), newTerms, memberId);

        return DictionaryResult.of(next, termReader.readAll(next.getId()));
    }
}
