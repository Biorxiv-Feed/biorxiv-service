package com.biorxiv.service;

import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Subscription;
import com.biorxiv.entity.User;
import com.biorxiv.exception.NotFoundException;
import com.biorxiv.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ServiceTestConfiguration.class})
class UserServiceTests {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TestEntityManager em;

    @Test
    void registerOrUpdateCreatesNewUserWithDefaults() {
        User user = userService.registerOrUpdate(100L, "alice");

        assertThat(user.getId()).isNotNull();
        assertThat(user.getTelegramId()).isEqualTo(100L);
        assertThat(user.getUsername()).isEqualTo("alice");
        assertThat(user.getDailyArticleLimit()).isEqualTo(User.DEFAULT_DAILY_ARTICLE_LIMIT);
        assertThat(user.getActive()).isTrue();
    }

    @Test
    void registerOrUpdateUpdatesUsernameAndReactivatesExistingUser() {
        User existing = userService.registerOrUpdate(100L, "alice");
        userService.deactivate(existing.getId());

        User updated = userService.registerOrUpdate(100L, "alice_new");

        assertThat(updated.getId()).isEqualTo(existing.getId());
        assertThat(updated.getUsername()).isEqualTo("alice_new");
        assertThat(updated.getActive()).isTrue();
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void getByIdThrowsForUnknownUser() {
        assertThatThrownBy(() -> userService.getById(-1L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateDailyArticleLimitAcceptsOnlyAllowedRange() {
        Long id = userService.registerOrUpdate(100L, null).getId();

        assertThat(userService.updateDailyArticleLimit(id, UserService.MAX_DAILY_ARTICLE_LIMIT).getDailyArticleLimit())
                .isEqualTo(UserService.MAX_DAILY_ARTICLE_LIMIT);
        assertThatThrownBy(() -> userService.updateDailyArticleLimit(id, UserService.MIN_DAILY_ARTICLE_LIMIT - 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> userService.updateDailyArticleLimit(id, UserService.MAX_DAILY_ARTICLE_LIMIT + 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(userService.getById(id).getDailyArticleLimit()).isEqualTo(UserService.MAX_DAILY_ARTICLE_LIMIT);
    }

    @Test
    void findActiveSubscribersNormalizesCategoryAndSkipsInactiveUsers() {
        User active = userService.registerOrUpdate(1L, null);
        User inactive = userService.registerOrUpdate(2L, null);
        em.persist(Subscription.builder().user(active).category("cell biology").build());
        em.persist(Subscription.builder().user(inactive).category("cell biology").build());
        userService.deactivate(inactive.getId());

        assertThat(userService.findActiveSubscribers("  Cell   Biology "))
                .extracting(User::getTelegramId)
                .containsExactly(1L);
        assertThat(userService.findActiveSubscribers("  ")).isEmpty();
        assertThat(userService.findActiveSubscribers(null)).isEmpty();
    }
}
