package com.biorxiv.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One record of the {@code collection} array, exactly as the API returns it (every value is a string).
 *
 * @param date      posting date of this version, {@code yyyy-MM-dd}
 * @param version   "1", "2", ...
 * @param jatsxml   URL of the full-text JATS XML
 * @param published journal DOI, or "NA" while unpublished
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiorxivArticleDto(
        String doi,
        String title,
        String authors,
        @JsonProperty("author_corresponding") String authorCorresponding,
        @JsonProperty("author_corresponding_institution") String authorCorrespondingInstitution,
        String date,
        String version,
        String type,
        String license,
        String category,
        String jatsxml,
        @JsonProperty("abstract") String abstractText,
        String published,
        String server) {
}
