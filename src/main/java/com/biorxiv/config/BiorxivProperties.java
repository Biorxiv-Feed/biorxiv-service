package com.biorxiv.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * {@code biorxiv.*} settings: API access and the daily fetch window. Defaults live in {@code application.yaml}.
 */
@ConfigurationProperties("biorxiv")
public record BiorxivProperties(@DefaultValue Api api, @DefaultValue Fetch fetch) {

    /**
     * @param baseUrl        API root, e.g. {@code https://api.biorxiv.org}
     * @param server         {@code biorxiv} (the same API also serves {@code medrxiv})
     *                       <p>HTTP timeouts are Boot's {@code spring.http.clients.*} properties.
     */
    public record Api(
            @DefaultValue("https://api.biorxiv.org") String baseUrl,
            @DefaultValue("biorxiv") String server) {
    }

    /**
     * @param enabled         whether the scheduled daily fetch runs
     * @param runOnStartup    also run the fetch once when the application is ready (catch up after a deploy)
     * @param cron            schedule of the daily fetch
     * @param zone            zone of {@link #cron}
     * @param lookbackDays    each run re-fetches at least {@code today - lookbackDays .. today}
     * @param maxCatchUpDays  after downtime, the window starts no earlier than {@code today - maxCatchUpDays}
     * @param maxPages        safety stop for pagination
     */
    public record Fetch(
            @DefaultValue("true") boolean enabled,
            @DefaultValue("false") boolean runOnStartup,
            @DefaultValue("0 0 6 * * *") String cron,
            @DefaultValue("UTC") String zone,
            @DefaultValue("2") int lookbackDays,
            @DefaultValue("7") int maxCatchUpDays,
            @DefaultValue("200") int maxPages) {
    }
}
