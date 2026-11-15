# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

BioRxiv service: fetches newly published preprints from the bioRxiv API on a daily schedule (with pagination), stores them in PostgreSQL, matches them to users' category subscriptions, and delivers notifications through a Telegram bot, subject to a per-user daily article limit. See `README.md` for the product overview.

Stack: Java 21, Spring Boot 4.1 (Web MVC, RestClient, Data JPA/Hibernate, Flyway, Bean Validation, Actuator, DevTools), PostgreSQL, Lombok, MapStruct, Maven, Docker Compose. Telegram Bot API is planned but not yet a dependency.

Work is tracked as numbered tasks; branches are named `<n>-<task-slug>` (e.g. `1-create-jpa-entity-for-tables`).

## Commands

Use the Maven wrapper (`./mvnw` in Bash, `.\mvnw.cmd` in PowerShell). Tests need Docker running (Testcontainers).

```bash
./mvnw clean package                                     # build + tests
./mvnw test -Dtest=PersistenceMappingTests               # single class
./mvnw test -Dtest=PersistenceMappingTests#telegramIdIsUnique   # single method
./mvnw spring-boot:run                                   # run against DB from .env
./mvnw spring-boot:run -Dspring-boot.run.arguments=--spring.main.web-application-type=none  # start, migrate, validate (stays up: scheduler)
./mvnw spring-boot:run -Dspring-boot.run.arguments=--biorxiv.fetch.run-on-startup=true       # fetch from the live bioRxiv API at startup
./mvnw spring-boot:test-run                              # run with a throwaway Testcontainers Postgres
docker compose up -d                                     # local Postgres (container biorxiv-postgres)
```

On this Windows machine `docker` is not on the shell PATH; prefix `PATH="/c/Program Files/Docker/Docker/resources/bin:$PATH"` in Bash. Inspect the local DB with `docker exec biorxiv-postgres sh -c 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c "..."'`.

No linter/formatter is configured.

## Architecture

Monolith covering four business areas — article (bioRxiv retrieval/persistence), user (Telegram users, `daily_article_limit`), subscription (user ↔ category), notification (per-(user, article) delivery tracking).

Code is organized **by layer**, not by business area (chosen by the user):

- `com.biorxiv.entity` — all JPA entities and enums (`Article`, `User`, `Subscription`, `Notification`, `NotificationStatus`)
- `com.biorxiv.repository` — all Spring Data repositories (`<Entity>Repository`)
- `com.biorxiv.service` — service interfaces (`<Entity>Service`, one per entity); `com.biorxiv.service.impl` — their `@Service` implementations (`<Entity>ServiceImpl`)
- `com.biorxiv.client` — `BiorxivClient` interface (`client.impl` HTTP implementation, `client.dto` JSON records); `com.biorxiv.mapper` — MapStruct mappers; `com.biorxiv.scheduler` — `@Scheduled` jobs
- `com.biorxiv.config` (`ClockConfig`, `BiorxivClientConfig`, `BiorxivProperties`, `SchedulingConfig`), `com.biorxiv.exception` (`NotFoundException`), `com.biorxiv.util` (`CategoryUtils`)
- Future layers follow the same pattern, not per-module packages.

Repositories hold only query methods that a known feature needs (dedup checks, subscriber lookup by category, delivery queue, daily-limit count) — business rules belong in services.

### Service conventions

- Callers depend on the interface; implementations use `@Slf4j`, `@RequiredArgsConstructor` (constructor injection) and class-level `@Transactional`, with `@Transactional(readOnly = true)` on read methods. Updates rely on dirty checking (modify the managed entity, no explicit `save`).
- Services may call other services (e.g. `SubscriptionService` → `UserService.getById`) rather than reaching into another entity's repository.
- Missing entity → `NotFoundException`; invalid argument → `IllegalArgumentException`.
- Logging (`@Slf4j`; level set by `logging.level.com.biorxiv`, default `info`):
  - `info`: state changes and run summaries. Per item only where each item matters: every **new** article in a fetch, every sent notification, (un)subscribe, user changes.
  - `debug`: per-page/per-batch detail, already-stored articles, no-op calls (already subscribed, already inactive), window choice.
  - `trace`: `ArticleServiceImpl` per-item saves/duplicates (the fetch already logs them).
  - `warn`: something was dropped or degraded but the app carries on: skipped records, catch-up limit hit, failed HTTP attempts (`RetryEventLogger`), failed deliveries, ignored updates, unexpected API status.
  - `error`: a whole run failed (scheduler), with the stack trace.
  - Log ids, not usernames or message content. Article titles are public data and are logged, cut to 100 characters.
- "Now" comes from the injected `Clock` bean, never `LocalDateTime.now()` — tests replace it with a fixed clock. (`created_at` is the exception: Hibernate's `@CreationTimestamp` uses system time.)
- Categories are matched as exact strings, so every write and lookup goes through `CategoryUtils.normalize` (trim, collapse spaces, lower case). Article and subscription categories must use the same form.
- Business rules currently in services: daily limit range `UserService.MIN/MAX_DAILY_ARTICLE_LIMIT` (1–50); a failed notification is retried while `PENDING` and becomes `FAILED` after `NotificationService.MAX_ATTEMPTS` (3); status updates on a non-`PENDING` notification are ignored with a warning. Entities reference each other via JPA relationships (`Subscription`/`Notification` → `User`, `Notification` → `Article`). JPQL that names an enum constant uses its FQN (`com.biorxiv.entity.NotificationStatus.SENT`) — update it if the package moves.

### bioRxiv retrieval

- Flow: `ArticleFetchScheduler` → `ArticleFetchService.fetchLatest()` → `BiorxivClient.fetchPage` (per page) → `BiorxivArticleMapper` → `ArticleService.saveAllNew` (dedup on doi+version, normalizes category/type).
- API facts (verified against live responses): 30 records per page; paginate by `cursor += page size` until `cursor >= total` or the page is empty. `total`, `cursor`, `count_new_papers` and every record field arrive as **strings** (Jackson coerces the numbers). An empty window returns `{"messages":[{"status":"no posts found"}],"collection":[]}`. `published` is `"NA"` until there's a journal DOI. `type` and `category` casing varies over the years ("New Results" / "new results", "Cell Biology " with a trailing space), hence normalization. Revisions (version > 1) are separate records and are stored.
- `ArticleFetchServiceImpl` is deliberately **not** `@Transactional`: no DB transaction stays open across HTTP calls, and each page commits on its own. Records that won't map, or that lack a NOT NULL field, are skipped with a `warn`.
- Fetch window: `today − lookback-days` .. today, extended back to `max(publication_date) − 1` after downtime, but never earlier than `today − max-catch-up-days`. There is no separate sync-state table.
- `@Retryable` (Spring Framework 7, enabled by `@EnableResilientMethods` in `SchedulingConfig`) is on `BiorxivClientImpl.fetchPage`. It retries only network errors (`ResourceAccessException`), 5xx and 429; other 4xx and parse errors fail immediately. Settings are `biorxiv.api.retry.*` (placeholders in the annotation).
- **Pitfall:** Spring looks for `@Retryable` on the *invoked* method. With a JDK interface proxy that's `BiorxivClient.fetchPage`, so an annotation on the impl is silently ignored. The app uses CGLIB class proxies (Boot default `spring.aop.proxy-target-class=true`), where it works. Slice tests that need retry must `@ImportAutoConfiguration(AopAutoConfiguration.class)` (see `BiorxivClientRetryTests`).
- HTTP timeouts are Boot's `spring.http.clients.*`. Don't set a custom request factory on the `RestClient`, because it would bypass `MockRestServiceServer` in `@RestClientTest`.
- MapStruct: the processor runs after Lombok (`lombok-mapstruct-binding` in `annotationProcessorPaths`). In `BiorxivArticleMapper`, `trimToNull` applies to every String→String mapping.

### Persistence conventions

- **Flyway owns the schema** (`src/main/resources/db/migration`, `V<n>__<desc>.sql`); `spring.jpa.hibernate.ddl-auto=validate` makes startup fail if entities drift from migrations. Any entity change needs a new migration — never edit an applied one.
- Entities: Lombok `@Getter @Setter @Builder`, protected no-arg constructor, `@ToString(onlyExplicitlyIncluded = true)` with relationships excluded. No `@Data`, no custom `equals/hashCode` (identity semantics). Defaults that mirror DB defaults use `@Builder.Default`.
- IDs: `BIGINT GENERATED BY DEFAULT AS IDENTITY` + `GenerationType.IDENTITY`.
- Timestamps are `TIMESTAMP` (no time zone) ↔ `LocalDateTime`, **always UTC**: `Clock.systemUTC()`; `main` sets the JVM default zone to UTC (needed by `@CreationTimestamp`); surefire runs tests with `-Duser.timezone=UTC`. Dates ↔ `LocalDate`. `publication_date` is bioRxiv's calendar date, stored as given. A future per-user time zone should convert the user's local day into a UTC range for queries such as `countSentToUserBetween`.
- `TEXT` columns need `columnDefinition = "TEXT"` or schema validation fails.
- `@ManyToOne` is always `LAZY`; `@OneToMany` sides are `mappedBy`, read-only (no setter), and have **no cascade** — deleting a user/article must not silently remove history.
- Enums persist with `EnumType.STRING`; renaming a constant requires a data migration.
- Constraint/index names are declared both in SQL and in `@Table` (`uk_*`, `fk_*`, `idx_*`, `ck_*`). Single-column indexes on `user_id` are intentionally omitted where a composite unique constraint already leads with it.

## Configuration

- **Never read `.env`** (no Read/cat/Get-Content/grep on it, not even masked). Use `.env.example` for variable names; if an actual value is needed, ask the user.
- `application.yaml` imports `./.env` (`spring.config.import: optional:file:./.env[.properties]`) for `DB_HOST/DB_PORT/DB_NAME/DB_USERNAME/DB_PASSWORD`; the same `.env` drives `docker-compose.yml`. Run from repo root. Template: `.env.example`.
- `application-dev.yaml` / `application-prod.yaml` exist but are empty; no profile is active by default.
- Tests ignore `.env`: `TestcontainersConfiguration` supplies a `@ServiceConnection` Postgres (`postgres:latest`; compose uses `postgres:18`). DB tests must `@Import(TestcontainersConfiguration.class)` (it is `public`, so tests can live in the package they test, e.g. `com.biorxiv.repository.UserRepositoryTests`). Service tests are `@DataJpaTest` + `@Import({TestcontainersConfiguration.class, ServiceTestConfiguration.class})`. That adds the service impls, the MapStruct mapper, `BiorxivProperties`, a `FakeBiorxivClient` (call `reset()` in `@BeforeEach`) and a fixed UTC `Clock` at `ServiceTestConfiguration.NOW` — keep that import list identical across test classes so they share one Spring context (one container); `@DataJpaTest` + `TestEntityManager` (Boot 4 packages: `org.springframework.boot.data.jpa.test.autoconfigure`, `org.springframework.boot.jpa.test.autoconfigure`) runs Flyway against the container. Build valid articles with `TestArticles.article(doi, version)` (fills the NOT NULL columns). `BiorxivServiceApplicationTests` sets `biorxiv.fetch.enabled=false` so the scheduler never calls the real API. `BiorxivClientTests` is a `@RestClientTest` (`org.springframework.boot.restclient.test.autoconfigure`) against JSON fixtures in `src/test/resources/biorxiv/` copied from real responses.
