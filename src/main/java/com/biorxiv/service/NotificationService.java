package com.biorxiv.service;

import com.biorxiv.entity.Article;
import com.biorxiv.entity.Notification;

import java.util.List;

public interface NotificationService {

    /** A failed notification is retried until it has been attempted this many times. */
    int MAX_ATTEMPTS = 3;

    /**
     * Creates a {@code PENDING} notification for every active subscriber of the article's
     * category who has not been notified about this article yet.
     *
     * @return the number of notifications created
     */
    int createForArticle(Article article);

    /** Oldest {@code PENDING} notifications, with user and article loaded. */
    List<Notification> findPendingBatch(int batchSize);

    /** Marks a {@code PENDING} notification as {@code SENT} and records the send time. */
    void markSent(Long notificationId);

    /**
     * Records a failed delivery attempt. The notification stays {@code PENDING} for a retry
     * until {@link #MAX_ATTEMPTS} is reached, then becomes {@code FAILED}.
     */
    void markFailed(Long notificationId, String error);

    /** Notifications sent to the user since the start of today. */
    long countSentToday(Long userId);

    /** How many more articles the user may receive today ({@code dailyArticleLimit} minus sent today, never negative). */
    int remainingDailyQuota(Long userId);
}
