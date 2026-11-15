package com.biorxiv.service.impl;

import com.biorxiv.entity.Subscription;
import com.biorxiv.entity.User;
import com.biorxiv.repository.SubscriptionRepository;
import com.biorxiv.service.SubscriptionService;
import com.biorxiv.service.UserService;
import com.biorxiv.util.CategoryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserService userService;

    @Override
    public Subscription subscribe(Long userId, String category) {
        String normalized = requireCategory(category);

        return subscriptionRepository.findByUserIdAndCategory(userId, normalized)
                .orElseGet(() -> {
                    User user = userService.getById(userId);
                    Subscription subscription = subscriptionRepository.save(Subscription.builder()
                            .user(user)
                            .category(normalized)
                            .build());
                    log.info("User id={} subscribed to '{}'", userId, normalized);
                    return subscription;
                });
    }

    @Override
    public boolean unsubscribe(Long userId, String category) {
        String normalized = CategoryUtils.normalize(category);
        if (normalized == null) {
            return false;
        }
        boolean removed = subscriptionRepository.deleteByUserIdAndCategory(userId, normalized) > 0;
        if (removed) {
            log.info("User id={} unsubscribed from '{}'", userId, normalized);
        }
        return removed;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscription> getUserSubscriptions(Long userId) {
        return subscriptionRepository.findAllByUserIdOrderByCategoryAsc(userId);
    }

    private static String requireCategory(String category) {
        String normalized = CategoryUtils.normalize(category);
        if (normalized == null) {
            throw new IllegalArgumentException("Category must not be blank");
        }
        return normalized;
    }
}
