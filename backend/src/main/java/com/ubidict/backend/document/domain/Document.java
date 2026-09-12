package com.ubidict.backend.document.domain;

import com.ubidict.backend.common.domain.BaseEntity;
import com.ubidict.backend.common.exception.BusinessException;
import com.ubidict.backend.document.exception.DocumentErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 워크스페이스에 속한 작성·리뷰 대상 문서.
 *
 * <p>본문을 들고 있지 않다. 본문은 {@link DocumentVersion#getBody()}에만 있고, 현재 본문은 {@code currentVersionNo}가
 * 가리키는 버전이다. 두 곳에 저장하면 반영(Revise) 때 둘을 함께 갱신해야 하고 어긋난다.
 *
 * <p>본문은 버전 발행으로만 바뀐다. 업로드와 직접 편집은 이 엔티티가 다음 버전 번호를 발행한다.
 */
@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    public static final int TITLE_MAX_LENGTH = 200;

    private static final int FIRST_VERSION_NO = 1;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long workspaceId;

    @Column(nullable = false, length = TITLE_MAX_LENGTH)
    private String title;

    @Column(nullable = false)
    private int currentVersionNo;

    @Column(nullable = false)
    private Long updaterId;

    @Column(nullable = false, updatable = false)
    private Long createdBy;

    private Document(Long workspaceId, String title, Long createdBy) {
        this.workspaceId = workspaceId;
        this.title = title;
        this.currentVersionNo = FIRST_VERSION_NO;
        this.updaterId = createdBy;
        this.createdBy = createdBy;
    }

    /**
     * 업로드된 문서를 만든다. 본문은 받지 않는다 — 호출자가 같은 트랜잭션에서 v1 버전을 함께 발행한다.
     */
    public static Document create(Long workspaceId, String title, Long memberId) {
        return new Document(workspaceId, normalizeTitle(title), memberId);
    }

    public void rename(String title, Long memberId) {
        this.title = normalizeTitle(title);
        this.updaterId = memberId;
    }

    /**
     * 라벨만 바뀐 경우처럼 문서 자신의 필드는 그대로지만 최종수정자를 갱신해야 할 때 쓴다.
     */
    public void touch(Long memberId) {
        this.updaterId = memberId;
    }

    public int publishNext(Long memberId) {
        currentVersionNo++;
        updaterId = memberId;
        return currentVersionNo;
    }

    public boolean belongsTo(Long workspaceId) {
        return this.workspaceId.equals(workspaceId);
    }

    private static String normalizeTitle(String title) {
        if (title == null) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_INVALID_TITLE);
        }

        String normalized = title.strip();
        if (normalized.isEmpty() || normalized.length() > TITLE_MAX_LENGTH) {
            throw new BusinessException(DocumentErrorCode.DOCUMENT_INVALID_TITLE);
        }

        return normalized;
    }
}
