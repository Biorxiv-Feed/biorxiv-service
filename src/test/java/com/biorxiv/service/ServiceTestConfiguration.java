package com.biorxiv.service;

import com.biorxiv.config.BiorxivProperties;
import com.biorxiv.mapper.BiorxivArticleMapperImpl;
import com.biorxiv.service.impl.ArticleFetchServiceImpl;
import com.biorxiv.service.impl.ArticleServiceImpl;
import com.biorxiv.service.impl.NotificationServiceImpl;
import com.biorxiv.service.impl.SubscriptionServiceImpl;
import com.biorxiv.service.impl.UserServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

/**
 * Adds the service layer to a {@code @DataJpaTest} slice, with a UTC clock fixed at {@link #NOW}
 * and a {@link FakeBiorxivClient} instead of HTTP calls.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({UserServiceImpl.class, ArticleServiceImpl.class, SubscriptionServiceImpl.class, NotificationServiceImpl.class,
        ArticleFetchServiceImpl.class, BiorxivArticleMapperImpl.class})
public class ServiceTestConfiguration {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);

    /** Same values as the defaults in application.yaml. */
    public static final BiorxivProperties PROPERTIES = new BiorxivProperties(
            new BiorxivProperties.Api("https://api.biorxiv.org", "biorxiv"),
            new BiorxivProperties.Fetch(true, false, "0 0 6 * * *", "UTC", 2, 7, 200));

    @Bean
    Clock clock() {
        return Clock.fixed(NOW.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    }

    @Bean
    BiorxivProperties biorxivProperties() {
        return PROPERTIES;
    }

    @Bean
    FakeBiorxivClient fakeBiorxivClient() {
        return new FakeBiorxivClient();
    }
}
