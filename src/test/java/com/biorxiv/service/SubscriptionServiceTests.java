package com.biorxiv.service;

import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Subscription;
import com.biorxiv.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ServiceTestConfiguration.class})
class SubscriptionServiceTests {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserService userService;

    private Long userId;

    @BeforeEach
    void setUp() {
        userId = userService.registerOrUpdate(1L, null).getId();
    }

    @Test
    void subscribeNormalizesCategoryAndIsIdempotent() {
        Subscription first = subscriptionService.subscribe(userId, " Genomics ");
        Subscription second = subscriptionService.subscribe(userId, "genomics");

        assertThat(first.getCategory()).isEqualTo("genomics");
        assertThat(second.getId()).isEqualTo(first.getId());
        assertThat(subscriptionService.getUserSubscriptions(userId)).hasSize(1);
    }

    @Test
    void subscribeRejectsUnknownUserAndBlankCategory() {
        assertThatThrownBy(() -> subscriptionService.subscribe(-1L, "genomics"))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> subscriptionService.subscribe(userId, "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsubscribeReportsWhetherSomethingWasRemoved() {
        subscriptionService.subscribe(userId, "genomics");

        assertThat(subscriptionService.unsubscribe(userId, "GENOMICS")).isTrue();
        assertThat(subscriptionService.unsubscribe(userId, "genomics")).isFalse();
        assertThat(subscriptionService.unsubscribe(userId, " ")).isFalse();
        assertThat(subscriptionService.getUserSubscriptions(userId)).isEmpty();
    }

    @Test
    void getUserSubscriptionsIsSortedByCategory() {
        subscriptionService.subscribe(userId, "neuroscience");
        subscriptionService.subscribe(userId, "bioinformatics");
        subscriptionService.subscribe(userId, "genomics");

        assertThat(subscriptionService.getUserSubscriptions(userId))
                .extracting(Subscription::getCategory)
                .containsExactly("bioinformatics", "genomics", "neuroscience");
    }
}
