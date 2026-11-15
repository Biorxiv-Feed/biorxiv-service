package com.biorxiv.service;

import com.biorxiv.TestArticles;
import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Article;
import com.biorxiv.entity.Notification;
import com.biorxiv.entity.NotificationStatus;
import com.biorxiv.entity.User;
import com.biorxiv.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static com.biorxiv.service.ServiceTestConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ServiceTestConfiguration.class})
class NotificationServiceTests {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserService userService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private NotificationRepository notificationRepository;

    private User alice;
    private User bob;
    private int articleCounter;

    @BeforeEach
    void setUp() {
        alice = userService.registerOrUpdate(1L, "alice");
        bob = userService.registerOrUpdate(2L, "bob");
    }

    @Test
    void createForArticleNotifiesActiveSubscribersOnlyOnce() {
        User inactive = userService.registerOrUpdate(3L, null);
        subscriptionService.subscribe(alice.getId(), "genomics");
        subscriptionService.subscribe(bob.getId(), "neuroscience");
        subscriptionService.subscribe(inactive.getId(), "genomics");
        userService.deactivate(inactive.getId());
        Article article = newArticle("Genomics");

        assertThat(notificationService.createForArticle(article)).isEqualTo(1);
        assertThat(notificationService.createForArticle(article)).isZero();
        assertThat(notificationRepository.existsByUserIdAndArticleId(alice.getId(), article.getId())).isTrue();
        assertThat(notificationService.createForArticle(newArticle("zoology"))).isZero();   // no subscribers
    }

    @Test
    void findPendingBatchReturnsOnlyPendingUpToBatchSize() {
        Notification first = pending(alice);
        Notification second = pending(bob);
        pending(alice);
        notificationService.markSent(first.getId());

        List<Notification> batch = notificationService.findPendingBatch(1);

        assertThat(batch).extracting(Notification::getId).containsExactly(second.getId());
    }

    @Test
    void markSentRecordsSendTimeAndIgnoresRepeatedCalls() {
        Notification notification = pending(alice);

        notificationService.markSent(notification.getId());
        notificationService.markSent(notification.getId());   // ignored: already SENT

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(notification.getSentAt()).isEqualTo(NOW);
        assertThat(notification.getAttempts()).isEqualTo(1);
    }

    @Test
    void markFailedRetriesUntilMaxAttemptsThenFails() {
        Notification notification = pending(alice);

        for (int attempt = 1; attempt < NotificationService.MAX_ATTEMPTS; attempt++) {
            notificationService.markFailed(notification.getId(), "timeout " + attempt);
            assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        }
        notificationService.markFailed(notification.getId(), "blocked by user");

        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.FAILED);
        assertThat(notification.getAttempts()).isEqualTo(NotificationService.MAX_ATTEMPTS);
        assertThat(notification.getLastError()).isEqualTo("blocked by user");
        assertThat(notification.getSentAt()).isNull();
    }

    @Test
    void remainingDailyQuotaCountsOnlyTodaysSentNotifications() {
        userService.updateDailyArticleLimit(alice.getId(), 2);
        sentAt(alice, NOW.minusDays(1));                       // yesterday: not counted
        notificationService.markSent(pending(alice).getId());  // today
        sentAt(bob, NOW);                                      // other user

        assertThat(notificationService.countSentToday(alice.getId())).isEqualTo(1);
        assertThat(notificationService.remainingDailyQuota(alice.getId())).isEqualTo(1);

        notificationService.markSent(pending(alice).getId());
        notificationService.markSent(pending(alice).getId());  // over the limit

        assertThat(notificationService.remainingDailyQuota(alice.getId())).isZero();
    }

    private Notification pending(User user) {
        return notificationRepository.save(Notification.builder().user(user).article(newArticle("genomics")).build());
    }

    private void sentAt(User user, LocalDateTime sentAt) {
        notificationRepository.save(Notification.builder()
                .user(user)
                .article(newArticle("genomics"))
                .status(NotificationStatus.SENT)
                .sentAt(sentAt)
                .build());
    }

    private Article newArticle(String category) {
        return articleService.saveIfNew(TestArticles.article("10.1101/test." + (++articleCounter), 1)
                .category(category)
                .build()).orElseThrow();
    }
}
