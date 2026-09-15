package com.ubidict.backend.dictionary.service;

import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.common.exception.CommonErrorCode;
import com.ubidict.backend.common.infra.event.EventPublisher;
import com.ubidict.backend.common.service.PageResult;
import com.ubidict.backend.dictionary.domain.Dictionary;
import com.ubidict.backend.dictionary.domain.NewTerm;
import com.ubidict.backend.dictionary.domain.event.DictionaryRevisedEvent;
import com.ubidict.backend.dictionary.exception.DictionaryErrorCode;
import com.ubidict.backend.dictionary.implement.DictionaryAppender;
import com.ubidict.backend.dictionary.implement.DictionaryReader;
import com.ubidict.backend.dictionary.implement.DictionaryUpdater;
import com.ubidict.backend.dictionary.implement.TermAppender;
import com.ubidict.backend.dictionary.implement.TermFormValidator;
import com.ubidict.backend.dictionary.implement.TermReader;
import com.ubidict.backend.dictionary.service.model.DictionaryResult;
import com.ubidict.backend.dictionary.service.model.DictionarySearchQuery;
import com.ubidict.backend.dictionary.service.model.DictionaryVersionResult;
import com.ubidict.backend.dictionary.service.model.TermResult;
import com.ubidict.backend.workspace.domain.Permission;
import com.ubidict.backend.workspace.implement.WorkspaceAccessValidator;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사전집 도메인의 공개 지점. 용어 추출·문서 대조는 readActive를, 리뷰 승인의 반영은 publish를 부른다.
 *
 * <p>워크스페이스 접근 검증은 workspace 도메인의 WorkspaceAccessValidator 하나만 참조한다. 리포지토리나 다른 implement를 직접 건드리지 않는다.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DictionaryService {

    private final DictionaryReader dictionaryReader;
    private final DictionaryAppender dictionaryAppender;
    private final DictionaryUpdater dictionaryUpdater;
    private final TermReader termReader;
    private final TermAppender termAppender;
    private final TermFormValidator termFormValidator;
    private final WorkspaceAccessValidator workspaceAccessValidator;
    private final EventPublisher eventPublisher;

    /**
     * 리뷰 승인이 확정한 전체 용어 목록을 현재 활성 버전 다음 버전으로 발행한다.
     *
     * <p><b>이 도메인에 버전을 만드는 다른 진입점은 없다</b>(DIC-7). ADMIN이 용어 목록을 직접 실어 보내던 임시 엔드포인트를 제거하고, 발행 위임 포트가 호출하는
     * 이 메서드 하나만 남겼다 — DOMAIN.md 「새 버전은 리뷰 승인(Revise)의 반영으로만 생긴다」와 NFR-UPD-001에 맞춘 최종 형태다.
     *
     * <p>기준 버전이 현재 활성 버전과 다르면 동시 변경 충돌로 거절한다(D-42).
     */
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
    public DictionaryResult readActive(Long workspaceId, Long memberId, DictionarySearchQuery query) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        Dictionary dictionary = dictionaryReader.readActive(workspaceId);

        return DictionaryResult.of(
                dictionary,
                termReader
                        .readPage(dictionary.getId(), query)
                        .map(item -> new TermResult(item.termId(), item.preferredForm(), item.englishName(), null)));
    }

    /**
     * 버전 이력을 최신순으로 읽는다. 사전집을 만든 적 없는 워크스페이스는 빈 목록이다 — 조회의 빈 결과는 실패가 아니다.
     */
    @Transactional(readOnly = true)
    public PageResult<DictionaryVersionResult> readVersions(Long workspaceId, Long memberId, int page, int size) {
        validatePagination(page, size);
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);

        PageResult<Dictionary> versions = dictionaryReader.readVersions(workspaceId, page, size);
        Map<Long, Long> termCounts = termReader.countByDictionaryIds(
                versions.content().stream().map(Dictionary::getId).toList());

        return versions.map(
                version -> DictionaryVersionResult.of(version, termCounts.getOrDefault(version.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public DictionaryResult readVersion(Long workspaceId, int versionNo, Long memberId, DictionarySearchQuery query) {
        workspaceAccessValidator.validateParticipant(workspaceId, memberId);
        Dictionary dictionary = dictionaryReader.readByVersionNo(workspaceId, versionNo);

        return DictionaryResult.of(
                dictionary,
                termReader
                        .readPage(dictionary.getId(), query)
                        .map(item -> new TermResult(item.termId(), item.preferredForm(), item.englishName(), null)));
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

        List<TermResult> terms =
                termReader.readAll(next.getId()).stream().map(TermResult::from).toList();
        eventPublisher.publish(
                new DictionaryRevisedEvent(next.getWorkspaceId(), next.getId(), next.versionNo(), next.publishedAt()));
        log.info(
                "[DictionaryService.revise] Dictionary version published. workspaceId={}, dictionaryId={}, versionNo={}",
                next.getWorkspaceId(),
                next.getId(),
                next.versionNo());
        return DictionaryResult.of(next, new PageResult<>(terms, 0, terms.size(), terms.size()));
    }

    private static void validatePagination(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BusinessException(CommonErrorCode.COMMON_INVALID_REQUEST);
        }
    }
}
