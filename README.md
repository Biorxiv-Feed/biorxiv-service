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

> Status: the persistence model (entities + schema) is in place; retrieval, Telegram bot and delivery are not implemented yet.

## Tech stack

Java 21 · Spring Boot 4.1.2 · Spring Data JPA / Hibernate · PostgreSQL · Flyway · Lombok · Telegram Bot API · Maven · Docker Compose

## Architecture

Modular monolith with one package per business module under `com.biorxiv`:

| Module | Responsibility |
|---|---|
| `article` | Article retrieval, processing and persistence |
| `user` | Telegram users and their account information |
| `subscription` | User subscriptions to article categories |
| `notification` | Notification creation, tracking and delivery |

### Data model

| Table | Purpose | Key constraints |
|---|---|---|
| `users` | Telegram subscribers; `daily_article_limit` (default 10) is the overall per-day limit across all categories | `telegram_id` unique |
| `articles` | Articles from bioRxiv, including original JATS XML | `(doi, version)` unique |
| `subscriptions` | User ↔ category | `(user_id, category)` unique, FK → `users` |
| `notifications` | Delivery of an article to a user; status `PENDING` / `SENT` / `FAILED`, attempt count, last error | `(user_id, article_id)` unique, FKs → `users`, `articles` |

The schema is managed by Flyway migrations in `src/main/resources/db/migration`; Hibernate only validates entities against it.

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

Run the app from the repository root — `.env` is loaded from the working directory.

## Tests

```bash
./mvnw test
```

Tests use Testcontainers to start a disposable PostgreSQL instance, so Docker must be running; the local `.env` database is not touched.
