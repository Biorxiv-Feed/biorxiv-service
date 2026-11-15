package com.biorxiv.client;

import java.time.LocalDate;

/**
 * Read access to the bioRxiv API. An interface so tests can replace the HTTP client with a fake.
 */
public interface BiorxivClient {

    /**
     * Returns the page of preprints posted between {@code from} and {@code to} (inclusive)
     * that starts at record {@code cursor}.
     */
    BiorxivPage fetchPage(LocalDate from, LocalDate to, int cursor);
}
