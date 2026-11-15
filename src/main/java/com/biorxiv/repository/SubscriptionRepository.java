package com.biorxiv.repository;

import com.biorxiv.entity.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByUserIdOrderByCategoryAsc(Long userId);

    Optional<Subscription> findByUserIdAndCategory(Long userId, String category);

    boolean existsByUserIdAndCategory(Long userId, String category);

    /** Unsubscribes a user from a category. Returns the number of deleted rows (0 or 1). */
    @Transactional
    long deleteByUserIdAndCategory(Long userId, String category);
}
