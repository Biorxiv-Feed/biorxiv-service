package com.biorxiv.repository;

import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Article;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;

import static com.biorxiv.TestArticles.article;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ArticleRepositoryTests {

    private static final String DOI = "10.1101/2026.01.01.000001";

    @Autowired
    private ArticleRepository articles;

    @Test
    void findsAndChecksArticleByDoiAndVersion() {
        Article v1 = articles.save(article(DOI, 1).build());

        assertThat(articles.findByDoiAndVersion(DOI, 1)).contains(v1);
        assertThat(articles.findByDoiAndVersion(DOI, 2)).isEmpty();
        assertThat(articles.existsByDoiAndVersion(DOI, 1)).isTrue();
        assertThat(articles.existsByDoiAndVersion(DOI, 2)).isFalse();
        assertThat(articles.existsByDoiAndVersion("10.1101/other", 1)).isFalse();
    }

    @Test
    void findMaxPublicationDate() {
        assertThat(articles.findMaxPublicationDate()).isEmpty();

        articles.save(article(DOI, 1).publicationDate(LocalDate.of(2026, 1, 5)).build());
        articles.save(article(DOI, 2).publicationDate(LocalDate.of(2026, 2, 1)).build());
        articles.save(article("10.1101/other", 1).publicationDate(LocalDate.of(2026, 1, 20)).build());

        assertThat(articles.findMaxPublicationDate()).contains(LocalDate.of(2026, 2, 1));
    }
}
