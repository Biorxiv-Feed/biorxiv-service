package com.biorxiv.service;

import com.biorxiv.TestcontainersConfiguration;
import com.biorxiv.entity.Article;
import com.biorxiv.repository.ArticleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDate;
import java.util.List;

import static com.biorxiv.TestArticles.article;
import static com.biorxiv.service.FakeBiorxivClient.record;
import static com.biorxiv.service.ServiceTestConfiguration.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@Import({TestcontainersConfiguration.class, ServiceTestConfiguration.class})
class ArticleFetchServiceTests {

    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final LocalDate FROM = LocalDate.of(2026, 3, 1);
    private static final LocalDate TO = LocalDate.of(2026, 3, 2);

    @Autowired
    private ArticleFetchService articleFetchService;

    @Autowired
    private FakeBiorxivClient client;

    @Autowired
    private ArticleRepository articleRepository;

    @BeforeEach
    void setUp() {
        client.reset();
    }

    @Test
    void fetchReadsAllPagesAndSavesEveryField() {
        client.setPageSize(2);
        client.setRecords(List.of(
                record("10.1101/a", "1", "2026-03-01", "Cell Biology "),
                record("10.1101/a", "2", "2026-03-01", "cell biology"),
                record("10.1101/b", "1", "2026-03-02", "genomics"),
                record("10.1101/c", "1", "2026-03-02", "genomics"),
                record("10.1101/d", "1", "2026-03-02", "zoology")));

        FetchResult result = articleFetchService.fetch(FROM, TO);

        assertThat(result.pages()).isEqualTo(3);
        assertThat(result.received()).isEqualTo(5);
        assertThat(result.skipped()).isZero();
        assertThat(result.saved()).extracting(Article::getDoi)
                .containsExactly("10.1101/a", "10.1101/a", "10.1101/b", "10.1101/c", "10.1101/d");
        assertThat(client.requests()).extracting(FakeBiorxivClient.Request::cursor).containsExactly(0, 2, 4);
        assertThat(client.requests()).allSatisfy(r -> {
            assertThat(r.from()).isEqualTo(FROM);
            assertThat(r.to()).isEqualTo(TO);
        });

        Article first = result.saved().getFirst();
        assertThat(first.getVersion()).isEqualTo(1);
        assertThat(first.getTitle()).isEqualTo("Title of 10.1101/a");
        assertThat(first.getAuthors()).isEqualTo("Doe, J.; Roe, R.");
        assertThat(first.getAbstractText()).isEqualTo("Abstract");
        assertThat(first.getCategory()).isEqualTo("cell biology");
        assertThat(first.getPublicationDate()).isEqualTo(FROM);
        assertThat(first.getType()).isEqualTo("new results");
        assertThat(first.getLicense()).isEqualTo("cc_by");
        assertThat(first.getPublishedDoi()).isNull();
        assertThat(first.getCorrespondingAuthor()).isEqualTo("Jane Doe");
        assertThat(first.getCorrespondingAuthorInstitution()).isEqualTo("Some University");
        assertThat(first.getServer()).isEqualTo("bioRxiv");
        assertThat(first.getJatsXml()).startsWith("https://");
    }

    @Test
    void secondRunOverSameWindowSavesNothing() {
        client.setRecords(List.of(record("10.1101/a", "1", "2026-03-01", "genomics")));
        articleFetchService.fetch(FROM, TO);

        FetchResult again = articleFetchService.fetch(FROM, TO);

        assertThat(again.received()).isEqualTo(1);
        assertThat(again.saved()).isEmpty();
        assertThat(articleRepository.count()).isEqualTo(1);
    }

    @Test
    void malformedOrIncompleteRecordsAreSkipped() {
        client.setRecords(List.of(
                record("10.1101/ok", "1", "2026-03-01", "genomics"),
                record("10.1101/bad-version", "v1", "2026-03-01", "genomics"),
                record("10.1101/bad-date", "1", "01.03.2026", "genomics"),
                record("10.1101/no-category", "1", "2026-03-01", "  "),
                record(" ", "1", "2026-03-01", "genomics")));

        FetchResult result = articleFetchService.fetch(FROM, TO);

        assertThat(result.received()).isEqualTo(5);
        assertThat(result.skipped()).isEqualTo(4);
        assertThat(result.saved()).extracting(Article::getDoi).containsExactly("10.1101/ok");
    }

    @Test
    void emptyWindowFetchesOnePageAndSavesNothing() {
        FetchResult result = articleFetchService.fetch(FROM, TO);

        assertThat(result.pages()).isZero();
        assertThat(result.saved()).isEmpty();
        assertThat(client.requests()).hasSize(1);
    }

    @Test
    void fetchRejectsInvalidWindow() {
        assertThatThrownBy(() -> articleFetchService.fetch(TO, FROM)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fetchLatestUsesLookbackWindowOnEmptyDatabase() {
        FetchResult result = articleFetchService.fetchLatest();

        assertThat(result.from()).isEqualTo(TODAY.minusDays(2));
        assertThat(result.to()).isEqualTo(TODAY);
    }

    @Test
    void fetchLatestCatchesUpFromLatestStoredDate() {
        articleRepository.save(article("10.1101/old", 1).publicationDate(TODAY.minusDays(5)).build());

        assertThat(articleFetchService.fetchLatest().from()).isEqualTo(TODAY.minusDays(6));
    }

    @Test
    void fetchLatestCatchUpIsCapped() {
        articleRepository.save(article("10.1101/old", 1).publicationDate(TODAY.minusDays(30)).build());

        assertThat(articleFetchService.fetchLatest().from()).isEqualTo(TODAY.minusDays(7));
    }

    @Test
    void fetchLatestKeepsLookbackWhenRecentArticlesExist() {
        articleRepository.save(article("10.1101/recent", 1).publicationDate(TODAY).build());

        assertThat(articleFetchService.fetchLatest().from()).isEqualTo(TODAY.minusDays(2));
    }
}
