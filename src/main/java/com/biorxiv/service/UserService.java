package com.biorxiv.service;

import com.biorxiv.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    int MIN_DAILY_ARTICLE_LIMIT = 1;
    int MAX_DAILY_ARTICLE_LIMIT = 50;

    /**
     * Creates a user for a new Telegram id, or updates the username of an existing one.
     * An inactive user is reactivated (e.g. they pressed /start again).
     */
    User registerOrUpdate(Long telegramId, String username);

    Optional<User> findByTelegramId(Long telegramId);

    /** @throws com.biorxiv.exception.NotFoundException if there is no such user */
    User getById(Long userId);

    /**
     * @throws IllegalArgumentException if the limit is outside
     *         [{@value #MIN_DAILY_ARTICLE_LIMIT}, {@value #MAX_DAILY_ARTICLE_LIMIT}]
     */
    User updateDailyArticleLimit(Long userId, int dailyArticleLimit);

    /** Stops all deliveries to the user (e.g. they blocked the bot). Subscriptions are kept. */
    void deactivate(Long userId);

    /** Active users subscribed to the category; the category is normalized first. */
    List<User> findActiveSubscribers(String category);
}
