# BioRxiv Service

A Java service that automatically retrieves newly published scientific articles from [bioRxiv](https://www.biorxiv.org/), stores them in PostgreSQL, and delivers personalized notifications to subscribers through a Telegram bot.

## Features

1. Retrieve newly published articles from the bioRxiv API on a daily schedule.
2. Handle API pagination so all available articles are retrieved.
3. Parse and persist article metadata in PostgreSQL.
4. Detect newly discovered articles and skip duplicates.
5. Find subscribers interested in specific article categories.
6. Deliver personalized article notifications through Telegram.
7. Let users manage their subscriptions and notification preferences.
8. Enforce a configurable daily article delivery limit per subscriber.

> Status: persistence, the service layer and daily retrieval from bioRxiv are in place; the Telegram bot and delivery are not implemented yet.

## Tech stack

Java 21 · Spring Boot 4.1.2 · Spring Data JPA / Hibernate · PostgreSQL · Flyway · Spring RestClient · MapStruct · Lombok · Telegram Bot API · Maven · Docker Compose

## Architecture

Monolith covering four business areas:

| Area | Responsibility |
|---|---|
| Article | Article retrieval, processing and persistence |
| User | Telegram users and their account information |
| Subscription | User subscriptions to article categories |
| Notification | Notification creation, tracking and delivery |

Code under `com.biorxiv` is organized by layer:

| Package | Contents |
|---|---|
| `entity` | JPA entities and enums |
| `repository` | Spring Data JPA repositories |
| `service` / `service.impl` | Business services: interfaces and their implementations |
| `client` / `client.impl` / `client.dto` | bioRxiv API client and its JSON records |
| `mapper` | MapStruct mappers (API record → entity) |
| `scheduler` | Scheduled jobs (daily fetch) |
| `config` | Spring configuration (`Clock`, `RestClient`, `biorxiv.*` properties, scheduling) |
| `exception` | Application exceptions |
| `util` | Small shared helpers (e.g. category normalization) |

### Data model

| Table | Purpose | Key constraints |
|---|---|---|
| `users` | Telegram subscribers; `daily_article_limit` (default 10) is the overall per-day limit across all categories | `telegram_id` unique |
| `articles` | Every bioRxiv record field: title, authors, abstract, category, posting date, type, license, corresponding author, journal DOI once published, JATS XML URL. Every version of a preprint is its own row | `(doi, version)` unique; `category`, `publication_date` required |
| `subscriptions` | User ↔ category | `(user_id, category)` unique, FK → `users` |
| `notifications` | Delivery of an article to a user; status `PENDING` / `SENT` / `FAILED`, attempt count, last error | `(user_id, article_id)` unique, FKs → `users`, `articles` |

The schema is managed by Flyway migrations in `src/main/resources/db/migration`; Hibernate only validates entities against it. All timestamps are stored in UTC.

### bioRxiv retrieval

A scheduled job (`biorxiv.fetch.cron`, default daily at 06:00 UTC) calls `GET https://api.biorxiv.org/details/biorxiv/{from}/{to}/{cursor}/json`. It reads every page (30 records each) and stores (doi, version) pairs it hasn't seen before. Each run re-fetches an overlapping window, `today − lookback-days` .. today, because papers can appear in the API after their posting date. After downtime it goes back to the latest stored posting date, at most `max-catch-up-days`. Each page commits separately, and a failed request is retried with backoff.

| Property | Default | Meaning |
|---|---|---|
| `biorxiv.fetch.enabled` | `true` | Run the scheduled fetch |
| `biorxiv.fetch.run-on-startup` | `false` | Also fetch once when the app starts |
| `biorxiv.fetch.cron` / `zone` | `0 0 6 * * *` / `UTC` | Schedule |
| `biorxiv.fetch.lookback-days` | `2` | Overlap re-fetched on every run |
| `biorxiv.fetch.max-catch-up-days` | `7` | Furthest back a run goes after downtime |
| `biorxiv.fetch.max-pages` | `200` | Pagination safety stop |
| `biorxiv.api.retry.max-retries` / `delay-ms` / `multiplier` | `3` / `2000` / `2` | Retries per page for network errors, 5xx and 429 (waits 2s, 4s, 8s) |
| `spring.http.clients.connect-timeout` / `read-timeout` | `10s` / `30s` | HTTP timeouts |

## Getting started

Prerequisites: JDK 21, Docker.

1. Create `.env` from the template and fill in credentials:
   ```bash
   cp .env.example .env
   ```
2. Start PostgreSQL:
   ```bash
   docker compose up -d
   ```
3. Run the application (applies Flyway migrations on startup):
   ```bash
   ./mvnw spring-boot:run
   ```

Run the app from the repository root — `.env` is loaded from the working directory. To fetch articles right away instead of waiting for the schedule:
```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments=--biorxiv.fetch.run-on-startup=true
```

## Tests

```bash
./mvnw test
```

Tests use Testcontainers to start a disposable PostgreSQL instance, so Docker must be running; the local `.env` database is not touched.
