# Ecommerce API

REST API for managing an e-commerce product catalog: products, categories, filtered/paginated search, CSV bulk import,
idempotent creation, optimistic concurrency and a Valkey/Redis-backed cache.

Built with **Java 25**, **Spring Boot 4.1**, **PostgreSQL**, **Valkey (Redis-compatible)** and **Liquibase**.

---

## Table of contents

1. [Features](#1-features)
2. [Tech stack](#2-tech-stack)
3. [Project structure](#3-project-structure)
4. [Prerequisites](#4-prerequisites)
5. [Quick start (local development)](#5-quick-start-local-development)
6. [Configuration](#6-configuration)
7. [Database](#7-database)
8. [API reference](#8-api-reference)
9. [Error format](#9-error-format)
10. [Idempotency](#10-idempotency)
11. [Optimistic concurrency and HTTP caching](#11-optimistic-concurrency-and-http-caching)
12. [Server-side cache](#12-server-side-cache)
13. [CSV import](#13-csv-import)
14. [Testing](#14-testing)
15. [Building and Docker](#15-building-and-docker)
16. [Running in production](#16-running-in-production)
17. [Observability](#17-observability)
18. [Postman collection](#18-postman-collection)
19. [Troubleshooting](#19-troubleshooting)

---

## 1. Features

- **Product CRUD** with unique, immutable SKUs and soft delete (history is preserved).
- **Category management** (18 categories seeded by the schema; create, rename, delete).
- **Search and filtering** by name, SKU fragment, category, price range, minimum stock and weight range, combined with
  AND, with offset pagination and whitelisted sorting.
- **Bulk CSV import** that streams the file, upserts by SKU in batches, skips invalid rows and returns a detailed report.
- **Idempotent `POST`** through the `Idempotency-Key` header (replays the original response).
- **Optimistic concurrency** using version-based `ETag` / `If-Match` / `If-None-Match`.
- **Two-level caching**: server-side cache (Spring Cache over Valkey/Redis) and HTTP `Cache-Control` headers.
- **Fail-open cache**: cache failures never break reads; PostgreSQL is used as fallback.
- **OpenAPI / Swagger UI** documentation generated from the code.
- **Actuator** health, info and metrics on a separate management port.
- **Virtual threads** enabled for high concurrency.
- **Unit and integration tests** (JUnit 5, Mockito, Testcontainers) with JaCoCo coverage reports.

## 2. Tech stack

| Area | Technology |
|------|------------|
| Language / runtime | Java 25 (Gradle toolchain) |
| Framework | Spring Boot 4.1.2 (Web MVC, Data JPA, Validation, Actuator, Data Redis) |
| Database | PostgreSQL 18 (extensions `pgcrypto`, `pg_trgm`) |
| Cache / idempotency store | Valkey 9 (Redis protocol compatible), Lettuce client |
| Migrations | Liquibase |
| CSV parsing | Apache Commons CSV |
| API docs | springdoc-openapi 3.2.0 |
| Logging | Log4j2 |
| Boilerplate | Lombok |
| Build | Gradle (wrapper included) |
| Tests | JUnit 5, Mockito, AssertJ, Testcontainers, JaCoCo |

## 3. Project structure

```
ecommerce-api/
├── build.gradle                     Build, test, integration-test and JaCoCo configuration
├── settings.gradle
├── Dockerfile                       Multi-stage build (JDK 25 + jlink minimal runtime on Alpine)
├── config/
│   ├── ecommerce-api-docker-dev/    docker-compose.yml + .env for local PostgreSQL and Valkey
│   ├── postman/                     Postman collection and sample CSV
│   └── requirement/                 Requirements, design notes, cache and testing documents
└── src/
    ├── main/
    │   ├── java/com/gila/api/ecommerce/
    │   │   ├── configuration/       Cache, CORS, OpenAPI, clock, typed properties, constants
    │   │   ├── controller/          ProductController, CategoryController
    │   │   ├── service/             Product (query, catalog, import), category, cache/idempotency services
    │   │   ├── repository/          Spring Data repositories, JPA specifications, batch upsert
    │   │   ├── model/               entity, dto, query, cache and error models
    │   │   ├── mapper/              Entity <-> DTO mapping
    │   │   ├── error/               RestExceptionHandler and domain exceptions
    │   │   └── util/                Hashing, dates, query parameter validation, pageables
    │   └── resources/
    │       ├── application*.yaml    Base, dev and prod configuration
    │       ├── log4j2-spring*.xml   Logging configuration
    │       └── db/changelog/        Liquibase master changelog and schema.sql
    ├── test/                        Unit and slice tests (profile `test`)
    └── integrationTest/             End-to-end tests with Testcontainers (profile `IT`)
```

## 4. Prerequisites

| Tool | Version | Notes |
|------|---------|-------|
| JDK | 25 | Gradle can auto-provision it through the toolchain if a provisioner is available |
| Docker + Docker Compose | recent | Needed for local PostgreSQL/Valkey **and** for Testcontainers-based tests |
| Git | any | |

You do **not** need to install Gradle; use the included wrapper (`./gradlew`).

## 5. Quick start (local development)

### 5.1 Start PostgreSQL and Valkey

The compose file lives in `config/ecommerce-api-docker-dev/` and uses an **external** Docker network named
`local-net` and a `${volume}` variable (defined in the `.env` file next to it) for persistent data.

```bash
# one-time: create the external network
docker network create local-net

# start the infrastructure
cd config/ecommerce-api-docker-dev
docker compose up -d

# verify both containers are healthy
docker compose ps
```

| Service | Container | Host port | Credentials |
|---------|-----------|-----------|-------------|
| PostgreSQL 18.6 | `ecommerce-api-postgres` | `5450` | db/user/password: `ecommerce-api` |
| Valkey 9.1.3 | `ecommerce-api-valkey` | `6381` | no password |

Review `config/ecommerce-api-docker-dev/.env` and make sure `volume` points to a directory that exists and is writable on
your machine (it contains `postgresql/` and `valkey/` sub-folders).

To stop: `docker compose down`. To wipe data, also delete the contents of the `volume` directory.

### 5.2 Run the application

From the repository root:

```bash
./gradlew bootRun
```

`bootRun` activates the **`dev`** profile automatically (`--spring.profiles.active=dev`), which connects to
`localhost:5450` (PostgreSQL) and `localhost:6381` (Valkey), enables SQL logging and runs Liquibase on startup.

### 5.3 Verify

| What | URL |
|------|-----|
| API base | `http://localhost:8080/api` |
| Swagger UI | `http://localhost:8080/api/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/api/v3/api-docs` |
| Health | `http://localhost:9090/management/health` |

```bash
curl -s http://localhost:9090/management/health
curl -s "http://localhost:8080/api/v1/categories"
```

### 5.4 First requests

```bash
# Create a product (Idempotency-Key is required and must be a UUID)
curl -i -X POST http://localhost:8080/api/v1/products \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: $(uuidgen)" \
  -d '{
        "sku": "ELECMS001",
        "name": "Wireless Mouse",
        "description": "Ergonomic 2.4GHz wireless mouse",
        "category": "Electronics",
        "price": 24.99,
        "stock": 150,
        "weightKg": 0.120
      }'

# Search
curl -s "http://localhost:8080/api/v1/products/filter?name=mouse&category=electronics&minPrice=10&sortBy=price&sortDirection=ASC"

# Import a CSV
curl -s -X POST http://localhost:8080/api/v1/products/import-csv \
  -F "file=@config/postman/E-Commerce-products.csv;type=text/csv"
```

## 6. Configuration

Configuration lives in `src/main/resources/`:

- `application.yaml` – base configuration (all profiles).
- `application-dev.yaml` – local development (hard-coded localhost infra, SQL logging, Hikari debug).
- `application-prod.yaml` – compression and graceful shutdown, reduced log levels.
- `src/integrationTest/resources/application-IT.yml` – integration test profile.

### 6.1 Server

| Property | Value |
|----------|-------|
| `server.port` | `8080` |
| `server.servlet.context-path` | `/api` |
| `management.server.port` | `9090` (bound to `localhost`) |
| `management.endpoints.web.base-path` | `/management` |
| Exposed actuator endpoints | `health`, `info`, `metrics` (health probes enabled) |
| `spring.threads.virtual.enabled` | `true` |
| Multipart limits | 100 MB file / 100 MB request |

### 6.2 Environment variables

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRESQL_HOST` | – (required) | PostgreSQL host (non-dev profiles) |
| `POSTGRESQL_PORT` | – (required) | PostgreSQL port |
| `POSTGRESQL_DBNAME` | – (required) | Database name |
| `POSTGRESQL_USERNAME` | – (required) | Database user |
| `POSTGRESQL_PASSWORD` | – (required) | Database password |
| `REDIS_HOST` | `localhost` | Valkey/Redis host |
| `REDIS_PORT` | `6379` | Valkey/Redis port |
| `REDIS_PASSWORD` | empty | Valkey/Redis password |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` | Comma-separated allowed origins |
| `PRODUCT_CACHE_ENABLED` | `true` | `false` swaps in a no-op cache manager |
| `SPRING_PROFILES_ACTIVE` | – | e.g. `dev`, `prod` |

### 6.3 Application properties (`app.*`)

| Property | Default | Description |
|----------|---------|-------------|
| `app.cors.allowed-origins` | `http://localhost:5173` | Allowed CORS origins |
| `app.cors.max-age` | `1h` | Preflight cache duration |
| `app.idempotency.key-prefix` | `idempotency-key:` | Key prefix in Valkey |
| `app.idempotency.lock-ttl` | `30s` | In-flight lock duration for `POST /products` |
| `app.idempotency.record-ttl` | `24h` | How long a stored response is replayable |
| `app.ingestion.batch-size` | `1000` | Rows per upsert batch in CSV import |
| `app.ingestion.max-reported-errors` | `1000` | Max rejected rows detailed in the report |
| `app.ingestion.max-concurrent-batches` | `4` | Concurrent import batches |
| `app.ingestion.idempotency-lock-ttl` | `10m` | In-flight lock for CSV imports |
| `app.cache.enabled` | `true` | Enable server-side cache |
| `app.cache.key-prefix` | `product:v1:` | Cache key prefix |
| `app.cache.detail-ttl` | `6h` | TTL of product detail entries (valid range 2–12 h) |
| `app.cache.query-ttl` | `30s` | TTL of search/page entries |

### 6.4 Database pool

HikariCP is configured with up to 30 connections (min idle 10), 30 s connection timeout, 10 min idle timeout, 30 min max
lifetime and leak detection at 60 s.

## 7. Database

Schema and seed data are managed by **Liquibase** (`db/changelog/db.changelog-master.yaml` → `scripts/schema.sql`) and
run automatically on startup. Hibernate DDL is disabled (`ddl-auto: none`).

Highlights of the schema:

- Extensions: `pgcrypto` (UUID generation) and `pg_trgm` (trigram indexes for fast substring search).
- `product_categories`: unique, case-insensitively unique name; seeded with 18 categories (Accessories, Beauty, Books,
  Clothing, Electronics, Food & Beverage, Footwear, Games, Gifts, Health, Home & Office, Kitchen, Misc, Outdoors, Pets,
  Sports, Stationery, Tools).
- `products`:
  - `id` UUID, `sku` `VARCHAR(10)` unique with `CHECK (sku ~ '^[A-Z0-9]{1,10}$')`
  - `price NUMERIC(12,2) > 0`, `stock INTEGER >= 0`, `weight_kg NUMERIC(8,3) >= 0` (nullable)
  - `version` (optimistic locking / ETag), `is_active` + `deleted_at` (soft delete, kept consistent by a `CHECK`)
  - Partial indexes (`WHERE is_active`) for creation date, price, name, category+price, stock, weight and trigram
    indexes on `LOWER(name)` and `sku`.
- The schema also includes a `purchase_orders` table and related structures.

> **Changing `schema.sql` in place invalidates Liquibase checksums** for existing databases. In dev, recreate the
> database: `DROP SCHEMA public CASCADE; CREATE SCHEMA public;` (or wipe the Docker volume) and start the app again.
> Before migrating a database that already has data, check incompatible SKUs:
> `SELECT sku FROM products WHERE sku !~ '^[A-Z0-9]{1,10}$';`

## 8. API reference

Base URL: `http://localhost:8080/api`. All bodies are JSON unless noted. The authoritative, always up-to-date
reference is **Swagger UI** at `/api/swagger-ui.html`.

### 8.1 Categories — `/v1/categories`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/v1/categories` | List categories (paginated) |
| `GET` | `/v1/categories/{id}` | Get by id |
| `GET` | `/v1/categories/name/{name}` | Get by name (case-insensitive exact match) |
| `POST` | `/v1/categories` | Create a category |
| `PUT` | `/v1/categories/{id}` | Update a category |
| `DELETE` | `/v1/categories/{id}` | Delete; **409** while products reference it |

### 8.2 Products — `/v1/products`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/v1/products/filter` | Search and filter products (paginated) |
| `GET` | `/v1/products/{id}` | Product detail by **UUID** (SKU is not accepted); supports `If-None-Match` |
| `POST` | `/v1/products` | Create; requires `Idempotency-Key` (UUID) header |
| `PUT` | `/v1/products/{id}` | Full replacement; SKU is immutable; optional `If-Match` |
| `DELETE` | `/v1/products/{id}` | Soft delete (`204`) |
| `POST` | `/v1/products/import-csv` | Bulk import from CSV (`multipart/form-data`, part name `file`) |

### 8.3 Product payload

```json
{
  "sku": "ELECMS001",
  "name": "Wireless Mouse",
  "description": "Ergonomic 2.4GHz wireless mouse",
  "category": "Electronics",
  "price": 24.99,
  "stock": 150,
  "weightKg": 0.120
}
```

| Field | Rules |
|-------|-------|
| `sku` | Required, 1–10 chars, uppercase letters and digits only, unique, **immutable** after creation |
| `name` | Required, max 255 |
| `description` | Optional, max 5000; blank becomes `null` |
| `category` | Required, max 100; must match an existing category (case-insensitive) |
| `price` | Required, ≥ 0.01, up to 10 integer digits and 2 decimals |
| `stock` | Required, ≥ 0 |
| `weightKg` | Optional, ≥ 0, up to 5 integer digits and 3 decimals |

Text fields are stripped of leading/trailing whitespace.

### 8.4 Search: `GET /v1/products/filter`

All parameters are optional and combined with **AND**.

| Parameter | Description |
|-----------|-------------|
| `name` | Case-insensitive substring match on the name (max 100 chars). Words separated by spaces are combined with **OR** (e.g. `camp tent`). `%`, `_` and `\` are literals. No stemming or typo tolerance. Does not match SKU, category or description |
| `sku` | *Contains* match, 3–10 chars of `[A-Z0-9]` |
| `category` | Exact match, case-insensitive |
| `minPrice` / `maxPrice` | Inclusive price bounds (≥ 0; min ≤ max) |
| `minStock` | `stock >= minStock` |
| `minWeightKg` / `maxWeightKg` | Inclusive weight bounds; products without weight never match; min ≤ max |
| `page` | Zero-based, default `0`, max `1000` |
| `size` | Default `20`, between `1` and `100` |
| `sortBy` | `createdAt` (default, DESC), `price` (ASC), `name` (ASC) |
| `sortDirection` | `ASC` or `DESC` (case-insensitive) |

- Only active (non-deleted) products are returned.
- **Unknown query parameters are rejected with `400`**, never silently ignored.
- The sort always uses `id` as a tie-breaker so pages are deterministic.
- Out-of-range pages return an empty page with real totals and `isLast=true`.

Paginated response shape:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalPages": 3,
  "totalElements": 45,
  "isLast": false
}
```

### 8.5 Examples

```bash
# Detail (returns ETag)
curl -i http://localhost:8080/api/v1/products/<uuid>

# Conditional GET (304 if unchanged)
curl -i http://localhost:8080/api/v1/products/<uuid> -H 'If-None-Match: "<etag>"'

# Replace with optimistic concurrency
curl -i -X PUT http://localhost:8080/api/v1/products/<uuid> \
  -H "Content-Type: application/json" -H 'If-Match: "<etag>"' \
  -d '{"sku":"ELECMS001","name":"Wireless Mouse v2","category":"Electronics","price":22.50,"stock":120}'

# Soft delete
curl -i -X DELETE http://localhost:8080/api/v1/products/<uuid>

# Create a category
curl -i -X POST http://localhost:8080/api/v1/categories \
  -H "Content-Type: application/json" -d '{"name":"Musical Instruments"}'
```

## 9. Error format

Errors are returned as a `CustomErrorResponse` produced by `RestExceptionHandler`, with a message, a list of details
(field/parameter and reason) and request metadata. Typical statuses:

| Status | Cause |
|--------|-------|
| `400` | Validation errors, invalid UUID, invalid sort/category, unsupported query parameter, SKU change attempt, invalid or missing `Idempotency-Key`, invalid CSV file |
| `404` | Product or category not found (or already soft-deleted) |
| `409` | Stale `If-Match` version (optimistic lock failure), duplicate SKU/category, category in use, concurrent request with the same idempotency key, idempotency key reused with a different payload |
| `422` | CSV ingestion rejected (all rows invalid) |

## 10. Idempotency

- `POST /v1/products` **requires** an `Idempotency-Key` header containing a UUID generated by the client.
- Retrying with the same key and payload replays the original `201` response and adds the `Idempotent-Replayed` header.
- Reusing a key with a different payload, or sending a concurrent request while the first is in flight, results in a
  conflict error.
- `POST /v1/products/import-csv` accepts an **optional** `Idempotency-Key`; a replay returns the first import report.
- State is stored in Valkey (`app.idempotency.*`): in-flight lock 30 s, response record 24 h. Idempotency is
  **fail-closed**: it depends on Valkey being available.
- `PUT` and `DELETE` are idempotent by definition and do not need the header.

## 11. Optimistic concurrency and HTTP caching

- Product responses carry an `ETag` derived from the row `version`.
- `GET /v1/products/{id}` honors `If-None-Match` (returns `304 Not Modified`).
- `PUT /v1/products/{id}` honors `If-Match`; a malformed value returns `400` and a stale version returns `409`.
- `Cache-Control` max-age: 30 s for filter results, 60 s for product detail.

## 12. Server-side cache

Spring Cache over Valkey with two regions:

| Region | Used by | Key | TTL |
|--------|---------|-----|-----|
| `product-detail` | `GET /v1/products/{id}` | UUID | `app.cache.detail-ttl` (6 h) |
| `product-query` | `GET /v1/products/filter` | Normalized criteria + page, size and full sort | `app.cache.query-ttl` (30 s) |

- Keys are prefixed with `app.cache.key-prefix` (`product:v1:product-detail::<uuid>`). Equivalent requests (`10` vs
  `10.00`, different category capitalization, explicit vs omitted defaults) share the same key.
- **Invalidation** after commit: create clears `product-query`; update/delete clear that UUID in `product-detail` and all
  of `product-query`; CSV import clears the regions per committed batch (using SCAN, never `KEYS`).
- **Fail-open**: if Valkey is down, reads fall back to PostgreSQL and errors are logged and counted in the
  `catalog.cache.error` metric. Set `PRODUCT_CACHE_ENABLED=false` to disable the cache entirely.
- Search pages are eventually consistent (up to the query TTL). Never use cached data for inventory or financial
  decisions.

See `config/requirement/cache-details.md` for the full design.

## 13. CSV import

`POST /v1/products/import-csv` — `multipart/form-data`, part name **`file`**.

**Header** (order-independent, case-insensitive, UTF-8 BOM accepted):

```
name,sku,description,category,price,stock,weight_kg
```

`description` and `weight_kg` are optional; the others are required. A sample file is in
`config/postman/E-Commerce-products.csv`.

Behavior:

- The file is streamed and processed in batches (`app.ingestion.batch-size`, default 1000), upserting by **SKU**
  (existing SKUs are updated, new ones are inserted); rows within a batch are ordered by SKU.
- Each row is validated with the same rules as the JSON API (price, stock, weight, category, SKU format).
- Duplicate SKUs inside the same file: the first occurrence wins.
- Invalid rows are skipped and listed in the report (up to `app.ingestion.max-reported-errors`; `errorsTruncated=true`
  when more exist, while `rejectedRows` always counts all of them).
- A failing batch rejects only its own rows; remaining batches continue.
- Empty file / wrong type / missing header columns → `400`; every row invalid → `422`.

```bash
curl -s -X POST http://localhost:8080/api/v1/products/import-csv \
  -H "Idempotency-Key: $(uuidgen)" \
  -F "file=@config/postman/E-Commerce-products.csv;type=text/csv"
```

## 14. Testing

Tests that touch PostgreSQL or Valkey use **Testcontainers**, so **Docker must be running**.

| Type | Location | Command | Profile |
|------|----------|---------|---------|
| Unit / slice | `src/test/java` | `./gradlew test` | `test` |
| Integration (E2E) | `src/integrationTest/java` | `./gradlew integrationTest` | `IT` |
| Everything | – | `./gradlew check` | – |

```bash
./gradlew test                    # unit and slice tests (up to 3 parallel forks)
./gradlew integrationTest         # integration tests against real containers
./gradlew check                   # both of the above
./gradlew test --tests '*ProductServiceImplTest'   # a single class
```

### Coverage (JaCoCo)

Reports are generated automatically after each test task (packages `config`, `dto`, `model`, `domain`, `exception` are
excluded):

- Unit tests: `build/reports/jacoco/test/html/index.html`
- Integration tests: `build/reports/jacoco/integrationTest/html/index.html`
- XML versions sit next to the HTML directories.

Test results: `build/reports/tests/test/index.html` and `build/reports/tests/integrationTest/index.html`.

See `config/requirement/testing_instructions.md` for the complete test plan.

## 15. Building and Docker

### Build the jar

```bash
./gradlew clean bootJar            # build/libs/ecommerce-api-0.0.1-SNAPSHOT.jar
./gradlew build -x integrationTest # compile + unit tests + jar
```

`buildInfo()` is enabled, so build metadata appears in `/management/info`.

### Run the jar

```bash
export POSTGRESQL_HOST=localhost POSTGRESQL_PORT=5450 POSTGRESQL_DBNAME=ecommerce-api \
       POSTGRESQL_USERNAME=ecommerce-api POSTGRESQL_PASSWORD=ecommerce-api \
       REDIS_HOST=localhost REDIS_PORT=6381
java -jar build/libs/ecommerce-api-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod
```

### Docker image

The `Dockerfile` is multi-stage: it builds the fat jar with JDK 25, computes the required modules with `jdeps`, creates a
minimal runtime with `jlink`, and ships it on Alpine as a **non-root** user, exposing port `8080`.

```bash
docker build -t ecommerce-api:latest .

docker run --rm -p 8080:8080 \
  --network local-net \
  -e SPRING_PROFILES_ACTIVE=prod \
  -e POSTGRESQL_HOST=ecommerce-api-postgres -e POSTGRESQL_PORT=5432 \
  -e POSTGRESQL_DBNAME=ecommerce-api \
  -e POSTGRESQL_USERNAME=ecommerce-api -e POSTGRESQL_PASSWORD=ecommerce-api \
  -e REDIS_HOST=ecommerce-api-valkey -e REDIS_PORT=6379 \
  ecommerce-api:latest
```

(Inside the `local-net` network the containers use their internal ports `5432` and `6379`.)

## 16. Running in production

- Activate the `prod` profile and provide all `POSTGRESQL_*` variables; set `REDIS_*` and `CORS_ALLOWED_ORIGINS`.
- Never use the dev credentials; inject secrets through your platform's secret manager.
- `server.shutdown=graceful` lets in-flight requests finish on shutdown; response compression is enabled for bodies
  ≥ 1 KB.
- Swagger UI is served by default; restrict or disable it at the gateway if the API is public.
- The management port (`9090`) is bound to `localhost`. Expose it only to trusted networks (e.g. through a sidecar or
  internal probe) and use `/management/health/liveness` and `/management/health/readiness` for Kubernetes probes.
- Remember that idempotency requires Valkey to be available (fail-closed); plan for its availability.
- Liquibase applies the schema at startup; make sure the database user can create extensions
  (`pgcrypto`, `pg_trgm`) the first time, or create them beforehand with a privileged user.

## 17. Observability

| Endpoint | Description |
|----------|-------------|
| `GET :9090/management/health` | Health (with liveness/readiness probes) |
| `GET :9090/management/info` | Build and Java information |
| `GET :9090/management/metrics` | Metrics list (e.g. `catalog.cache.error`) |

Logging uses Log4j2 (`log4j2-spring.xml` by default, `log4j2-spring-dev.xml` for the `dev` profile). Root level is `WARN`
outside dev.

## 18. Postman collection

Import `config/postman/ecommerce-api.postman_collection.json` into Postman. Use
`config/postman/E-Commerce-products.csv` as the file for the import request.

## 19. Troubleshooting

| Symptom | Fix |
|---------|-----|
| `network local-net declared as external, but could not be found` | `docker network create local-net` |
| Compose warns that `volume` is not set / mount errors | Check `config/ecommerce-api-docker-dev/.env` and that the directory exists |
| `Connection refused` to PostgreSQL / Valkey | Check `docker compose ps`; dev ports are `5450` (PostgreSQL) and `6381` (Valkey) |
| Liquibase checksum validation failed | Schema was edited in place; recreate the DB (`DROP SCHEMA public CASCADE; CREATE SCHEMA public;`) |
| `400` with "unsupported query parameter" | Only documented parameters are accepted; check spelling and case |
| `400` missing/invalid `Idempotency-Key` on `POST /products` | Send a valid UUID in the `Idempotency-Key` header |
| `409` on create with an existing key | Same key with different payload, or a concurrent in-flight request; use a new UUID |
| Idempotent endpoints fail when Valkey is down | Expected (fail-closed); restore Valkey |
| Integration tests fail to start | Make sure Docker is running and can pull images |
| `Unsupported class file major version` / toolchain errors | Use JDK 25 (`java -version`) |
| Port `8080` or `9090` already in use | Stop the other process or override `server.port` / `management.server.port` |

---

Additional design documents (requirements, class diagram, endpoints, cache details, implementation plans) are available
in `config/requirement/`.
