package com.biorxiv.repository;

import com.biorxiv.entity.Notification;
import com.biorxiv.entity.NotificationStatus;
import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Article;
import com.biorxiv.entity.User;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class NotificationRepositoryTests {

    private static final LocalDate DAY = LocalDate.of(2026, 3, 10);

    @Autowired
    private NotificationRepository notifications;

    @Autowired
    private TestEntityManager em;

    private User alice;
    private User bob;
    private int articleCounter;

    @BeforeEach
    void setUp() {
        alice = em.persist(User.builder().telegramId(1L).build());
        bob = em.persist(User.builder().telegramId(2L).build());
    }

    @Test
    void existsByUserIdAndArticleId() {
        Article article = newArticle();
        notifications.save(Notification.builder().user(alice).article(article).build());

        assertThat(notifications.existsByUserIdAndArticleId(alice.getId(), article.getId())).isTrue();
        assertThat(notifications.existsByUserIdAndArticleId(bob.getId(), article.getId())).isFalse();
    }

    @Test
    void findByStatusReturnsOldestFirstUpToLimitWithUserAndArticleLoaded() {
        Notification first = save(alice, NotificationStatus.PENDING, null);
        Notification second = save(bob, NotificationStatus.PENDING, null);
        save(alice, NotificationStatus.PENDING, null);
        save(alice, NotificationStatus.SENT, DAY.atTime(9, 0));
        save(bob, NotificationStatus.FAILED, null);
        em.flush();
        em.clear();

        List<Notification> pending =
                notifications.findByStatusOrderByCreatedAtAscIdAsc(NotificationStatus.PENDING, Limit.of(2));

        assertThat(pending).extracting(Notification::getId).containsExactly(first.getId(), second.getId());
        assertThat(pending).allSatisfy(n -> {
            assertThat(n.getStatus()).isEqualTo(NotificationStatus.PENDING);
            assertThat(Hibernate.isInitialized(n.getUser())).isTrue();
            assertThat(Hibernate.isInitialized(n.getArticle())).isTrue();
        });
    }

    @Test
    void countSentToUserBetweenCountsOnlySentNotificationsOfThatUserInRange() {
        LocalDateTime from = DAY.atStartOfDay();
        LocalDateTime to = DAY.plusDays(1).atStartOfDay();

        save(alice, NotificationStatus.SENT, from);                       // first instant of the day: counted
        save(alice, NotificationStatus.SENT, DAY.atTime(12, 0));          // counted
        save(alice, NotificationStatus.SENT, to.minusNanos(1000));        // last microsecond of the day: counted
        save(alice, NotificationStatus.SENT, to);                         // next day: excluded
        save(alice, NotificationStatus.SENT, from.minusSeconds(1));       // previous day: excluded
        save(alice, NotificationStatus.PENDING, null);                    // not sent: excluded
        save(alice, NotificationStatus.FAILED, DAY.atTime(13, 0));        // not sent: excluded
        save(bob, NotificationStatus.SENT, DAY.atTime(12, 0));            // other user: excluded
        em.flush();

        assertThat(notifications.countSentToUserBetween(alice.getId(), from, to)).isEqualTo(3);
        assertThat(notifications.countSentToUserBetween(bob.getId(), from, to)).isEqualTo(1);
    }

    private Notification save(User user, NotificationStatus status, LocalDateTime sentAt) {
        Notification notification = notifications.save(Notification.builder()
                .user(user)
                .article(newArticle())
                .status(status)
                .sentAt(sentAt)
                .build());
        em.flush();
        return notification;
    }

    private Article newArticle() {
        return em.persist(Article.builder()
                .doi("10.1101/test." + (++articleCounter))
                .version(1)
                .title("Title")
                .build());
    }
}
