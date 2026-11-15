package com.biorxiv.mapper;

import com.biorxiv.client.dto.BiorxivArticleDto;
import com.biorxiv.entity.Article;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

/**
 * Maps a bioRxiv API record to an unsaved {@link Article}.
 * <p>
 * Conversions: {@code version} String → Integer and {@code date} (ISO {@code yyyy-MM-dd}) → LocalDate are
 * built into MapStruct and throw {@code NumberFormatException} / {@code DateTimeParseException} on bad input.
 * Every String → String mapping goes through {@link #trimToNull}. Category and type are normalized later by
 * {@code ArticleService}.
 */
@Mapper(componentModel = "spring")
public interface BiorxivArticleMapper {

    String NOT_PUBLISHED = "NA";

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "notifications", ignore = true)
    @Mapping(target = "publicationDate", source = "date")
    @Mapping(target = "jatsXml", source = "jatsxml")
    @Mapping(target = "correspondingAuthor", source = "authorCorresponding")
    @Mapping(target = "correspondingAuthorInstitution", source = "authorCorrespondingInstitution")
    @Mapping(target = "publishedDoi", source = "published", qualifiedByName = "publishedDoi")
    Article toArticle(BiorxivArticleDto dto);

    /** Applied by MapStruct to every String → String mapping. */
    default String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /** The API sends "NA" while the preprint has no journal publication. */
    @Named("publishedDoi")
    default String publishedDoi(String published) {
        String value = trimToNull(published);
        return value == null || NOT_PUBLISHED.equalsIgnoreCase(value) ? null : value;
    }
}
