package com.biorxiv.service;

import com.biorxiv.entity.Subscription;

import java.util.List;

public interface SubscriptionService {

    /**
     * Subscribes the user to the category (normalized first). Idempotent: an existing
     * subscription is returned unchanged.
     *
     * @throws com.biorxiv.exception.NotFoundException if there is no such user
     * @throws IllegalArgumentException if the category is blank
     */
    Subscription subscribe(Long userId, String category);

    /** @return {@code true} if a subscription was removed, {@code false} if there was none */
    boolean unsubscribe(Long userId, String category);

    /** The user's subscriptions sorted by category. */
    List<Subscription> getUserSubscriptions(Long userId);
}
