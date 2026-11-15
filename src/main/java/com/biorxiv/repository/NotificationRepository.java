package com.biorxiv.repository;

import com.biorxiv.entity.Notification;
import com.biorxiv.entity.NotificationStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /** Duplicate check: a user must not be notified about the same article twice. */
    boolean existsByUserIdAndArticleId(Long userId, Long articleId);

    /**
     * Oldest notifications in the given status (ties broken by id), with user and article loaded
     * in the same query (a delivery job needs both, and loading them lazily would cost one query per row).
     */
    @EntityGraph(attributePaths = {"user", "article"})
    List<Notification> findByStatusOrderByCreatedAtAscIdAsc(NotificationStatus status, Limit limit);

    /**
     * Notifications sent to a user in {@code [from, to)}, e.g. one calendar day.
     * Used to enforce {@code User.dailyArticleLimit}.
     */
    @Query("""
            select count(n) from Notification n
            where n.user.id = :userId
              and n.status = com.biorxiv.entity.NotificationStatus.SENT
              and n.sentAt >= :from
              and n.sentAt < :to
            """)
    long countSentToUserBetween(@Param("userId") Long userId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);
}
