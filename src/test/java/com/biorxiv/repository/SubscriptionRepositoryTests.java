package com.biorxiv.repository;

import com.biorxiv.entity.Subscription;
import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class SubscriptionRepositoryTests {

    @Autowired
    private SubscriptionRepository subscriptions;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;

    @BeforeEach
    void setUp() {
        alice = em.persist(User.builder().telegramId(1L).build());
        bob = em.persist(User.builder().telegramId(2L).build());
        subscriptions.save(Subscription.builder().user(alice).category("neuroscience").build());
        subscriptions.save(Subscription.builder().user(alice).category("genomics").build());
        subscriptions.save(Subscription.builder().user(bob).category("genomics").build());
        em.flush();
    }

    @Test
    void findAllByUserIdReturnsOnlyThatUsersSubscriptionsSortedByCategory() {
        assertThat(subscriptions.findAllByUserIdOrderByCategoryAsc(alice.getId()))
                .extracting(Subscription::getCategory)
                .containsExactly("genomics", "neuroscience");
    }

    @Test
    void findsAndChecksSubscriptionByUserAndCategory() {
        assertThat(subscriptions.findByUserIdAndCategory(bob.getId(), "genomics")).isPresent();
        assertThat(subscriptions.findByUserIdAndCategory(bob.getId(), "neuroscience")).isEmpty();
        assertThat(subscriptions.existsByUserIdAndCategory(alice.getId(), "neuroscience")).isTrue();
        assertThat(subscriptions.existsByUserIdAndCategory(bob.getId(), "neuroscience")).isFalse();
    }

    @Test
    void deleteByUserIdAndCategoryRemovesOnlyThatSubscription() {
        assertThat(subscriptions.deleteByUserIdAndCategory(alice.getId(), "genomics")).isEqualTo(1);
        assertThat(subscriptions.deleteByUserIdAndCategory(alice.getId(), "genomics")).isZero();
        em.flush();

        assertThat(subscriptions.existsByUserIdAndCategory(alice.getId(), "genomics")).isFalse();
        assertThat(subscriptions.existsByUserIdAndCategory(alice.getId(), "neuroscience")).isTrue();
        assertThat(subscriptions.existsByUserIdAndCategory(bob.getId(), "genomics")).isTrue();
    }
}
