# Ecommerce UI

A single-page web application (SPA) for catalog and product management in an online store. It lets users search and filter products, view product details, create, edit, and delete products, import products from a CSV file, and add items to a shopping cart.

This frontend consumes a backend REST API. It has no database or business logic of its own: persistence and domain validation live in the backend service.

## Table of contents

- [Features](#features)
- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Environment variables](#environment-variables)
- [Available scripts](#available-scripts)
- [Project structure](#project-structure)
- [Architecture and data flow](#architecture-and-data-flow)
- [API integration](#api-integration)
- [Testing](#testing)
- [Code quality](#code-quality)
- [Docker](#docker)

## Features

- **Product catalog** with a paginated table and configurable page size.
- **Search and filters** by text, category, and minimum stock. Filters are applied on the backend through a search endpoint.
- **Product details** in a read-only dialog.
- **Create, edit, and delete** products with validated forms (react-hook-form + zod) and a confirmation step before deleting.
- **Optimistic concurrency control**: when editing, the product's `ETag` is sent as `If-Match`. If someone else modified the product, the app reloads the data and notifies the user.
- **Idempotent creates**: each create sends an `Idempotency-Key`, so retrying the same operation does not create duplicates.
- **Bulk CSV import** with a per-batch result report.
- **Shopping cart** persisted in the browser (`localStorage`), with stock control, quantity changes, and confirmation dialogs for clearing the cart and removing items.
- **Categories** loaded from the API for filters and forms.
- **Backend errors shown in the form**, per field when applicable, or as a general message.

## Tech stack

| Area | Technology |
|------|-----------|
| Runtime and bundler | React 19, Vite 8, TypeScript 7 (strict mode) |
| Compiler | React Compiler (`babel-plugin-react-compiler`) |
| UI | Material UI v9 (`@mui/material`, `@mui/icons-material`), Emotion |
| Utility styles | Tailwind CSS v4 (`@tailwindcss/vite`), `clsx`, `tailwind-merge` |
| Secondary icons | `lucide-react` |
| Server state | TanStack React Query v5 |
| UI state | Zustand v5 (cart and product filters) |
| Forms | react-hook-form v7 + zod v4 (`@hookform/resolvers`) |
| HTTP | Axios |
| Testing | Vitest, jsdom, Testing Library, MSW (Mock Service Worker) |
| Lint and format | oxlint, oxfmt |

## Prerequisites

- **Node.js 22** or later.
- **pnpm** (the project uses `pnpm-lock.yaml`). You can enable it with `corepack enable`.
- A running backend for the product API (defaults to `http://localhost:8080/api`), or the MSW mocks used by the tests if you want to work without a backend.

## Quick start

```bash
# 1. Install dependencies
pnpm install

# 2. Start the development server (http://localhost:5173)
pnpm dev
```

To verify that the project builds and passes its tests:

```bash
pnpm build
pnpm test:run
pnpm lint
```

## Environment variables

| Variable | Description | Default |
|----------|-------------|---------|
| `VITE_API_BASE_URL` | Base URL of the backend REST API. | `http://localhost:8080/api` |

Vite variables are embedded at build time, so they must be set **before** building. For example, to point development at another backend:

```bash
VITE_API_BASE_URL=https://api.my-store.com/api pnpm dev
```

## Available scripts

| Command | What it does |
|---------|--------------|
| `pnpm dev` | Starts the Vite development server with hot module replacement. |
| `pnpm build` | Type-checks with `tsc -b` and produces the production build in `dist/`. |
| `pnpm preview` | Serves the `dist/` build locally for review. |
| `pnpm test` | Runs Vitest in interactive (watch) mode. |
| `pnpm test:run` | Runs the full test suite once. |
| `pnpm test:coverage` | Runs the tests with a coverage report (80% threshold). |
| `pnpm lint` | Runs oxlint on the project. |
| `pnpm lint:fix` | Applies oxlint's automatic fixes. |
| `pnpm format` | Formats the project with oxfmt. |
| `pnpm format:check` | Checks formatting without modifying files. |

## Project structure

The code is organized by **feature** (feature-first). Each domain keeps its API, hooks, components, models, and tests in its own folder.

```text
src/
├── app/                  # Providers, layouts (header, footer, main layout), and theme
├── core/
│   └── http/             # Shared HTTP client (Axios) and API error types
├── shared/               # Domain-agnostic reusable pieces: Loader, formatting utilities
├── mocks/                # MSW handlers for tests
├── features/
│   ├── product/          # Catalog, forms, CSV import, filters, and product stores
│   │   ├── api/          # Functions calling /v1/products, and DTOs
│   │   ├── hooks/        # TanStack Query hooks (queries and mutations) and search
│   │   ├── components/   # ProductsPage, ProductTable, ProductFormDialog, etc.
│   │   ├── model/        # zod validation schemas
│   │   ├── stores/       # Zustand store with catalog filters
│   │   ├── types/        # Domain types
│   │   ├── util/         # Pure functions (filter mapping, formatting)
│   │   └── __tests__/    # Domain tests
│   ├── category/         # Category lookup (API, hooks, validation)
│   └── cart/             # Cart: persisted store, drawer, checkout, and tests
├── App.tsx               # Root component
├── main.tsx              # Entry point
└── vite-env.d.ts         # Vite type declarations
```

Other relevant files at the root:

- `vite.config.ts`: Vite configuration, React Compiler, Tailwind, path aliases, and Vitest.
- `testSetup.ts`: global test setup (Testing Library and MSW).
- `tsconfig.app.json` / `tsconfig.node.json`: TypeScript configuration for the app code and the Vite configuration.
- `.oxlintrc.json`: oxlint rules.
- `Dockerfile` and `nginx.conf`: production packaging.
- `.github/copilot-instructions.md`: project architecture and conventions guide.

### Path aliases

Vite and TypeScript resolve the aliases `@/`, `@/app`, `@/shared`, and `@/features` to `src/`. Use them instead of long relative paths, for example `import { apiClient } from '@/core/http/api-client'`.

## Architecture and data flow

The app separates state by its origin:

- **Server state** (products, categories, product details): managed by TanStack React Query. Hooks in `features/*/hooks` encapsulate queries and mutations. After a mutation, affected lists are invalidated so the table reloads automatically.
- **UI state** (catalog filters, cart): managed by Zustand stores. The cart is persisted in `localStorage`.
- **Forms**: react-hook-form manages field state and zod validates the rules before submission. Errors returned by the backend are shown on the matching field.

Typical product creation flow:

1. The user opens `ProductFormDialog` and fills in the form.
2. zod validates the data on the client.
3. The React Query mutation calls `createProduct`, which sends `POST /v1/products` with an `Idempotency-Key`.
4. On success, product lists are invalidated and the table reloads.
5. On error, `toFormErrors` maps the messages to fields or shows them as a general message.

## API integration

The Axios client is created in [api-client.ts](/Users/luis.galicia/Documents/workspace/gila/ecommerce-ui/src/core/http/api-client.ts) using `VITE_API_BASE_URL` and a 15-second timeout. The main operations are:

| Method | Path | Purpose |
|--------|------|---------|
| `GET` | `/v1/products/filter` | Paginated search with filters (no HTTP cache). |
| `GET` | `/v1/products/{id}` | Product details. Supports `If-None-Match`. |
| `POST` | `/v1/products` | Create. Requires `Idempotency-Key`. |
| `PUT` | `/v1/products/{idOrSku}` | Update. Sends `If-Match` with the product's ETag. |
| `DELETE` | `/v1/products/{idOrSku}` | Delete. |
| `POST` | `/v1/products/import-csv` | Bulk CSV import (10-minute timeout). |
| `GET` | `/v1/categories` | List categories. |
| `GET` | `/v1/categories/{id}` and `/v1/categories/name/{name}` | Get a single category. |

Backend errors follow the `ApiErrorResponse` shape (status code, message, and per-field `errorDetails`), parsed with `getApiError`.

## Testing

Tests use **Vitest** with **jsdom** and **Testing Library**. HTTP calls are intercepted with **MSW** (`src/mocks/handlers.ts`), so the tests do not need a real backend.

```bash
pnpm test:run          # single run
pnpm test:coverage     # with coverage; minimum threshold is 80%
```

Tests live next to each feature's code, in `__tests__/` folders.

## Code quality

- **oxlint** checks the code against the rules in `.oxlintrc.json`.
- **oxfmt** keeps the formatting consistent. Run `pnpm format` before submitting changes, or `pnpm format:check` in CI.
- **TypeScript strict mode**: implicit `any` is not allowed.

## Docker

The `Dockerfile` builds the application in two stages:

1. **Build**: a `node:22-alpine` image with pnpm installs dependencies with `--frozen-lockfile` and runs `pnpm build`.
2. **Runtime**: an `nginx:alpine` image serves the contents of `dist/`. `nginx.conf` rewrites unknown routes to `index.html` so the SPA works when reloading.

```bash
# Build the image, pointing at the desired backend
docker build --build-arg VITE_API_BASE_URL=https://api.my-store.com/api -t ecommerce-ui .

# Run it at http://localhost:8080
docker run --rm -p 8080:80 ecommerce-ui
```

> Because `VITE_API_BASE_URL` is embedded at build time, changing it requires rebuilding the image.
