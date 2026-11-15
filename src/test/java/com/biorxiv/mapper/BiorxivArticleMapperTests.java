package com.biorxiv.mapper;

import com.biorxiv.client.dto.BiorxivArticleDto;
import com.biorxiv.entity.Article;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BiorxivArticleMapperTests {

    private final BiorxivArticleMapper mapper = Mappers.getMapper(BiorxivArticleMapper.class);

    @Test
    void mapsEveryField() {
        Article article = mapper.toArticle(dto("2", "2026-11-12", "10.1038/s41586-026-00001-1"));

        assertThat(article.getId()).isNull();
        assertThat(article.getDoi()).isEqualTo("10.1101/2026.11.10.000001");
        assertThat(article.getVersion()).isEqualTo(2);
        assertThat(article.getTitle()).isEqualTo("Title");
        assertThat(article.getAuthors()).isEqualTo("Doe, J.");
        assertThat(article.getCorrespondingAuthor()).isEqualTo("Jane Doe");
        assertThat(article.getCorrespondingAuthorInstitution()).isEqualTo("Some University");
        assertThat(article.getPublicationDate()).isEqualTo(LocalDate.of(2026, 11, 12));
        assertThat(article.getType()).isEqualTo("New Results");          // normalized later by ArticleService
        assertThat(article.getLicense()).isEqualTo("cc_by");
        assertThat(article.getCategory()).isEqualTo("Cell Biology");     // trimmed; normalized by ArticleService
        assertThat(article.getJatsXml()).isEqualTo("https://example.org/a.source.xml");
        assertThat(article.getAbstractText()).isEqualTo("Abstract");
        assertThat(article.getPublishedDoi()).isEqualTo("10.1038/s41586-026-00001-1");
        assertThat(article.getServer()).isEqualTo("bioRxiv");
        assertThat(article.getNotifications()).isEmpty();
    }

    @Test
    void notPublishedAndBlankValuesBecomeNull() {
        assertThat(mapper.toArticle(dto("1", "2026-11-12", "NA")).getPublishedDoi()).isNull();
        assertThat(mapper.toArticle(dto("1", "2026-11-12", " ")).getPublishedDoi()).isNull();

        BiorxivArticleDto blanks = new BiorxivArticleDto("10.1101/x", "Title", "", " ", null,
                "2026-11-12", "1", "", "", "genomics", "", "", "NA", "");
        Article article = mapper.toArticle(blanks);
        assertThat(article.getAuthors()).isNull();
        assertThat(article.getCorrespondingAuthor()).isNull();
        assertThat(article.getType()).isNull();
        assertThat(article.getAbstractText()).isNull();
    }

    @Test
    void invalidVersionOrDateThrows() {
        assertThatThrownBy(() -> mapper.toArticle(dto("v1", "2026-11-12", "NA")))
                .isInstanceOf(NumberFormatException.class);
        assertThatThrownBy(() -> mapper.toArticle(dto("1", "12.11.2026", "NA")))
                .isInstanceOf(DateTimeParseException.class);
    }

    private static BiorxivArticleDto dto(String version, String date, String published) {
        return new BiorxivArticleDto("10.1101/2026.11.10.000001", " Title ", "Doe, J.", "Jane Doe", "Some University",
                date, version, "New Results", "cc_by", "Cell Biology ", "https://example.org/a.source.xml",
                "Abstract", published, "bioRxiv");
    }
}
