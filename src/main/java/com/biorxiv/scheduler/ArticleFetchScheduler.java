package com.biorxiv.scheduler;

import com.biorxiv.config.BiorxivProperties;
import com.biorxiv.service.ArticleFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Runs the daily bioRxiv fetch, and optionally once at startup ({@code biorxiv.fetch.run-on-startup}).
 * Disable with {@code biorxiv.fetch.enabled=false}.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBooleanProperty(name = "biorxiv.fetch.enabled", matchIfMissing = true)
public class ArticleFetchScheduler {

    private final ArticleFetchService articleFetchService;
    private final BiorxivProperties properties;

    @EventListener(ApplicationReadyEvent.class)
    public void fetchOnStartup() {
        if (properties.fetch().runOnStartup()) {
            fetchDaily();
        }
    }

    @Scheduled(cron = "${biorxiv.fetch.cron}", zone = "${biorxiv.fetch.zone}")
    public void fetchDaily() {
        try {
            articleFetchService.fetchLatest();
        } catch (RuntimeException e) {
            // Pages saved before the failure stay; the next run's overlapping window picks up the rest.
            log.error("Daily bioRxiv fetch failed", e);
        }
    }
}
