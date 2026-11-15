package com.biorxiv.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/** Body of {@code GET /details/{server}/{from}/{to}/{cursor}/json}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BiorxivResponse(List<BiorxivMessage> messages, List<BiorxivArticleDto> collection) {
}
