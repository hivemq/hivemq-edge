---
title: "Configuration"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Reference for all environment variables, feature flags, and third-party service configuration"
audience: "Developers setting up or maintaining the application, AI agents"
maintained_at: "docs/technical/CONFIGURATION.md"
---

# Configuration

---

## Table of Contents

- [Environment File Hierarchy](#environment-file-hierarchy)
- [Variable Reference](#variable-reference)
  - [Application Variables (VITE\_)](#application-variables-vite_)
  - [Feature Flags (VITE_FLAG\_)](#feature-flags-vite_flag_)
  - [Monitoring (VITE_MONITORING\_)](#monitoring-vite_monitoring_)
- [The `config` Singleton](#the-config-singleton)
  - [Build and CI Variables](#build-and-ci-variables)
  - [Percy Variables](#percy-variables)
- [Feature Flags In Depth](#feature-flags-in-depth)
- [Third-Party Services](#third-party-services)
  - [Heap Analytics](#heap-analytics)
  - [Sentry](#sentry)
  - [Percy](#percy)
  - [SonarCloud](#sonarcloud)
  - [Snyk](#snyk)
- [Known Issues](#known-issues)

---

## Environment File Hierarchy

Vite loads environment files in this order (later files take precedence):

| File | Committed | Purpose |
|------|-----------|---------|
| `.env` | Yes | Shared defaults for all environments — public values only |
| `.env.local` | No (gitignored) | Developer overrides — secrets and local flags |
| `.env.test` | Yes | Overrides for Vitest/Cypress test runs |
| `.env.production` | — | Not used (production values injected by Gradle) |

**The golden rule:** Only variables prefixed `VITE_` are embedded in the browser bundle. All others (`PERCY_*`, `CYPRESS_*`, etc.) are Node/CI-only and never reach the client.

**See:** [Build and Deployment](./BUILD_AND_DEPLOYMENT.md#environment-variables) for how `VITE_HIVEMQ_EDGE_VERSION` is injected by Gradle and why it differs between local and production builds.

---

## Variable Reference

### Application Variables (`VITE_`)

These are read in `src/config/index.ts` and exposed via the `config` singleton throughout the application.

| Variable | Default (`.env`) | Local (`.env.local`) | Purpose |
|----------|-----------------|----------------------|---------|
| `VITE_HIVEMQ_EDGE_VERSION` | `0.0.0` | — | Broker version shown in the UI. Overwritten by Gradle to the actual release version in production. |
| `VITE_APP_VERSION` | `$npm_package_version` | — | Frontend app version, taken from `package.json`. Shown in the dev console and `<body data-app-version>`. |
| `VITE_APP_DOCUMENTATION` | `https://docs.hivemq.com/` | — | Base URL for documentation links in the UI. |
| `VITE_APP_ISSUES` | `https://hivemq.kanbanize.com/ctrl_board/57/` | — | URL for the issue tracker linked from the UI. **Outdated** — see [Known Issues](#known-issues). |
| `VITE_API_BASE_URL` | — | `http://localhost:8080` | API base URL. Only used in development mode (`MODE === 'development'`). In production, the URL is derived from `window.location.href`. |

**`VITE_API_BASE_URL` resolution logic** (`src/config/index.ts:getApiBaseUrl()`):

```typescript
// Development: use .env.local value directly
if (import.meta.env.MODE === 'development') {
  return import.meta.env.VITE_API_BASE_URL
}
// Production: strip /app suffix from window.location.href
// e.g. http://localhost:8080/app → http://localhost:8080
```

---

### Feature Flags (`VITE_FLAG_`)

Feature flags are boolean (`'true'` string = enabled, anything else = disabled). They are collected in `src/config/index.ts` under `config.features`.

| Flag Variable | Config Key | Default | Description |
|---------------|------------|---------|-------------|
| `VITE_FLAG_MOCK_SERVER` | `DEV_MOCK_SERVER` | `false` | Activates the MSW mock server in the browser (dev only). Enables mock data for adapters, bridges, and MQTT sampling without a running backend. |
| `VITE_FLAG_DATAHUB_FSM_REACTFLOW` | `DATAHUB_FSM_REACT_FLOW` | `false` | Renders DataHub behavior policy FSM diagrams using React Flow canvas instead of Mermaid. |
| `VITE_FLAG_WORKSPACE_SHOW_EXPERIMENTAL` | `WORKSPACE_EXPERIMENTAL` | `false` | Enables experimental Workspace features not yet ready for general use. |

To enable a flag locally, uncomment and set it to `true` in `.env.local`:

```bash
VITE_FLAG_MOCK_SERVER=true
VITE_FLAG_DATAHUB_FSM_REACTFLOW=true
VITE_FLAG_WORKSPACE_SHOW_EXPERIMENTAL=true
```

> [!CAUTION]
> Two flag variables appear in `.env.local` but have **no corresponding entry in `config/index.ts`** and no usage in `src/`: `VITE_FLAG_WORKSPACE_AUTO_LAYOUT` and `VITE_FLAG_WORKSPACE_SHOW_MAPPER_NODES`. These are orphaned — either the features were removed without cleaning up `.env.local`, or they were never wired up. They have no effect. Recommend removing them from `.env.local` to avoid confusion.

---

### Monitoring (`VITE_MONITORING_`)

| Variable | Prod Value (`.env`) | Dev Value (`.env.local`) | Purpose |
|----------|--------------------|-----------------------|---------|
| `VITE_MONITORING_HEAP` | `3411251519` | `1974822562` | Heap Analytics project ID. Different IDs for prod and dev ensure dev usage doesn't pollute production metrics. |
| `VITE_MONITORING_SENTRY` | `https://6878b30b...ingest.de.sentry.io/...` | `https://d78a3d0d...ingest.sentry.io/...` | Sentry DSN. Different DSNs route to different Sentry projects. |

Both monitoring tools are **opt-in only**: they are loaded only after the user accepts the Privacy Consent Banner, and only if the backend's `trackingAllowed` flag is `true`. See [Heap Analytics](#heap-analytics) and [Sentry](#sentry) for details.

> [!NOTE]
> The production Sentry DSN and Heap project ID are committed in `.env` — this is intentional. These are client-side keys with no elevated privileges; exposure only affects data attribution, not security. The Sentry DSN scope is limited to error ingestion for this specific project.

---

### Build and CI Variables

These are **not** `VITE_` prefixed and are never embedded in the browser bundle. They are read by build tooling or CI systems only.

| Variable | Set By | Purpose |
|----------|--------|---------|
| `VITE_COVERAGE` | CI pipeline | Set to `'true'` to enable Istanbul instrumentation in the Vite build (forces `forceBuildInstrument: true` in `vite-plugin-istanbul`). Not a standard `VITE_` variable — it is read by `vite.config.ts` as a Node environment variable via `process.env`. |
| `SONAR_TOKEN` | CI secrets | SonarCloud authentication token for `check-frontend.yml`. |
| `SNYK_TOKEN` | CI secrets | Snyk authentication for all three Snyk workflows. |

---

### Percy Variables

Percy variables control visual regression testing. They are needed locally only when running Percy snapshots outside of CI.

| Variable | Example | Purpose |
|----------|---------|---------|
| `PERCY_TOKEN` | `web_d4fc...` | Project authentication token. Found in the Percy project settings. |
| `PERCY_BRANCH` | `local` | Branch label for local Percy runs (prevents polluting the CI baseline). |
| `PERCY_PARALLEL_TOTAL` | `-1` | Total parallel builds (`-1` = disable parallel mode). |
| `PERCY_PARALLEL_NONCE` | `1234` | Unique identifier for grouping parallel runs. |

The `.env.example` file documents these with placeholder values and is the canonical template for setting up `.env.local`.

---

## Feature Flags In Depth

Feature flags are accessed through the `config` singleton:

```typescript
import config from '@/config'

if (config.features.DEV_MOCK_SERVER) {
  // activate mock server
}
```

### `DEV_MOCK_SERVER`

Starts the MSW (Mock Service Worker) browser worker on app load. Intercepts all `/api/*` requests and returns mock data defined in `src/__test-utils__/`. Use this when no backend is available locally.

**When to enable:** Local development without the Java backend running. Never enable in a build that connects to a real backend — it will intercept real API calls.

### `DATAHUB_FSM_REACT_FLOW`

Switches the Behavior Policy FSM visualisation from Mermaid (static, read-only diagram) to React Flow (interactive, editable graph). The React Flow implementation is the target state; Mermaid is the fallback while the canvas-based FSM editor is under development.

### `WORKSPACE_EXPERIMENTAL`

Reveals Workspace features gated behind this flag during active development. Exactly which features are behind this gate changes sprint by sprint — check `src/modules/Workspace/` for current `config.features.WORKSPACE_EXPERIMENTAL` usages.

---

## Third-Party Services

### Heap Analytics

**Purpose:** Session recording and user behaviour analytics for UX research.

**Activation:** Only loads when:

1. Backend returns `configuration.trackingAllowed: true`
2. User has accepted the Privacy Consent Banner (stored in `localStorage['edge.privacy']`)

**Code:** `src/modules/Trackers/PrivacyConsentBanner.tsx`

**Configuration:**

- Prod project ID: `3411251519` (in `.env`)
- Dev project ID: `1974822562` (in `.env.local`) — use the dev ID locally to avoid polluting production data
- User properties sent: `hivemqId`, broker `version`

**Dashboard access:** Contact the team for access to the HiveMQ Heap organisation. The dev and prod projects are separate.

**See:** [External Services — Heap Analytics](./EXTERNAL_SERVICES.md#heap-analytics) for the dashboard URL, login method, and access status.

---

### Sentry

**Purpose:** Error monitoring, performance tracing, and session replay.

**Activation:** Initialises in all modes except `'development'` (never runs locally with `pnpm dev`). User consent is also checked via the Privacy Consent Banner.

**Code:** `src/config/sentry.config.ts`

**Configuration:**

- Org: `hivemq`, Project: `edge`
- `tracesSampleRate: 1.0` — 100% of transactions traced
- `replaysSessionSampleRate: 0.1` — 10% of sessions recorded
- `replaysOnErrorSampleRate: 1.0` — 100% of sessions with errors recorded
- Release tagged as `hivemq-edge@{VITE_HIVEMQ_EDGE_VERSION}`

**Source maps:** `@sentry/vite-plugin` in `vite.config.ts` uploads source maps to Sentry on every build (org: `hivemq`, project: `edge`). This requires `SENTRY_AUTH_TOKEN` to be available in the build environment — this is a CI secret, not a developer-facing variable.

**Dashboard:** [https://sentry.io](https://sentry.io) — HiveMQ organisation, `edge` project. Contact the team for access.

**See:** [External Services — Sentry](./EXTERNAL_SERVICES.md#sentry) for the dashboard URL, login method, and access status.

---

### Percy

**Purpose:** Visual regression testing — pixel-level screenshot comparison on every PR.

**Activation:** Runs in CI as part of `check-frontend.yml`. Can be run locally with the `PERCY_TOKEN` from `.env.local`.

**Dashboard:** [https://percy.io](https://percy.io) — find the project linked from the GitHub PR checks. The project token (`PERCY_TOKEN`) in `.env.local` gives access.

**Local run:**

```bash
# Runs E2E + component tests under Percy for screenshot capture
pnpm cypress:percy
```

The `PERCY_BRANCH=local` setting prevents local snapshots from being compared against the CI baseline.

**See:** [Testing Architecture](../architecture/TESTING_ARCHITECTURE.md) for how Percy fits into the visual regression workflow.

**See:** [External Services — Percy](./EXTERNAL_SERVICES.md#percy) for the dashboard URL, login method, and access status (⚠️ manager access not yet shared).

---

### SonarCloud

**Purpose:** Static analysis, code smell detection, test coverage reporting.

**CI only** — no local configuration required.

**Dashboard:** [https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge](https://sonarcloud.io/summary/new_code?id=hivemq_hivemq-edge)

**PR analysis:** SonarCloud posts a quality gate status on every PR via `check-frontend.yml`. Coverage data is merged from Vitest + Cypress component + Cypress E2E before upload.

**See:** [Dependency Management](./DEPENDENCY_MANAGEMENT.md) and the `/sonarqube` skill for fetching per-PR metrics.

**See:** [External Services — SonarCloud](./EXTERNAL_SERVICES.md#sonarcloud) for the dashboard URL, login method, and access status.

---

### Snyk

**Purpose:** Dependency vulnerability scanning.

**CI only** — no local configuration required.

**Dashboard (frontend project):** [https://app.snyk.io/org/hivemq-edge/project/05151663-f435-4653-9e82-98a53a2640d3](https://app.snyk.io/org/hivemq-edge/project/05151663-f435-4653-9e82-98a53a2640d3)

**Vulnerability alert routing:** Currently Kanbanize; migrating to Linear — see [INT-63](https://linear.app/hivemq/issue/INT-63/snyk-notifier-migrate-to-linear).

**See:** [Dependency Management](./DEPENDENCY_MANAGEMENT.md#snyk) for the full Snyk workflow breakdown.

**See:** [External Services — Snyk](./EXTERNAL_SERVICES.md#snyk) for the dashboard URL, login method, and access status.

---

## The `config` Singleton

**Location:** `src/config/index.ts`
**Import:** `import config from '@/config'`
**Used in:** 19 files across API hooks, routing, feature gating, and UI modules.

The `config` object is a module-level singleton (not a React context) assembled once at app startup from `import.meta.env.*`. It is the single place where raw environment variables are normalised into typed, named values that the rest of the application reads.

```typescript
const config: configType = {
  environment: string         // Vite MODE: 'development' | 'production' | 'test'
  isDevMode: boolean          // MODE === 'development'
  isProdMode: boolean         // MODE === 'production'
  apiBaseUrl: string          // derived from window.location or VITE_API_BASE_URL
  version: string             // VITE_APP_VERSION (from package.json)
  documentationUrl: string    // VITE_APP_DOCUMENTATION

  httpClient: { ... }         // hardcoded React Query + Axios values (see below)
  features: { ... }           // feature flags (see above)
  documentation: { ... }      // hardcoded namespace URL
}
```

### `httpClient` — Hardcoded Runtime Behaviour

These values are not environment-driven. They are hardcoded in `config` because they represent application-level design decisions that apply everywhere, not per-environment tuning:

| Field | Value | Purpose |
|-------|-------|---------|
| `axiosTimeout` | `15000` ms | Axios request timeout. Requests taking longer than 15 s are aborted. |
| `networkMode` | `'always'` | React Query network mode. Queries fire regardless of browser online/offline state — appropriate since the backend is on localhost or LAN. |
| `pollingRefetchInterval` | `5000` ms | Interval for status-polling queries (adapter status, bridge status, Pulse). Used in `refetchInterval` options across ~6 hooks. |

**Why hardcoded rather than env-driven:** These are operational constants, not deployment-time concerns. Changing them requires a code review decision, not a config change.

### `documentation.namespace`

```typescript
documentation: {
  namespace: 'https://www.hivemq.com/solutions/manufacturing/...',
}
```

A hardcoded URL for the ISA-95/Sparkplug documentation namespace. Used in `DomainOntologyManager` and `UnifiedNamespace` info panels. There is a `// TODO[NVL] Is this the right place?` comment in the source — its location in `config` is questioned but it hasn't moved.

### How Feature Flags Are Gated

```typescript
// ✅ Always read from config, not directly from import.meta.env
import config from '@/config'

if (config.features.DEV_MOCK_SERVER) { ... }

// ❌ Never do this in application code
if (import.meta.env.VITE_FLAG_MOCK_SERVER === 'true') { ... }
```

The indirection through `config` means:

- Type safety — flags are booleans in `config`, not raw strings
- Single source of truth — rename a flag in one place, not across every consumer
- Testability — tests can mock `config` rather than Vite's env system

Feature flags gate routes in `src/modules/App/routes.tsx`, FSM rendering in `FiniteStateMachineFlow.tsx`, and drag-and-drop behaviour in `AccessibleDraggableLock.tsx`.

---

## Known Issues

### `VITE_APP_ISSUES` points to Kanbanize

```
VITE_APP_ISSUES=https://hivemq.kanbanize.com/ctrl_board/57/
```

This URL is rendered in the UI as a feedback/issue link. The team has migrated from Kanbanize to Linear. This value should be updated to the relevant Linear board URL once decided.

**Files to update:** `.env`, and any UI component that reads `import.meta.env.VITE_APP_ISSUES` (referenced in `src/__test-utils__/dev-console.ts`).

---

### `VITE_FLAG_WORKSPACE_AUTO_LAYOUT` and `VITE_FLAG_WORKSPACE_SHOW_MAPPER_NODES` are orphaned

These appear in `.env.local` but have no corresponding entry in `src/config/index.ts` and no usage in `src/`. See [Feature Flags](#feature-flags-vite_flag_) for details.

**Recommended action:** Remove from `.env.local` unless they are actively being wired up.
