package com.ubidict.backend.notification.infra;

import com.ubidict.backend.notification.domain.Notification;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 소프트 삭제 조건을 메서드 이름에 박아 둔다. {@code @SQLRestriction}을 쓰지 않는 이유는 다른 도메인과 같다 — 조건이 보이지 않으면 잊는다.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByIdAndDeletedAtIsNull(Long id);

    Page<Notification> findAllByRecipientIdAndWorkspaceIdAndDeletedAtIsNull(
            Long recipientId, Long workspaceId, Pageable pageable);

    Page<Notification> findAllByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(
            Long recipientId, Long workspaceId, Pageable pageable);

    long countByRecipientIdAndWorkspaceIdAndReadFalseAndDeletedAtIsNull(Long recipientId, Long workspaceId);

    boolean existsByRecipientIdAndDedupeKey(Long recipientId, String dedupeKey);

    /**
     * 모두 읽음. 행을 하나씩 불러 {@code markRead()}를 부르면 안 읽은 알림 수만큼 UPDATE가 나가므로 한 문장으로 끝낸다.
     *
     * <p>벌크 UPDATE는 영속성 컨텍스트를 건너뛴다. 호출부는 이 메서드 뒤에 같은 엔티티를 다시 읽지 않는다.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Notification notification
            set notification.read = true,
                notification.readAt = :readAt,
                notification.updatedAt = :readAt
            where notification.recipientId = :recipientId
              and notification.workspaceId = :workspaceId
              and notification.read = false
              and notification.deletedAt is null
            """)
    int markAllRead(
            @Param("recipientId") Long recipientId,
            @Param("workspaceId") Long workspaceId,
            @Param("readAt") java.time.OffsetDateTime readAt);
}
