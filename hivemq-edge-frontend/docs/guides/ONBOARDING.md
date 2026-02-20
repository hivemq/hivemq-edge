---
title: "Onboarding Guide"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Step-by-step guide for new developers joining the HiveMQ Edge frontend team"
audience: "New developers, contractors, anyone setting up the project for the first time"
maintained_at: "docs/guides/ONBOARDING.md"
---

# Onboarding Guide

Welcome to the HiveMQ Edge frontend team. This guide takes you from zero to a running development environment.

**Read this top to bottom.** Don't skip steps — several are easy to miss and cause confusing failures later.

---

## Table of Contents

- [The Repository](#the-repository)
- [Prerequisites](#prerequisites)
  - [Node.js 22](#nodejs-22)
  - [pnpm 10](#pnpm-10)
  - [Java Backend (Optional)](#java-backend-optional)
- [Getting the Code](#getting-the-code)
- [Install Dependencies](#install-dependencies)
- [Configure Your Local Environment](#configure-your-local-environment)
  - [Minimum Configuration](#minimum-configuration)
  - [Running Without the Java Backend (Mock Mode)](#running-without-the-java-backend-mock-mode)
  - [Monitoring Keys (Optional)](#monitoring-keys-optional)
- [Start the Dev Server](#start-the-dev-server)
- [Verify the Setup](#verify-the-setup)
- [Running Tests](#running-tests)
- [Getting Access to External Services](#getting-access-to-external-services)
- [Understanding the Codebase](#understanding-the-codebase)
- [Day One Checklist](#day-one-checklist)

---

## The Repository

The frontend lives inside the **HiveMQ Edge monorepo**:

**Repository:** [https://github.com/hivemq/hivemq-edge](https://github.com/hivemq/hivemq-edge)

The monorepo contains both the Java backend and the frontend. The frontend lives entirely inside the `hivemq-edge-frontend/` subdirectory. You clone the whole monorepo but work inside that subdirectory.

```
hivemq-edge/                        ← monorepo root
├── hivemq-edge-frontend/           ← everything frontend (this is your workspace)
│   ├── src/                        ← application source code
│   ├── cypress/                    ← E2E tests
│   ├── docs/                       ← this documentation
│   ├── package.json
│   ├── vite.config.ts
│   └── ...
├── hivemq-edge/                    ← Java backend
├── hivemq-edge-composite/          ← Gradle composite build
└── ...
```

**All commands in this guide are run from inside `hivemq-edge-frontend/`**, not the monorepo root.

### Branching Convention

Branch names follow the pattern `feat/{LINEAR-ID}/{description}`. Example: `feat/EDG-40/technical-documentation`. The Linear issue ID is always included. Create branches from `master`.

---

## Prerequisites

You need two things installed before anything else: **Node.js 22** and **pnpm 10**. Both version requirements are enforced — the wrong versions cause `pnpm install` to fail with a clear error.

### Node.js 22

The project uses **Node.js 22** (LTS). The exact version is pinned in `.nvmrc`.

The recommended approach is [NVM (Node Version Manager)](https://github.com/nvm-sh/nvm), which lets you switch Node versions per project:

**Install NVM (macOS/Linux):**

```bash
curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash
```

After installing NVM, restart your terminal, then:

```bash
# Navigate to the frontend directory first
cd hivemq-edge/hivemq-edge-frontend

# Install the exact Node version specified in .nvmrc
nvm install

# Confirm the right version is active
node --version   # Should print v22.x.x
```

> [!TIP]
> Add `nvm use` to your shell's `cd` hook (or use [direnv](https://direnv.net/)) to automatically switch Node versions when you enter the project directory.

**Alternative — macOS without NVM:**

```bash
brew install node@22
```

### pnpm 10

The project uses **pnpm 10** as its package manager. npm and yarn do not work — the lockfile format is incompatible.

**Install pnpm:**

```bash
# Recommended: via corepack (bundled with Node.js 16.9+)
corepack enable
corepack prepare pnpm@latest --activate

# Alternative: via npm
npm install -g pnpm@10
```

**Confirm the version:**

```bash
pnpm --version   # Should print 10.x.x
```

### Java Backend (Optional)

The frontend proxies its API calls to a Java backend running on `http://localhost:8080`. You need the backend running to use the full application. However, you can work without it using mock mode — see [Running Without the Java Backend](#running-without-the-java-backend-mock-mode).

If you do want the backend locally, follow the Java/Gradle setup instructions in the monorepo root `README.md`.

---

## Getting the Code

Clone the full monorepo (not just the frontend subdirectory):

```bash
git clone git@github.com:hivemq/hivemq-edge.git
cd hivemq-edge/hivemq-edge-frontend
```

If you're using NVM, switch to the correct Node version now:

```bash
nvm install   # Reads .nvmrc and installs v22.x.x if not already present
nvm use       # Activates it for this shell
```

---

## Install Dependencies

```bash
pnpm install
```

This installs all frontend dependencies from `pnpm-lock.yaml`. It should complete without errors. If it fails:

- Check your Node version (`node --version`) — must be 22
- Check your pnpm version (`pnpm --version`) — must be 10
- Check your network connection (some dependencies are fetched from GitHub)

> [!NOTE]
> The `xlsx` (SheetJS) package is fetched directly from a CDN URL (`https://cdn.sheetjs.com/...`) rather than the npm registry. This is the officially supported distribution method since the package was removed from npm. If `pnpm install` hangs, check your network access to that domain.

---

## Configure Your Local Environment

> [!CAUTION]
> **This step is mandatory.** Without a `.env.local` file, `pnpm dev` starts but the application does not function — API calls fail with no useful error message.

The application uses Vite's environment file system. Your personal, local configuration goes in `.env.local` at the root of `hivemq-edge-frontend/`. This file is gitignored and never committed.

An `.env.example` file documents the available variables with placeholder values. Use it as a starting point:

```bash
cp .env.example .env.local
```

### Minimum Configuration

The minimum viable `.env.local` for local development is:

```dotenv
# Required: the URL where your Java backend is running
VITE_API_BASE_URL=http://localhost:8080
```

Without this variable, the dev server has no URL to proxy API calls to.

### Running Without the Java Backend (Mock Mode)

If you don't have the Java backend running, enable mock mode. MSW (Mock Service Worker) intercepts all `/api/*` requests in the browser and returns pre-defined test data:

```dotenv
VITE_API_BASE_URL=http://localhost:8080

# Set to true to enable mock mode (no backend required)
VITE_FLAG_MOCK_SERVER=true
```

Mock mode is fully functional for most UI work — adapters, bridges, MQTT data, and most configuration screens are mocked. Some features (live broker data, real MQTT sampling) require a real backend.

> [!WARNING]
> Never enable `VITE_FLAG_MOCK_SERVER=true` on a build that connects to a real backend — MSW will intercept and override all real API calls.

### Monitoring Keys (Optional)

Heap Analytics and Sentry are already configured with development project IDs in `.env.local` (if you copied `.env.example`). They are **opt-in only** — they only activate after a user accepts the Privacy Consent Banner and the backend's `trackingAllowed` flag is `true`. You do not need to configure these for normal development.

**See:** [Configuration — Monitoring](../technical/CONFIGURATION.md#monitoring-vite_monitoring_) for details.

---

## Start the Dev Server

```bash
pnpm dev
```

The dev server starts on **[http://localhost:3000](http://localhost:3000)**. You should see the HiveMQ Edge UI in your browser.

```
  VITE v7.x.x  ready in 800ms

  ➜  Local:   http://localhost:3000/
  ➜  Network: use --host to expose
```

Changes to source files trigger instant in-browser updates via Hot Module Replacement (HMR) — you don't need to restart the server.

---

## Verify the Setup

When you open `http://localhost:3000`:

**With the Java backend running:**

- The dashboard should load with real data
- The sidebar shows adapters, bridges, and other resources

**With mock mode (`VITE_FLAG_MOCK_SERVER=true`):**

- The dashboard loads with pre-populated mock data
- Open the browser console — you should see MSW intercept messages:
  ```
  [MSW] Mocking enabled.
  [MSW] 12:34:56 GET /api/v1/management/bridges (mocked)
  ```

**If the page is blank or shows an error:**

1. Check the browser console for errors
2. Confirm `.env.local` exists and contains `VITE_API_BASE_URL`
3. If not using mock mode, confirm the Java backend is running on port 8080
4. Restart `pnpm dev` after any `.env.local` changes (env changes are not hot-reloaded)

---

## Running Tests

The project has two test runners: **Cypress** (component and E2E tests) and **Vitest** (unit tests).

### Cypress (Component Tests)

The most common test type. Component tests mount individual React components in isolation:

```bash
# Interactive mode (recommended for development)
pnpm cypress:open:component

# Headless mode (CI-style)
pnpm cypress:run:component

# Run a specific file
pnpm cypress:run:component --spec "src/modules/Bridges/components/BridgeEditor.spec.cy.tsx"
```

### Cypress (E2E Tests)

End-to-end tests run against the full running application:

```bash
pnpm cypress:open:e2e
pnpm cypress:run:e2e
```

> [!NOTE]
> E2E tests require the dev server (`pnpm dev`) to be running in a separate terminal, or you can use mock mode.

### Vitest (Unit Tests)

```bash
pnpm test          # Watch mode
pnpm test:coverage # Single run with coverage report
```

**See:** [Testing Guide](./TESTING_GUIDE.md) for full testing patterns, required accessibility tests, and selector conventions.

---

## Getting Access to External Services

The team uses several external dashboards for monitoring, code quality, and project management. Access requests go through the team lead.

| Service | What You Need Access To | How |
|---------|------------------------|-----|
| **Linear** | Issue tracker — your sprint tasks | Ask team lead to add you to the HiveMQ workspace |
| **GitHub** | Repository — PR reviews and CI | Request access to `hivemq` GitHub org |
| **Sentry** | Error monitoring dashboard | HiveMQ Google SSO — ask team lead |
| **SonarCloud** | Code quality gate results | GitHub SSO — ask team lead |
| **Snyk** | Dependency security alerts | HiveMQ SSO — ask team lead |

**See:** [External Services](../technical/EXTERNAL_SERVICES.md) for the complete list with dashboard URLs and login methods.

---

## Understanding the Codebase

Once the dev server is running, take time to understand the structure before writing code.

### Key Directories

```
src/
├── api/                  # API layer
│   ├── __generated__/    # Auto-generated from OpenAPI spec — never edit manually
│   ├── hooks/            # React Query hooks (useGetAdapters, useUpdateBridge, etc.)
│   └── schemas/          # JSON schemas for forms
├── components/           # Shared UI components (used across modules)
├── extensions/datahub/   # DataHub feature — self-contained extension
├── modules/              # Feature modules
│   ├── App/              # Router, main shell, top-level layout
│   ├── Workspace/        # React Flow canvas (device topology view)
│   ├── ProtocolAdapters/ # Adapter configuration forms
│   ├── Bridges/          # MQTT bridge configuration
│   └── ...               # One directory per feature area
├── __test-utils__/       # MSW mock handlers, test helpers, custom commands
├── config/               # App configuration singleton (src/config/index.ts)
└── hooks/                # Shared React hooks
```

### How Data Flows

The application is a REST API client. It fetches data from the Java backend via the generated `HiveMqClient` (`src/api/__generated__/`), wrapped in React Query hooks (`src/api/hooks/`). Components subscribe to those hooks and re-render when data changes.

**See:** [Architecture Overview](../architecture/OVERVIEW.md) for the full picture.

### The `config` Singleton

`src/config/index.ts` is a module-level singleton that normalizes all environment variables into typed values. Always use it rather than reading `import.meta.env` directly:

```typescript
// ✅ Correct
import config from '@/config'
if (config.features.DEV_MOCK_SERVER) { ... }

// ❌ Never do this in application code
if (import.meta.env.VITE_FLAG_MOCK_SERVER === 'true') { ... }
```

**See:** [Configuration — The config Singleton](../technical/CONFIGURATION.md#the-config-singleton).

### Path Aliases

TypeScript path aliases are configured so you don't need relative import chains:

```typescript
import { something } from '@/components/...'          // → src/components/...
import { something } from '@datahub/api/hooks/...'    // → src/extensions/datahub/api/hooks/...
import { something } from '@cypr/support/...'         // → cypress/support/...
```

### External Product Documentation

The application has official user-facing documentation published at:

- **HiveMQ Edge:** [https://docs.hivemq.com/hivemq-edge/index.html](https://docs.hivemq.com/hivemq-edge/index.html) — full product documentation for operators and users
- **Data Hub:** [https://docs.hivemq.com/hivemq/latest/data-hub/index.html](https://docs.hivemq.com/hivemq/latest/data-hub/index.html) — data transformation and policy documentation (shared with HiveMQ Platform)

As a frontend developer, you are responsible for keeping these docs up to date when features ship. See [User-Facing Documentation](./USER_FACING_DOCUMENTATION.md) for the full process including screenshot generation.

### Recommended Reading Order

After getting the dev server running, read these in order:

1. [Architecture Overview](../architecture/OVERVIEW.md) — big picture, design principles
2. [Technical Stack](../technical/TECHNICAL_STACK.md) — what every dependency does
3. [Configuration](../technical/CONFIGURATION.md) — env variables and feature flags
4. [Testing Guide](./TESTING_GUIDE.md) — how to write tests correctly (mandatory before writing tests)
5. [Design Guide](./DESIGN_GUIDE.md) — UI component conventions (mandatory before writing UI)
6. [User-Facing Documentation](./USER_FACING_DOCUMENTATION.md) — how to write PR descriptions, feature announcements, and generate screenshots

---

## Day One Checklist

- [ ] Node.js 22 installed (`node --version` prints `v22.x.x`)
- [ ] pnpm 10 installed (`pnpm --version` prints `10.x.x`)
- [ ] Repository cloned and you're working inside `hivemq-edge-frontend/`
- [ ] `pnpm install` completed without errors
- [ ] `.env.local` created with `VITE_API_BASE_URL=http://localhost:8080`
- [ ] `pnpm dev` starts and opens in the browser without errors
- [ ] You can see the dashboard (with real data or mock data)
- [ ] `pnpm cypress:open:component` opens Cypress and tests run green
- [ ] You have access to Linear (ask team lead if not)
- [ ] You have access to the GitHub repository
- [ ] You've read [Architecture Overview](../architecture/OVERVIEW.md)

**Welcome to the team.**

---

## Troubleshooting

### `pnpm install` fails with "Unsupported engine"

Your Node.js or pnpm version doesn't match the requirements. Run `node --version` and `pnpm --version` and compare against the [Prerequisites](#prerequisites) section.

### `pnpm dev` starts but the UI shows "failed to fetch"

The API proxy has nothing to proxy to. Either:

- Start the Java backend on port 8080, or
- Enable mock mode (`VITE_FLAG_MOCK_SERVER=true` in `.env.local`) and restart `pnpm dev`

### `.env.local` changes have no effect

Vite reads env files at startup. Restart `pnpm dev` after any changes to `.env.local`.

### Cypress tests fail immediately with "cannot connect to server"

For E2E tests, the dev server must be running. Open a second terminal and run `pnpm dev` there, then run the Cypress tests in the first terminal.

### `nvm install` fails or `nvm` command not found

Close and reopen your terminal after installing NVM (the installer modifies your shell profile). Then retry `nvm install` from inside `hivemq-edge-frontend/`.
