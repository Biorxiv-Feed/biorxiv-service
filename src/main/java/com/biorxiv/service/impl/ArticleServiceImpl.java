package com.biorxiv.service.impl;

import com.biorxiv.entity.Article;
import com.biorxiv.exception.NotFoundException;
import com.biorxiv.repository.ArticleRepository;
import com.biorxiv.service.ArticleService;
import com.biorxiv.util.CategoryUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ArticleServiceImpl implements ArticleService {

    private final ArticleRepository articleRepository;

    @Override
    public Optional<Article> saveIfNew(Article article) {
        if (articleRepository.existsByDoiAndVersion(article.getDoi(), article.getVersion())) {
            log.debug("Skipped duplicate article doi={} v{}", article.getDoi(), article.getVersion());
            return Optional.empty();
        }
        article.setCategory(CategoryUtils.normalize(article.getCategory()));
        Article saved = articleRepository.save(article);
        log.debug("Saved article id={} doi={} v{}", saved.getId(), saved.getDoi(), saved.getVersion());
        return Optional.of(saved);
    }

    @Override
    public List<Article> saveAllNew(Collection<Article> articles) {
        List<Article> saved = new ArrayList<>();
        for (Article article : articles) {
            saveIfNew(article).ifPresent(saved::add);
        }
        log.info("Saved {} new of {} articles ({} duplicates skipped)",
                saved.size(), articles.size(), articles.size() - saved.size());
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Article> findByDoiAndVersion(String doi, Integer version) {
        return articleRepository.findByDoiAndVersion(doi, version);
    }

    @Override
    @Transactional(readOnly = true)
    public Article getById(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new NotFoundException("Article", articleId));
    }
}
