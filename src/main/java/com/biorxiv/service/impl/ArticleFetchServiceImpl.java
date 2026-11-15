package com.biorxiv.service.impl;

import com.biorxiv.client.BiorxivClient;
import com.biorxiv.client.BiorxivPage;
import com.biorxiv.client.dto.BiorxivArticleDto;
import com.biorxiv.config.BiorxivProperties;
import com.biorxiv.entity.Article;
import com.biorxiv.mapper.BiorxivArticleMapper;
import com.biorxiv.repository.ArticleRepository;
import com.biorxiv.service.ArticleFetchService;
import com.biorxiv.service.ArticleService;
import com.biorxiv.service.FetchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Not transactional on purpose: HTTP calls must not hold a DB transaction open.
 * Each page is saved through {@link ArticleService#saveAllNew}, which commits on its own.
 * <p>
 * Logging: one line per received record. New article: INFO; already stored: DEBUG; skipped: WARN.
 * One INFO line at the start and end of each run, and DEBUG per page.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleFetchServiceImpl implements ArticleFetchService {

    /** Titles can be ~300 characters; keep log lines readable. */
    private static final int TITLE_LOG_LENGTH = 100;

    private final BiorxivClient biorxivClient;
    private final BiorxivArticleMapper articleMapper;
    private final ArticleService articleService;
    private final ArticleRepository articleRepository;
    private final BiorxivProperties properties;
    private final Clock clock;

    @Override
    public FetchResult fetch(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException("Invalid fetch window: " + from + ".." + to);
        }
        int maxPages = properties.fetch().maxPages();
        long started = System.nanoTime();
        log.info("Fetching bioRxiv articles posted {}..{}", from, to);

        List<Article> saved = new ArrayList<>();
        int pages = 0;
        int received = 0;
        int skipped = 0;
        int cursor = 0;
        while (true) {
            if (pages >= maxPages) {
                log.warn("Stopped bioRxiv fetch {}..{} after max-pages={} (cursor={})", from, to, maxPages, cursor);
                break;
            }
            BiorxivPage page = biorxivClient.fetchPage(from, to, cursor);
            if (page.articles().isEmpty()) {
                log.debug("No records at cursor={}, done", cursor);
                break;
            }
            pages++;
            received += page.articles().size();

            List<Article> articles = new ArrayList<>(page.articles().size());
            for (BiorxivArticleDto dto : page.articles()) {
                toArticle(dto).ifPresent(articles::add);
            }
            skipped += page.articles().size() - articles.size();
            List<Article> savedOnPage = articleService.saveAllNew(articles);
            saved.addAll(savedOnPage);
            logArticles(articles, savedOnPage);
            log.debug("Page {} (cursor={}): {} records, {} new, total in window {}",
                    pages, cursor, page.articles().size(), savedOnPage.size(), page.total());

            cursor += page.articles().size();
            if (cursor >= page.total()) {
                break;
            }
        }

        log.info("Fetched bioRxiv {}..{} in {} ms: {} pages, {} received, {} new, {} already stored, {} skipped",
                from, to, Duration.ofNanos(System.nanoTime() - started).toMillis(),
                pages, received, saved.size(), received - skipped - saved.size(), skipped);
        return new FetchResult(from, to, pages, received, skipped, saved);
    }

    @Override
    public FetchResult fetchLatest() {
        BiorxivProperties.Fetch config = properties.fetch();
        LocalDate today = LocalDate.now(clock);
        LocalDate from = today.minusDays(config.lookbackDays());
        Optional<LocalDate> latestStored = articleRepository.findMaxPublicationDate();
        if (latestStored.isPresent() && latestStored.get().minusDays(1).isBefore(from)) {
            from = latestStored.get().minusDays(1);
        }
        LocalDate earliest = today.minusDays(config.maxCatchUpDays());
        if (from.isBefore(earliest)) {
            // Downtime longer than the catch-up limit: papers posted before `earliest` are not fetched.
            log.warn("Catch-up limited to {} days: latest stored posting date is {}, fetching from {}",
                    config.maxCatchUpDays(), latestStored.orElse(null), earliest);
            from = earliest;
        }
        log.debug("Fetch window {}..{} (lookback {} days, latest stored posting date {})",
                from, today, config.lookbackDays(), latestStored.orElse(null));
        return fetch(from, today);
    }

    /** One line per mapped record: new articles at INFO, already stored ones at DEBUG. */
    private static void logArticles(List<Article> articles, List<Article> savedOnPage) {
        Set<Article> saved = Collections.newSetFromMap(new IdentityHashMap<>());
        saved.addAll(savedOnPage);
        for (Article article : articles) {
            if (saved.contains(article)) {
                log.info("New article id={} doi={} v{} posted={} category='{}' type='{}' title='{}'",
                        article.getId(), article.getDoi(), article.getVersion(), article.getPublicationDate(),
                        article.getCategory(), article.getType(), abbreviate(article.getTitle()));
            } else if (log.isDebugEnabled()) {
                log.debug("Already stored doi={} v{}", article.getDoi(), article.getVersion());
            }
        }
    }

    private static String abbreviate(String text) {
        return text.length() <= TITLE_LOG_LENGTH ? text : text.substring(0, TITLE_LOG_LENGTH - 1) + "…";
    }

    /** Maps one record, or returns empty (with a warning) if it is malformed or lacks a required field. */
    private Optional<Article> toArticle(BiorxivArticleDto dto) {
        Article article;
        try {
            article = articleMapper.toArticle(dto);
        } catch (NumberFormatException | DateTimeException e) {
            log.warn("Skipped bioRxiv record doi={} v{}: {}", dto.doi(), dto.version(), e.getMessage());
            return Optional.empty();
        }
        if (article.getDoi() == null || article.getVersion() == null || article.getTitle() == null
                || article.getCategory() == null || article.getPublicationDate() == null) {
            log.warn("Skipped bioRxiv record doi={} v{}: missing required field", dto.doi(), dto.version());
            return Optional.empty();
        }
        return Optional.of(article);
    }
}
