package com.biorxiv.client.impl;

import com.biorxiv.client.BiorxivClient;
import com.biorxiv.client.BiorxivPage;
import com.biorxiv.client.dto.BiorxivArticleDto;
import com.biorxiv.client.dto.BiorxivMessage;
import com.biorxiv.client.dto.BiorxivResponse;
import com.biorxiv.config.BiorxivProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
public class BiorxivClientImpl implements BiorxivClient {

    static final String DETAILS_PATH = "/details/{server}/{from}/{to}/{cursor}/json";

    private final RestClient restClient;
    private final String server;

    public BiorxivClientImpl(@Qualifier("biorxivRestClient") RestClient restClient, BiorxivProperties properties) {
        this.restClient = restClient;
        this.server = properties.api().server();
    }

    /**
     * Retried only for failures that can go away on their own: network errors and timeouts
     * ({@link ResourceAccessException}), 5xx responses and 429 Too Many Requests. Other 4xx responses and
     * unparsable bodies fail immediately. Settings: {@code biorxiv.api.retry.*}.
     */
    @Override
    @Retryable(
            includes = {ResourceAccessException.class, HttpServerErrorException.class,
                    HttpClientErrorException.TooManyRequests.class},
            maxRetriesString = "${biorxiv.api.retry.max-retries:3}",
            delayString = "${biorxiv.api.retry.delay-ms:2000}",
            multiplierString = "${biorxiv.api.retry.multiplier:2}")
    public BiorxivPage fetchPage(LocalDate from, LocalDate to, int cursor) {
        log.debug("Fetching bioRxiv {}..{} cursor={}", from, to, cursor);
        BiorxivResponse response = restClient.get()
                .uri(DETAILS_PATH, server, from, to, cursor)
                .retrieve()
                .body(BiorxivResponse.class);
        return toPage(response);
    }

    private static BiorxivPage toPage(BiorxivResponse response) {
        if (response == null || response.collection() == null || response.collection().isEmpty()) {
            return BiorxivPage.EMPTY;
        }
        BiorxivMessage message = response.messages() == null || response.messages().isEmpty()
                ? null : response.messages().getFirst();
        List<BiorxivArticleDto> articles = response.collection();
        int total = message != null && message.total() != null ? message.total() : articles.size();
        return new BiorxivPage(articles, total);
    }
}
