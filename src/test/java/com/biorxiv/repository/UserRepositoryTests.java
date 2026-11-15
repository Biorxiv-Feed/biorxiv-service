package com.biorxiv.repository;

import com.biorxiv.entity.User;
import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Subscription;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class UserRepositoryTests {

    @Autowired
    private UserRepository users;

    @Autowired
    private TestEntityManager em;

    @Test
    void findsAndChecksUserByTelegramId() {
        User saved = users.save(User.builder().telegramId(100L).username("alice").build());

        assertThat(users.findByTelegramId(100L)).contains(saved);
        assertThat(users.findByTelegramId(999L)).isEmpty();
        assertThat(users.existsByTelegramId(100L)).isTrue();
        assertThat(users.existsByTelegramId(999L)).isFalse();
    }

    @Test
    void findActiveSubscribersOfCategoryReturnsOnlyActiveUsersOfThatCategory() {
        User active = em.persist(User.builder().telegramId(1L).build());
        User inactive = em.persist(User.builder().telegramId(2L).active(false).build());
        User otherCategory = em.persist(User.builder().telegramId(3L).build());
        User twoCategories = em.persist(User.builder().telegramId(4L).build());

        subscribe(active, "genomics");
        subscribe(inactive, "genomics");
        subscribe(otherCategory, "neuroscience");
        subscribe(twoCategories, "genomics");
        subscribe(twoCategories, "neuroscience");
        em.flush();
        em.clear();

        assertThat(users.findActiveSubscribersOfCategory("genomics"))
                .extracting(User::getTelegramId)
                .containsExactlyInAnyOrder(1L, 4L);
        assertThat(users.findActiveSubscribersOfCategory("zoology")).isEmpty();
    }

    private void subscribe(User user, String category) {
        em.persist(Subscription.builder().user(user).category(category).build());
    }
}
