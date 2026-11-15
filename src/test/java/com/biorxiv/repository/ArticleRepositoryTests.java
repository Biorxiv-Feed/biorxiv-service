package com.biorxiv.repository;

import com.biorxiv.entity.Article;
import com.biorxiv.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(TestcontainersConfiguration.class)
class ArticleRepositoryTests {

    private static final String DOI = "10.1101/2026.01.01.000001";

    @Autowired
    private ArticleRepository articles;

    @Test
    void findsAndChecksArticleByDoiAndVersion() {
        Article v1 = articles.save(Article.builder().doi(DOI).version(1).title("Title").build());

        assertThat(articles.findByDoiAndVersion(DOI, 1)).contains(v1);
        assertThat(articles.findByDoiAndVersion(DOI, 2)).isEmpty();
        assertThat(articles.existsByDoiAndVersion(DOI, 1)).isTrue();
        assertThat(articles.existsByDoiAndVersion(DOI, 2)).isFalse();
        assertThat(articles.existsByDoiAndVersion("10.1101/other", 1)).isFalse();
    }
}
