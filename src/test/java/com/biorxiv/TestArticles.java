package com.biorxiv;

import com.biorxiv.entity.Article;

import java.time.LocalDate;

/**
 * Builders for valid (savable) articles in tests: every NOT NULL column is filled.
 */
public final class TestArticles {

    public static final LocalDate PUBLICATION_DATE = LocalDate.of(2026, 1, 1);

    private TestArticles() {
    }

    /** Article in category "genomics"; override fields on the returned builder as needed. */
    public static Article.ArticleBuilder article(String doi, int version) {
        return Article.builder()
                .doi(doi)
                .version(version)
                .title("Title")
                .category("genomics")
                .publicationDate(PUBLICATION_DATE);
    }
}
