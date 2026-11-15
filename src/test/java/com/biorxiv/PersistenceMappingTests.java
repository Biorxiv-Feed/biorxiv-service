package com.biorxiv;

import com.biorxiv.entity.Article;
import com.biorxiv.entity.Notification;
import com.biorxiv.entity.NotificationStatus;
import com.biorxiv.entity.Subscription;
import com.biorxiv.entity.User;
import jakarta.persistence.PersistenceException;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Verifies entity mappings against the Flyway schema (constraints, defaults, relationships).
 */
@DataJpaTest
@Import(TestcontainersConfiguration.class)
class PersistenceMappingTests {

    @Autowired
    private TestEntityManager em;

    @Test
    void databaseDefaultsMatchSpecification() {
        em.getEntityManager()
                .createNativeQuery("INSERT INTO users (telegram_id, created_at) VALUES (42, now())")
                .executeUpdate();

        Object[] row = (Object[]) em.getEntityManager()
                .createNativeQuery("SELECT daily_article_limit, active FROM users WHERE telegram_id = 42")
                .getSingleResult();

        assertThat(row[0]).isEqualTo(10);
        assertThat(row[1]).isEqualTo(true);
    }

    @Test
    void userGetsEntityDefaultsAndCreationTimestamp() {
        User user = em.persistFlushFind(User.builder().telegramId(1L).build());

        assertThat(user.getId()).isNotNull();
        assertThat(user.getDailyArticleLimit()).isEqualTo(User.DEFAULT_DAILY_ARTICLE_LIMIT);
        assertThat(user.getActive()).isTrue();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    @Test
    void telegramIdIsUnique() {
        em.persistAndFlush(User.builder().telegramId(1L).build());

        assertThatThrownBy(() -> em.persistAndFlush(User.builder().telegramId(1L).build()))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void articleDoiAndVersionAreUniqueTogether() {
        em.persistAndFlush(article("10.1101/2026.01.01.000001", 1));
        em.persistAndFlush(article("10.1101/2026.01.01.000001", 2));

        assertThatThrownBy(() -> em.persistAndFlush(article("10.1101/2026.01.01.000001", 1)))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void subscriptionUserAndCategoryAreUniqueTogether() {
        User user = em.persist(User.builder().telegramId(1L).build());
        em.persistAndFlush(Subscription.builder().user(user).category("genomics").build());
        em.persistAndFlush(Subscription.builder().user(user).category("neuroscience").build());

        assertThatThrownBy(() -> em.persistAndFlush(Subscription.builder().user(user).category("genomics").build()))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void notificationUserAndArticleAreUniqueTogether() {
        User user = em.persist(User.builder().telegramId(1L).build());
        Article article = em.persist(article("10.1101/x", 1));
        em.persistAndFlush(Notification.builder().user(user).article(article).build());

        assertThatThrownBy(() -> em.persistAndFlush(Notification.builder().user(user).article(article).build()))
                .isInstanceOf(PersistenceException.class);
    }

    @Test
    void notificationStatusIsStoredByNameAndRelationshipsAreLazy() {
        User user = em.persist(User.builder().telegramId(1L).build());
        Article article = em.persist(article("10.1101/x", 1));
        em.persist(Subscription.builder().user(user).category("genomics").build());
        Long notificationId = em.persistAndGetId(Notification.builder().user(user).article(article).build(), Long.class);
        em.flush();
        em.clear();

        Object storedStatus = em.getEntityManager()
                .createNativeQuery("SELECT status FROM notifications WHERE id = :id")
                .setParameter("id", notificationId)
                .getSingleResult();
        assertThat(storedStatus).isEqualTo("PENDING");

        Notification notification = em.find(Notification.class, notificationId);
        assertThat(notification.getStatus()).isEqualTo(NotificationStatus.PENDING);
        assertThat(notification.getAttempts()).isZero();
        assertThat(Hibernate.isInitialized(notification.getUser())).isFalse();
        assertThat(Hibernate.isInitialized(notification.getArticle())).isFalse();

        User reloaded = em.find(User.class, user.getId());
        assertThat(reloaded.getSubscriptions()).extracting(Subscription::getCategory).containsExactly("genomics");
        assertThat(reloaded.getNotifications()).extracting(Notification::getId).containsExactly(notificationId);
        assertThat(em.find(Article.class, article.getId()).getNotifications()).hasSize(1);
    }

    private static Article article(String doi, int version) {
        return Article.builder()
                .doi(doi)
                .version(version)
                .title("Title")
                .category("genomics")
                .publicationDate(LocalDate.of(2026, 1, 1))
                .build();
    }
}
