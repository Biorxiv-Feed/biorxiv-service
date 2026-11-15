package com.biorxiv.service;

import com.biorxiv.entity.Article;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ArticleService {

    /**
     * Saves the article unless one with the same DOI and version already exists.
     * The category is normalized before saving.
     *
     * @return the saved article, or empty if it was a duplicate
     */
    Optional<Article> saveIfNew(Article article);

    /**
     * Saves every article that is not stored yet (duplicates inside the batch included).
     *
     * @return only the newly saved articles, in input order
     */
    List<Article> saveAllNew(Collection<Article> articles);

    Optional<Article> findByDoiAndVersion(String doi, Integer version);

    /** @throws com.biorxiv.exception.NotFoundException if there is no such article */
    Article getById(Long articleId);
}
