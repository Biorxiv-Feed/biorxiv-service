package com.biorxiv.client;

import com.biorxiv.client.dto.BiorxivArticleDto;

import java.util.List;

/**
 * One page of the bioRxiv "details" endpoint.
 *
 * @param articles records on this page (at most 30)
 * @param total    total records in the requested window; 0 when nothing matches
 */
public record BiorxivPage(List<BiorxivArticleDto> articles, int total) {

    public static final BiorxivPage EMPTY = new BiorxivPage(List.of(), 0);
}
