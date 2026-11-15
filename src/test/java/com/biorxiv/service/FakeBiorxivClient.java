package com.biorxiv.service;

import com.biorxiv.client.BiorxivClient;
import com.biorxiv.client.BiorxivPage;
import com.biorxiv.client.dto.BiorxivArticleDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory {@link BiorxivClient}: serves {@link #records} in pages of {@link #pageSize}, ignoring dates,
 * and records every request. Shared across tests in one context, so call {@link #reset()} before each test.
 */
public class FakeBiorxivClient implements BiorxivClient {

    public record Request(LocalDate from, LocalDate to, int cursor) {
    }

    private final List<BiorxivArticleDto> records = new ArrayList<>();
    private final List<Request> requests = new ArrayList<>();
    private int pageSize = 30;

    public void reset() {
        records.clear();
        requests.clear();
        pageSize = 30;
    }

    public void setRecords(List<BiorxivArticleDto> newRecords) {
        records.clear();
        records.addAll(newRecords);
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<Request> requests() {
        return List.copyOf(requests);
    }

    @Override
    public BiorxivPage fetchPage(LocalDate from, LocalDate to, int cursor) {
        requests.add(new Request(from, to, cursor));
        if (cursor >= records.size()) {
            return BiorxivPage.EMPTY;
        }
        List<BiorxivArticleDto> page = records.subList(cursor, Math.min(cursor + pageSize, records.size()));
        return new BiorxivPage(List.copyOf(page), records.size());
    }

    /** A record shaped like the real API's (all strings). */
    public static BiorxivArticleDto record(String doi, String version, String date, String category) {
        return new BiorxivArticleDto(doi, "Title of " + doi, "Doe, J.; Roe, R.", "Jane Doe", "Some University",
                date, version, "New Results", "cc_by", category, "https://www.biorxiv.org/content/early/x.source.xml",
                "Abstract", "NA", "bioRxiv");
    }
}
