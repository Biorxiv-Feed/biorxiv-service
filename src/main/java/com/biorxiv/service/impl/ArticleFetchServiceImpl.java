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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Not transactional on purpose: HTTP calls must not hold a DB transaction open.
 * Each page is saved through {@link ArticleService#saveAllNew}, which commits on its own.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleFetchServiceImpl implements ArticleFetchService {

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
                break;
            }
            pages++;
            received += page.articles().size();

            List<Article> articles = new ArrayList<>(page.articles().size());
            for (BiorxivArticleDto dto : page.articles()) {
                toArticle(dto).ifPresent(articles::add);
            }
            skipped += page.articles().size() - articles.size();
            saved.addAll(articleService.saveAllNew(articles));

            cursor += page.articles().size();
            if (cursor >= page.total()) {
                break;
            }
        }

        log.info("Fetched bioRxiv {}..{}: {} pages, {} received, {} new, {} skipped",
                from, to, pages, received, saved.size(), skipped);
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
            from = earliest;
        }
        return fetch(from, today);
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
