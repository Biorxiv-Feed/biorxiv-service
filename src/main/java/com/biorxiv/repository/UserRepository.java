package com.biorxiv.repository;

import com.biorxiv.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByTelegramId(Long telegramId);

    boolean existsByTelegramId(Long telegramId);

    /**
     * Active users subscribed to the given category, i.e. the recipients for a new article in it.
     */
    @Query("""
            select u from User u
            where u.active = true
              and exists (select 1 from Subscription s where s.user = u and s.category = :category)
            """)
    List<User> findActiveSubscribersOfCategory(@Param("category") String category);
}
