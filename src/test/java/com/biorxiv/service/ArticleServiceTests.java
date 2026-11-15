package com.biorxiv.service;

import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Article;
import com.biorxiv.exception.NotFoundException;
import com.biorxiv.repository.ArticleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ServiceTestConfiguration.class})
class ArticleServiceTests {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleRepository articleRepository;

    @Test
    void saveIfNewSavesWithNormalizedCategoryAndSkipsDuplicates() {
        assertThat(articleService.saveIfNew(article("10.1101/a", 1, " Cell  Biology")))
                .get()
                .satisfies(saved -> {
                    assertThat(saved.getId()).isNotNull();
                    assertThat(saved.getCategory()).isEqualTo("cell biology");
                });

        assertThat(articleService.saveIfNew(article("10.1101/a", 1, "cell biology"))).isEmpty();
        assertThat(articleService.saveIfNew(article("10.1101/a", 2, "cell biology"))).isPresent();
        assertThat(articleRepository.count()).isEqualTo(2);
    }

    @Test
    void saveAllNewReturnsOnlyNewArticlesInInputOrder() {
        articleService.saveIfNew(article("10.1101/existing", 1, "genomics"));

        List<Article> saved = articleService.saveAllNew(List.of(
                article("10.1101/b", 1, "genomics"),
                article("10.1101/existing", 1, "genomics"),
                article("10.1101/c", 1, "genomics"),
                article("10.1101/b", 1, "genomics")));   // duplicate inside the batch

        assertThat(saved).extracting(Article::getDoi).containsExactly("10.1101/b", "10.1101/c");
        assertThat(articleRepository.count()).isEqualTo(3);
    }

    @Test
    void findAndGet() {
        Article saved = articleService.saveIfNew(article("10.1101/a", 1, null)).orElseThrow();

        assertThat(articleService.findByDoiAndVersion("10.1101/a", 1)).contains(saved);
        assertThat(articleService.getById(saved.getId())).isEqualTo(saved);
        assertThatThrownBy(() -> articleService.getById(-1L)).isInstanceOf(NotFoundException.class);
    }

    private static Article article(String doi, int version, String category) {
        return Article.builder().doi(doi).version(version).title("Title").category(category).build();
    }
}
