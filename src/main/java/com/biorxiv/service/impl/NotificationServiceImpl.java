package com.biorxiv.service.impl;

import com.biorxiv.entity.Article;
import com.biorxiv.entity.Notification;
import com.biorxiv.entity.NotificationStatus;
import com.biorxiv.entity.User;
import com.biorxiv.exception.NotFoundException;
import com.biorxiv.repository.NotificationRepository;
import com.biorxiv.service.NotificationService;
import com.biorxiv.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserService userService;
    private final Clock clock;

    @Override
    public int createForArticle(Article article) {
        List<User> subscribers = userService.findActiveSubscribers(article.getCategory());
        int created = 0;
        for (User user : subscribers) {
            if (notificationRepository.existsByUserIdAndArticleId(user.getId(), article.getId())) {
                continue;
            }
            notificationRepository.save(Notification.builder()
                    .user(user)
                    .article(article)
                    .build());
            created++;
        }
        if (created > 0) {
            log.info("Created {} notifications for article id={} category='{}' ({} subscribers)",
                    created, article.getId(), article.getCategory(), subscribers.size());
        } else {
            // Most articles have no subscribers; keep INFO for articles that actually reach someone.
            log.debug("No new notifications for article id={} category='{}' ({} subscribers)",
                    article.getId(), article.getCategory(), subscribers.size());
        }
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Notification> findPendingBatch(int batchSize) {
        if (batchSize <= 0) {
            throw new IllegalArgumentException("Batch size must be positive, got " + batchSize);
        }
        List<Notification> batch = notificationRepository.findByStatusOrderByCreatedAtAscIdAsc(
                NotificationStatus.PENDING, Limit.of(batchSize));
        log.debug("Loaded {} pending notifications (batch size {})", batch.size(), batchSize);
        return batch;
    }

    @Override
    public void markSent(Long notificationId) {
        Notification notification = getPending(notificationId);
        if (notification == null) {
            return;
        }
        notification.setStatus(NotificationStatus.SENT);
        notification.setAttempts(notification.getAttempts() + 1);
        notification.setSentAt(LocalDateTime.now(clock));
        notification.setLastError(null);
        log.info("Notification id={} sent to user id={} (article id={}, attempt {})", notificationId,
                notification.getUser().getId(), notification.getArticle().getId(), notification.getAttempts());
    }

    @Override
    public void markFailed(Long notificationId, String error) {
        Notification notification = getPending(notificationId);
        if (notification == null) {
            return;
        }
        int attempts = notification.getAttempts() + 1;
        notification.setAttempts(attempts);
        notification.setLastError(error);
        if (attempts >= MAX_ATTEMPTS) {
            notification.setStatus(NotificationStatus.FAILED);
            log.warn("Notification id={} failed permanently after {} attempts: {}", notificationId, attempts, error);
        } else {
            log.warn("Notification id={} failed (attempt {}/{}), will retry: {}",
                    notificationId, attempts, MAX_ATTEMPTS, error);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long countSentToday(Long userId) {
        LocalDate today = LocalDate.now(clock);
        return notificationRepository.countSentToUserBetween(
                userId, today.atStartOfDay(), today.plusDays(1).atStartOfDay());
    }

    @Override
    @Transactional(readOnly = true)
    public int remainingDailyQuota(Long userId) {
        User user = userService.getById(userId);
        long remaining = user.getDailyArticleLimit() - countSentToday(userId);
        if (remaining <= 0) {
            log.debug("User id={} reached the daily limit of {} articles", userId, user.getDailyArticleLimit());
        }
        return (int) Math.max(0, remaining);
    }

    /** Returns the notification if it is still PENDING; logs and returns null otherwise. */
    private Notification getPending(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new NotFoundException("Notification", notificationId));
        if (notification.getStatus() != NotificationStatus.PENDING) {
            log.warn("Ignored status update of notification id={}: already {}",
                    notificationId, notification.getStatus());
            return null;
        }
        return notification;
    }
}
