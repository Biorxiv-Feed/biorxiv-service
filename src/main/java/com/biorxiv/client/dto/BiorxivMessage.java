package com.biorxiv.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Paging info ({@code messages[0]}). The API sends {@code total} and {@code count_new_papers} as strings;
 * Jackson coerces them to numbers. When nothing matches, {@code status} is not "ok" and the counts are absent.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiorxivMessage(
        String status,
        String interval,
        Integer cursor,
        Integer count,
        @JsonProperty("count_new_papers") Integer countNewPapers,
        Integer total) {
}
