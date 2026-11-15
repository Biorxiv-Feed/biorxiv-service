package com.biorxiv.service;

import com.biorxiv.service.impl.ArticleServiceImpl;
import com.biorxiv.service.impl.NotificationServiceImpl;
import com.biorxiv.service.impl.SubscriptionServiceImpl;
import com.biorxiv.service.impl.UserServiceImpl;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Adds the service layer to a {@code @DataJpaTest} slice, with a clock fixed at {@link #NOW}.
 */
@TestConfiguration(proxyBeanMethods = false)
@Import({UserServiceImpl.class, ArticleServiceImpl.class, SubscriptionServiceImpl.class, NotificationServiceImpl.class})
public class ServiceTestConfiguration {

    public static final LocalDateTime NOW = LocalDateTime.of(2026, 3, 10, 12, 0);

    @Bean
    Clock clock() {
        ZoneId zone = ZoneId.systemDefault();
        return Clock.fixed(NOW.atZone(zone).toInstant(), zone);
    }
}
