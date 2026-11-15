package com.biorxiv.repository;

import com.biorxiv.entity.Article;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    Optional<Article> findByDoiAndVersion(String doi, Integer version);

    /** Duplicate check before saving an article fetched from bioRxiv. */
    boolean existsByDoiAndVersion(String doi, Integer version);

    /** Latest stored posting date; the daily fetch starts from here to catch up after downtime. */
    @Query("select max(a.publicationDate) from Article a")
    Optional<LocalDate> findMaxPublicationDate();
}
