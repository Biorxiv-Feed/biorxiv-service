package com.biorxiv.service;

import com.biorxiv.entity.Article;

import java.time.LocalDate;
import java.util.List;

/**
 * Outcome of one fetch run.
 *
 * @param from     first posting date requested (inclusive)
 * @param to       last posting date requested (inclusive)
 * @param pages    API pages read
 * @param received records returned by the API
 * @param skipped  records dropped because they could not be mapped or lacked required fields
 * @param saved    articles stored by this run (new (doi, version) pairs only), in API order
 */
public record FetchResult(LocalDate from, LocalDate to, int pages, int received, int skipped, List<Article> saved) {
}
