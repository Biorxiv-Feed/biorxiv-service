package com.biorxiv.service;

import java.time.LocalDate;

/**
 * Retrieves preprints from the bioRxiv API and stores the ones not seen before.
 */
public interface ArticleFetchService {

    /**
     * Fetches every page of preprints posted between {@code from} and {@code to} (inclusive) and saves new
     * (doi, version) pairs. Each page is committed separately, so an API failure keeps the pages already saved.
     */
    FetchResult fetch(LocalDate from, LocalDate to);

    /**
     * Daily run: fetches from {@code today - lookbackDays} (or earlier, back to the latest stored posting date,
     * to catch up after downtime, but never before {@code today - maxCatchUpDays}) up to today (UTC).
     */
    FetchResult fetchLatest();
}
