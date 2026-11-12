# Ecommerce Local Stack (Docker Compose)

This folder contains the Docker Compose file that runs the complete Ecommerce application on your machine. It starts five containers:

| Service | Container name | Purpose | Host URL / port |
|---|---|---|---|
| `ecommerce-api-postgres` | `ecommerce-api-postgres` | PostgreSQL 18 database | `localhost:5450` |
| `ecommerce-api-valkey` | `ecommerce-api-valkey` | Valkey 9 (Redis-compatible) cache | `localhost:6381` |
| `ecommerce-api` | `ecommerce-api` | Spring Boot REST API | http://localhost:8080/api |
| `ecommerce-ui` | `ecommerce-ui` | React single-page application served by nginx | http://localhost:8081 |

Containers talk to each other through the Docker network `local-net`, using the service names as host names. You only need the host ports above when you connect from your own machine (for example, with a database client).

---

## 1. Prerequisites

1. **Docker Desktop** (macOS, Windows) or **Docker Engine + Compose plugin** (Linux), version 24 or newer. Check it with:

   ```bash
   docker --version
   docker compose version
   ```

2. **Docker must be running.** On Docker Desktop, wait until the whale icon shows that Docker is running.

3. **Project folder layout.** The Compose file uses relative paths to find the application source code. The three folders must be siblings:

   ```text
   workspace/
   ├── ecommerce-api-docker-dev/   <- this folder (docker-compose.yml)
   ├── ecommerce-api/              <- Spring Boot API (Dockerfile inside)
   └── ecommerce-ui/               <- React SPA (Dockerfile + nginx.conf inside)
   ```

   If your folders are placed differently, update the `context:` paths in `docker-compose.yml`.

4. **Free host ports:** `5450`, `6381`, `8080`, and `8081`. See [Port already in use](#port-already-in-use).

No Java, Node.js, or database installation is required on your machine. Everything is built and run inside Docker.

---

## 2. Files in this folder

| File / folder | Description |
|---|---|
| `docker-compose.yml` | Defines all five services, their network, health checks, and build settings. |
| `.env` | Sets `volume=./volume`. Compose uses this value to locate the persistent data folders. |
| `volume/postgresql/init_db/` | Scripts placed here run automatically the first time PostgreSQL initializes an empty data directory. |
| `volume/postgresql/data/` | PostgreSQL data files. **Do not edit these by hand.** |
| `volume/valkey/` | Valkey snapshot files (`dump.rdb`). |

Data persists in `volume/` between restarts. Deleting this folder deletes all database content.

---

## 3. First-time setup

Run all commands from this folder (`ecommerce-api-docker-dev`).

### Step 1: Create the external network

The network `local-net` is declared as `external: true`, which means Compose will not create it. Create it once:

```bash
docker network create local-net
```

If it already exists, Docker prints an error such as `network with name local-net already exists`. That is harmless; continue to the next step.

To check whether it exists:

```bash
docker network ls --filter name=local-net
```

### Step 2: Validate the configuration (optional but recommended)

```bash
docker compose config --quiet && echo "Configuration OK"
```

This prints nothing except `Configuration OK` when the file is valid. If it shows an error, read the message: it usually points to the line with the problem.

### Step 3: Build and start all containers

```bash
docker compose up -d --build
```

- `up` creates and starts the containers.
- `-d` runs them in the background.
- `--build` builds the `ecommerce-api` and `ecommerce-ui` images before starting them.

The first build takes several minutes because Gradle downloads dependencies and pnpm installs `node_modules`. Later builds are much faster thanks to Docker's layer cache.

### Step 4: Wait until the services are ready

The API starts only after PostgreSQL and Valkey report `healthy`. Check the status with:

```bash
docker compose ps
```

Expected output (the API may take about 30–60 seconds after the others are healthy):

```text
NAME                     STATUS
ecommerce-api            Up About a minute
ecommerce-api-postgres   Up 2 minutes (healthy)
ecommerce-api-valkey     Up 2 minutes (healthy)
ecommerce-ui             Up 2 minutes
```

To follow the API startup until it finishes:

```bash
docker compose logs -f ecommerce-api
```

Press `Ctrl+C` to stop following the logs (this does **not** stop the containers). Look for a line similar to `Started ... Application`.

### Step 5: Open the application

| What | URL |
|---|---|
| Web application | http://localhost:8081 |
| API base path | http://localhost:8080/api |
| Swagger UI | http://localhost:8080/api/swagger-ui.html |

Quick check of the API from a terminal:

```bash
curl -i "http://localhost:8080/api/v1/products/filter?page=0&size=5"
```

A `200 OK` response with JSON means the API is connected to the database.

---

## 4. Daily commands

| Task | Command |
|---|---|
| Start all containers (already built) | `docker compose up -d` |
| Stop all containers (keeps data) | `docker compose stop` |
| Start again after `stop` | `docker compose start` |
| Stop and remove containers (keeps data) | `docker compose down` |
| Show container status | `docker compose ps` |
| Follow logs of all services | `docker compose logs -f` |
| Follow logs of one service | `docker compose logs -f ecommerce-api` |
| Open a shell in the API container | `docker exec -it ecommerce-api sh` |
| Open a `psql` session | `docker exec -it ecommerce-api-postgres psql -U ecommerce-api -d ecommerce-api` |
| Open the Valkey CLI | `docker exec -it ecommerce-api-valkey valkey-cli` |

### Rebuilding after code changes

Compose does not rebuild images automatically when source code changes. Rebuild only the service you changed:

```bash
# After changing the React app in ecommerce-ui
docker compose up -d --build ecommerce-ui

# After changing the Java code in ecommerce-api
docker compose up -d --build ecommerce-api
```

> **Important for the UI:** Vite embeds `VITE_API_BASE_URL` into the JavaScript bundle at build time. If you change that value, you must rebuild `ecommerce-ui`. Changing it at runtime has no effect.

### Stopping everything

```bash
docker compose down
```

This removes the containers and the `local-net` connections, but **keeps** the database and cache data in `volume/`. The network `local-net` is external, so it is not removed.

---

## 5. Resetting the data

Use this only when you want a completely empty database. **All data will be lost.**

```bash
docker compose down
rm -rf volume/postgresql/data volume/valkey
docker compose up -d
```

On the next start, PostgreSQL initializes an empty database, and the API runs its Liquibase migrations to recreate the tables.

> If your `volume/postgresql/data` folder was created by a different PostgreSQL major version, the database will fail to start. Reset the data as shown above.

---

## 6. Configuration reference

### Database (PostgreSQL)

| Setting | Value |
|---|---|
| Database | `ecommerce-api` |
| User | `ecommerce-api` |
| Password | `ecommerce-api` |
| Host from your machine | `localhost` |
| Port from your machine | `5450` |
| Host from other containers | `ecommerce-api-postgres` |
| Port from other containers | `5432` |

Example connection string from your machine:

```text
jdbc:postgresql://localhost:5450/ecommerce-api
```

### Cache (Valkey)

| Setting | Value |
|---|---|
| Host from your machine | `localhost` |
| Port from your machine | `6381` |
| Host from other containers | `ecommerce-api-valkey` |
| Port from other containers | `6379` |
| Password | none |

### API environment variables

These are set in `docker-compose.yml` for the `ecommerce-api` service:

| Variable | Default in compose | Meaning |
|---|---|---|
| `POSTGRESQL_HOST` / `POSTGRESQL_PORT` / `POSTGRESQL_DBNAME` | `ecommerce-api-postgres` / `5432` / `ecommerce-api` | Database location |
| `POSTGRESQL_USERNAME` / `POSTGRESQL_PASSWORD` | `ecommerce-api` / `ecommerce-api` | Database credentials |
| `REDIS_HOST` / `REDIS_PORT` | `ecommerce-api-valkey` / `6379` | Cache location |
| `CORS_ALLOWED_ORIGINS` | `${CORS_ALLOWED_ORIGINS:-http://localhost:8081}` | Origins allowed to call the API from a browser |
| `PRODUCT_CACHE_ENABLED` | `${PRODUCT_CACHE_ENABLED:-true}` | Enables the product cache |

Variables written as `${NAME:-default}` can be overridden without editing the file, for example:

```bash
PRODUCT_CACHE_ENABLED=false docker compose up -d
```

### UI build argument

| Argument | Default | Meaning |
|---|---|---|
| `VITE_API_BASE_URL` | `http://localhost:8080/api` | URL the browser uses to reach the API. It must be reachable from **your browser**, so keep `localhost`. |

---

## 7. Troubleshooting

### `network local-net declared as external, but could not be found`

The network was not created. Run:

```bash
docker network create local-net
```

Then start again with `docker compose up -d`.

### `Bind for 0.0.0.0:8080 failed: port is already in use` (or 5450, 6381, 8081)

Another program is using the port. Find it:

```bash
# macOS / Linux
lsof -i :8080
```

On Windows, run `netstat -ano | findstr :8080` in PowerShell or Command Prompt. Stop that program, or change the host port on the left side of the mapping in `docker-compose.yml`, for example `"8090:8080"`. If you change the API port, also update the `VITE_API_BASE_URL` build argument for `ecommerce-ui` and rebuild it.

### `ecommerce-api` keeps restarting or exits immediately

Read the logs:

```bash
docker compose logs --tail 100 ecommerce-api
```

Common causes:

- **Database not ready:** `depends_on` waits for PostgreSQL to be `healthy`. If PostgreSQL is unhealthy, check `docker compose logs ecommerce-api-postgres`.
- **Authentication failed for user "ecommerce-api":** the PostgreSQL data folder was created with different credentials. Reset the data (see [Resetting the data](#5-resetting-the-data)).
- **Liquibase error:** a migration failed, often because the database was modified manually. Reset the data to start clean.

### `ecommerce-api-postgres` is unhealthy or keeps restarting

- Check the logs: `docker compose logs ecommerce-api-postgres`.
- If you see `database files are incompatible with server` or a version mismatch, the `volume/postgresql/data` folder comes from a different PostgreSQL major version. Reset the data.
- If you see permission errors on `volume/`, make sure Docker has access to the folder in Docker Desktop: *Settings → Resources → File sharing*.

### The UI loads, but API calls fail in the browser

1. **CORS error in the browser console** (`has been blocked by CORS policy`): the UI origin is not allowed. The default `http://localhost:8081` is correct if you use the port in this README. If you changed the UI port, update `CORS_ALLOWED_ORIGINS` and restart the API:

   ```bash
   CORS_ALLOWED_ORIGINS=http://localhost:9000 docker compose up -d ecommerce-api
   ```

2. **Network error or requests to the wrong host:** the bundle was built with a different API URL. Rebuild the UI:

   ```bash
   docker compose up -d --build ecommerce-ui
   ```

3. **API not running:** check `curl -i http://localhost:8080/api/v1/products/filter`.

### I changed the React code but I still see the old version

The image was not rebuilt. Run:

```bash
docker compose up -d --build ecommerce-ui
```

Then hard-refresh the browser (`Cmd+Shift+R` on macOS, `Ctrl+Shift+R` on Windows/Linux).

### Build fails while downloading dependencies

- Check your internet connection and any corporate proxy settings in Docker Desktop (*Settings → Resources → Proxies*).
- Retry the build; transient network errors are common:

  ```bash
  docker compose build --no-cache ecommerce-api
  ```

### Build fails in the UI with TypeScript or pnpm errors

Run the same steps locally in `ecommerce-ui` to see the full error:

```bash
pnpm install --frozen-lockfile
pnpm build
```

If `pnpm-lock.yaml` is out of date, the Docker build stops because `--frozen-lockfile` is used. Update the lockfile with `pnpm install` in `ecommerce-ui` and commit it.

### `docker compose` command not found

You have the legacy `docker-compose` binary or no Compose plugin. Install Docker Desktop, or on Linux install the `docker-compose-plugin` package. Then use `docker compose` (with a space).

### Containers are healthy but I cannot connect to the database from my IDE

Use `localhost` and port `5450`, not `5432`. Port `5432` is only used inside the Docker network.

### Reset everything when nothing else works

```bash
docker compose down --remove-orphans
docker compose build --no-cache
docker compose up -d
```

To also delete the data, follow [Resetting the data](#5-resetting-the-data).
