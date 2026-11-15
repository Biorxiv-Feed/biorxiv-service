package com.biorxiv.service.impl;

import com.biorxiv.entity.User;
import com.biorxiv.exception.NotFoundException;
import com.biorxiv.repository.UserRepository;
import com.biorxiv.service.UserService;
import com.biorxiv.util.CategoryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public User registerOrUpdate(Long telegramId, String username) {
        Objects.requireNonNull(telegramId, "telegramId");

        return userRepository.findByTelegramId(telegramId)
                .map(user -> {
                    if (!Objects.equals(user.getUsername(), username)) {
                        user.setUsername(username);
                        log.debug("Updated username of user id={}", user.getId());
                    }
                    if (!user.getActive()) {
                        user.setActive(true);
                        log.info("Reactivated user id={}", user.getId());
                    }
                    return user;
                })
                .orElseGet(() -> {
                    User user = userRepository.save(User.builder()
                            .telegramId(telegramId)
                            .username(username)
                            .build());
                    log.info("Registered user id={} telegramId={}", user.getId(), telegramId);
                    return user;
                });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByTelegramId(Long telegramId) {
        return userRepository.findByTelegramId(telegramId);
    }

    @Override
    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User", userId));
    }

    @Override
    public User updateDailyArticleLimit(Long userId, int dailyArticleLimit) {
        if (dailyArticleLimit < MIN_DAILY_ARTICLE_LIMIT || dailyArticleLimit > MAX_DAILY_ARTICLE_LIMIT) {
            throw new IllegalArgumentException("Daily article limit must be between %d and %d, got %d"
                    .formatted(MIN_DAILY_ARTICLE_LIMIT, MAX_DAILY_ARTICLE_LIMIT, dailyArticleLimit));
        }
        User user = getById(userId);
        user.setDailyArticleLimit(dailyArticleLimit);
        log.info("Set daily article limit of user id={} to {}", userId, dailyArticleLimit);
        return user;
    }

    @Override
    public void deactivate(Long userId) {
        User user = getById(userId);
        if (user.getActive()) {
            user.setActive(false);
            log.info("Deactivated user id={}", userId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findActiveSubscribers(String category) {
        String normalized = CategoryUtils.normalize(category);
        if (normalized == null) {
            return List.of();
        }
        return userRepository.findActiveSubscribersOfCategory(normalized);
    }
}
