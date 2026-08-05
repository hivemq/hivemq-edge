---
title: "Technical Stack"
author: "Edge Frontend Team"
last_updated: "2026-08-05"
purpose: "Complete reference for the frontend toolchain, dependencies, scripts, and CI/CD pipeline"
audience: "Developers, AI agents"
maintained_at: "docs/technical/TECHNICAL_STACK.md"
---

# HiveMQ Edge Frontend - Technical Stack

**Version:** 0.0.31

---

## Overview

The HiveMQ Edge Frontend is a modern React-based single-page application built with TypeScript, Vite, and a comprehensive testing infrastructure. This document provides a complete technical reference for developers.

> **`package.json` is the source of truth for versions.** The numbers below are a snapshot for
> orientation and go stale on every patch bump; treat the **major** as the meaningful part. When you
> raise a major, update this document in the same change.

---

## Core Toolchain

### Build System

**Vite 8.1.5** - Modern build tool and development server (Rolldown/Oxc based since v8)

- **Development Server:** Port 3000 with Hot Module Replacement (HMR)
- **API Proxy:** `/api` routes proxied to `http://127.0.0.1:8080`
- **Features:**
  - Fast ES module-based dev server
  - Optimized production builds
  - Source map generation enabled
  - Istanbul instrumentation for code coverage
  - Sentry integration for error tracking via `@sentry/vite-plugin`

**Configuration:** `vite.config.ts`

### Language & Runtime

**TypeScript 5.9.3** - Strict type checking enabled

- **Target:** ESNext
- **Module System:** ESNext with bundler resolution
- **JSX:** `react-jsx` (automatic runtime)
- **Strict Mode:** Enabled with comprehensive linting rules
- **Path Aliases:**
  - `@/` → `src/`
  - `@datahub/` → `src/extensions/datahub/`
  - `@cypr/` → `cypress/`

**Configuration:** `tsconfig.json`, `tsconfig.app.json`, `tsconfig.node.json`

### Package Manager

**pnpm 11** - Required (enforced via `engines` field)
**Node.js 24** - Required runtime version

**Rationale:** pnpm provides:
- Fast, disk-efficient package management
- Strict dependency resolution
- Monorepo support

---

## Main Dependencies

### UI Framework & Components

#### Core React
- **react 19.2.8** - UI library
- **react-dom 19.2.8** - DOM renderer

#### Component Library
- **@chakra-ui/react 2.10.10** - Component library with custom theming
- **@chakra-ui/icons 2.2.4** - Icon components
- **@chakra-ui/theme-tools 2.2.10** - Theme customization utilities
- **@emotion/react 11.14.0** - CSS-in-JS (required by Chakra UI)
- **@emotion/styled 11.14.1** - Styled components
- **framer-motion 12.42.2** - Animation library (Chakra UI dependency)
- **chakra-react-select 4.10.1** - Enhanced select components with Chakra UI styling

**Custom Theme:** `src/modules/Theme/themeHiveMQ.ts`

**Design Patterns:**
- Button variants: `primary`, `outline`, `ghost`, `danger`
- Custom color schemes
- Responsive breakpoints

### State Management

#### Server State
- **@tanstack/react-query 5.101.4** - Async state management, caching, synchronization
- **@tanstack/react-query-devtools 5.101.4** - DevTools for debugging queries

#### Client State
- **zustand 5.0.14** - Lightweight state management
  - Used for: Workspace state, DataHub drafts, UI state
  - v5 requires stable selector results: wrap object/array selectors in `useShallow`

#### Form State
- **react-hook-form 7.82.0** - Performant form state management with validation

### Routing

- **react-router 8.3.0** - Declarative routing
  - The package was renamed from `react-router-dom` in v7; there is no `react-router-dom` v8
  - DOM builds must import `RouterProvider` from **`react-router/dom`** — that entry point is the one
    that supplies `flushSync`, which `viewTransition`/`flushSync` navigations rely on
  - Router creation is wrapped by Sentry in `src/modules/App/routes.tsx`, which is why
    `src/config/sentry.config.ts` must be imported before it in `main.tsx`
  - File-based routing structure in `src/modules/App/`
  - Nested routes for DataHub extension

### Data Visualization

#### Canvas & Node Graphs
- **@xyflow/react 12.11.2** (React Flow) - Node-based graph editor for workspace topology
- **@dagrejs/dagre 3.0.0** - Directed graph layout algorithms (ships its own type declarations; the
  `@types/dagre` stub is no longer needed)
- **elkjs 0.12.0** - Eclipse Layout Kernel for automatic layout
- **webcola 3.4.0** - Constraint-based layout

#### Charts & Diagrams
- **@nivo/bar 0.99.0** - Bar charts
- **@nivo/chord 0.99.0** - Chord diagrams
- **@nivo/line 0.99.0** - Line charts
- **@nivo/sankey 0.99.0** - Sankey diagrams
- **@nivo/sunburst 0.99.0** - Sunburst charts
- **@nivo/tree 0.99.0** - Tree diagrams
- **recharts 3.10.1** - Additional charting library
- **d3-array 3.2.4** - Data manipulation utilities
- **d3-hierarchy 3.1.2** - Hierarchical data structures
- **d3-scale-chromatic 3.1.0** - Color scales
- **d3-shape 3.2.0** - Shape generators
- **mermaid 11.16.0** - Diagram generation from text

### Forms & Schema Validation

#### JSON Schema Forms
- **@rjsf/core 5.24.13** - JSON Schema form generator (core)
- **@rjsf/chakra-ui 5.24.13** - Chakra UI widgets for RJSF
- **@rjsf/utils 5.24.13** - Utilities for RJSF
- **@rjsf/validator-ajv8 5.24.13** - JSON Schema validation with AJV8
- **@jsonhero/schema-infer 0.1.5** - Schema inference

**Usage:** Protocol adapter configuration, bridge setup, DataHub policy forms

### Rich Text Editing

#### Tiptap (Headless Editor)
- **@tiptap/react 3.29.0** - React integration
- **@tiptap/extension-document 3.29.0** - Document node
- **@tiptap/extension-paragraph 3.29.0** - Paragraph node
- **@tiptap/extension-text 3.29.0** - Text node
- **@tiptap/extension-mention 3.29.0** - Mention functionality
- **@tiptap/extension-placeholder 3.29.0** - Placeholder text
- **@tiptap/suggestion 3.29.0** - Suggestion dropdown
- **@tiptap/pm 3.29.0** - ProseMirror integration

All Tiptap packages must move together — a mixed 2.x/3.x graph pulls in two copies of ProseMirror.
The project owns its own Tippy-based suggestion renderer rather than using the one v3 dropped.

### Code Editing

- **monaco-editor 0.55.1** - VS Code editor component
- **@monaco-editor/react 4.7.0** - React wrapper for Monaco

**Usage:** Code snippets, JSON editing in DataHub policies

> **Pinned to 0.55.1 on purpose.** Monaco 0.56 removed `monaco.languages.json` and
> `monaco.languages.typescript`, which DataHub's editor configures. The breakage is silent — the
> editor still renders, but IntelliSense and JSON validation stop working — so do not bump this
> without reworking the language-service setup.

### Protocols & Communication

#### HTTP Client
- **axios 1.18.1** - Promise-based HTTP client
  - API communication with HiveMQ Edge backend
  - Interceptors for auth, error handling

#### MQTT
- **mqtt 5.15.2** - MQTT protocol implementation
- **mqtt-match 3.0.0** - MQTT topic matching utilities

#### Protocol Buffers
- **protobufjs 8.7.1** - Protocol buffers support

### Internationalization (i18n)

- **i18next 26.3.6** - i18n framework
- **react-i18next 17.0.11** - React bindings for i18next

**Translation Files:**
- `src/locales/en/translation.json` - Main translations
- `src/extensions/datahub/locales/en/datahub.json` - DataHub-specific translations

**Configuration:** `src/config/i18n.config.ts`

### Utilities

#### Date & Time
- **luxon 3.7.2** - DateTime handling (Moment.js successor)

#### IDs & Data
- **uuid 14.0.1** - UUID generation (ships its own types; the `@types/uuid` stub is no longer needed)
- **ts-pattern 5.9.0** - Pattern matching for TypeScript
- **immutable-json-patch 6.0.3** - JSON patch operations

#### React Hooks
- **@uidotdev/usehooks 2.4.1** - Custom React hooks collection
- **react-hotkeys-hook 5.3.3** - Keyboard shortcuts
- **react-dropzone 19.1.1** - File upload handling
  - v19 accepts the files that fit under `maxFiles` and rejects only the excess, where v14 rejected
    the whole batch. Single-file zones must therefore reject in one `onDrop` handler rather than
    relying on `onDropAccepted`/`onDropRejected` firing exclusively.

#### UI Utilities
- **react-icons 5.7.0** - Icon library (Lucide, React Icons, etc.)
- **@atlaskit/pragmatic-drag-and-drop 2.0.1** - Drag and drop primitives

#### Data Handling
- **xlsx 0.20.3** (SheetJS) - Spreadsheet reading/writing (installed from the SheetJS CDN tarball,
  not npm)
- **@tanstack/react-table 8.21.3** - Headless table utilities
- **react-accessible-treeview 2.11.2** - Accessible tree view component

### Error Tracking & Monitoring

- **@sentry/react 10.68.0** - Error tracking and performance monitoring
- **@sentry/vite-plugin 5.4.0** - Sentry integration for Vite builds

**Configuration:** Environment-based Sentry DSN, release tracking

Use the version-neutral `reactRouterBrowserTracingIntegration`; the `reactRouterV6`/`reactRouterV7`
aliases are deprecated. Route-named transactions also require `Sentry.init` to run **before**
`createBrowserRouter` is wrapped, since the wrapper is a no-op if the integration is not registered
yet.

---

## Development Dependencies

### Testing Framework

#### Cypress (Component & E2E Testing)
- **cypress 15.19.0** - Test runner
- **@cypress/code-coverage 4.0.3** - Code coverage reporting
- **cypress-axe 1.7.0** - Accessibility testing
- **axe-core 4.12.1** - Accessibility rules engine
- **cypress-real-events 1.15.0** - Real user event simulation
- **@4tw/cypress-drag-drop 2.3.1** - Drag and drop testing
- **cypress-each 1.14.1** - Parameterized testing
- **@cypress/grep 6.0.0** - Test filtering by tags
- **cypress-terminal-report 7.3.3** - Terminal logging
- **cypress-multi-reporters 2.0.5** - Multiple test reporters

**Configuration:** `cypress.config.ts`

**Environment/config conventions (Cypress 15):**
- `Cypress.env()` is deprecated and the project sets `allowCypressEnv: false`. Use `cy.env()` for
  secrets and `Cypress.expose()` for public values.
- `@cypress/code-coverage` v4 reads its options from the top-level `expose` object, **not** the `env`
  bag it used through v3. Options left under `env` are silently ignored.
- The coverage excludes are load-bearing in two places: `expose.codeCoverage.exclude` filters in the
  browser, and `.nycrc.json`'s `exclude` filters the report. `nyc`'s `all: true` re-adds every
  matching file, so an exclude listed in only one of the two has no net effect.

**Custom Commands:**
- `cy.mountWithProviders()` - Mount with React providers
- `cy.getByTestId()` - Select by data-testid
- `cy.checkAccessibility()` - Run axe accessibility checks
- `cy.injectAxe()` - Inject axe-core

#### Vitest (Unit Testing)
- **vitest 4.1.10** - Fast unit test framework
- **@vitest/ui 4.1.10** - Test UI
- **@vitest/coverage-v8 4.1.10** - V8 coverage provider
- **@vitest/coverage-istanbul 4.1.10** - Istanbul coverage provider
- **jsdom 24.1.3** - DOM implementation for Node.js

**Configuration:** `vitest.config.ts`

#### Testing Library
- **@testing-library/react 16.3.2** - React testing utilities
- **@testing-library/jest-dom 7.0.0** - Custom matchers

#### API Mocking
- **msw 2.15.0** - Mock Service Worker for API mocking
- **@mswjs/data 0.16.2** - Data modeling for MSW

**Handlers Location:** `src/api/hooks/__handlers__/`

#### Visual Regression Testing
- **@percy/cli 1.32.4** - Percy command-line interface
- **@percy/cypress 3.1.9** - Percy integration for Cypress

### Linting & Code Quality

#### ESLint (10.8.0) - Flat Config
- **@eslint/js 10.0.1** - ESLint JavaScript rules
- **typescript-eslint 8.65.0** - TypeScript rules and parser
- **eslint-plugin-react 7.37.5** - React-specific rules
- **eslint-plugin-react-hooks 7.1.1** - Hooks rules, including the React Compiler rules
- **eslint-plugin-react-refresh 0.5.3** - React Refresh rules
- **eslint-plugin-cypress 6.4.3** - Cypress-specific rules
- **eslint-plugin-sonarjs 4.0.0** - Code quality and bug detection
- **@tanstack/eslint-plugin-query 5.101.4** - React Query rules
- **eslint-config-prettier 10.1.8** - Disable conflicting Prettier rules

**Configuration:** `eslint.config.mjs` (flat config format)

**Key Rules:**
- Strict TypeScript checking
- React hooks validation
- Consistent type imports: `import type { ... }`
- JSX curly brace presence: `props="never"`, `children="never"`
- No unnecessary Cypress waits

**Warning policy:** errors block CI; warnings do not. `pnpm lint:eslint` does **not** pass
`--max-warnings`, so the build stays green on warnings. The React Compiler rules introduced with
`eslint-plugin-react-hooks` 7 are deliberately set to `warn` rather than `error`, which leaves a
standing warning baseline (~80 at the time of writing). Treat that number as a backlog to burn down,
not as a passing grade — and do not assume a warning-free tree.

#### Prettier (3.9.6) - Code Formatting
- **Configuration:** `.prettierrc.cjs`
- **Settings:**
  - Single quotes
  - No semicolons
  - 120 character line width
  - 2-space indentation
  - ES5 trailing commas

#### Stylelint (17.14.1) - CSS Linting
- **stylelint-config-standard 40.0.0** - Standard CSS rules
- **stylelint-config-standard-scss 17.0.0** - SCSS rules

**Usage:** Lint CSS files in `./src/**/*.css`

### Code Generation

#### OpenAPI Client Generation
- **openapi-typescript-codegen 0.31.0** - Generate TypeScript client from OpenAPI specs

**Command:** `pnpm dev:openAPI`

**Generated Output:** `src/api/__generated__/`

**Source:** `../hivemq-edge-openapi/dist/bundle.yaml`

#### Chakra UI Types
- **@chakra-ui/cli 2.5.8** - Generate Chakra UI theme types

**Command:** `pnpm dev:chakra:types`

### Coverage & Reporting

#### Coverage Tools
- **nyc 18.0.0** - Istanbul command-line interface
- **@istanbuljs/nyc-config-typescript 1.0.2** - TypeScript configuration for NYC
- **vite-plugin-istanbul 9.0.1** - Istanbul plugin for Vite

#### Test Reporters
- **mocha-junit-reporter 2.2.1** - JUnit XML reports for CI
- **mochawesome 7.1.4** - HTML/JSON test reports
- **mochawesome-merge 5.0.0** - Merge multiple mochawesome reports
- **mochawesome-report-generator 6.3.2** - Generate HTML from JSON

**Custom Scripts:** `tools/run-tests.cjs`, `tools/merge-cypress-coverage.cjs`, `tools/merge-all-coverage.cjs`

### Build & Utility Tools

- **@vitejs/plugin-react 6.0.4** - Vite plugin for React Fast Refresh (v6 requires Vite 8)
- **sass 1.101.6** - CSS preprocessor
- **copyfiles 2.4.1** - Cross-platform file copying
- **fs-extra 11.3.6** - Enhanced file system operations
- **commander 15.0.0** - CLI framework for custom scripts
- **globals 17.8.0** - Global variable definitions

---

## Scripts Reference

### Development

```bash
pnpm dev                    # Start Vite dev server (port 3000, proxy to :8080)
pnpm preview                # Preview production build
pnpm dev:openAPI            # Generate API client from OpenAPI spec
pnpm dev:chakra:types       # Generate Chakra UI theme types
```

### Building

```bash
pnpm build                  # TypeScript check + Vite build
pnpm build:tsc              # TypeScript check only (no Vite build)
pnpm bundle:size            # Analyze bundle size with vite-bundle-visualizer
```

**Build Output:** `dist/` directory

**Build Process:**
1. TypeScript compilation check (`tsc`)
2. Vite build (bundling, optimization, tree-shaking)
3. Source map generation
4. Sentry release artifacts upload

### Linting

```bash
pnpm lint:eslint            # Run ESLint (errors fail; warnings are reported but do not fail)
pnpm lint:eslint:fix        # Run ESLint with auto-fix
pnpm lint:prettier          # Check Prettier formatting
pnpm lint:prettier:write    # Fix Prettier formatting
pnpm lint:stylelint         # Lint CSS files
pnpm lint:all               # Run ESLint + Prettier checks (CI)
```

**CI Usage:** `pnpm lint:all` is used in CI pipelines to enforce code quality.

### Testing - Cypress

#### Interactive Mode (Development)

```bash
pnpm cypress:open           # Open Cypress launcher
pnpm cypress:open:component # Open component tests (Chrome)
pnpm cypress:open:e2e       # Open E2E tests (Chrome)
```

#### Headless Mode (CI)

```bash
pnpm cypress:run            # Run all tests (E2E + component)
pnpm cypress:run:component  # Run component tests only
pnpm cypress:run:e2e        # Run E2E tests only (quiet mode)
```

**Run Specific Test File:**

```bash
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"
```

#### Coverage

```bash
pnpm cypress:coverage                    # Run Cypress with coverage
pnpm cypress:coverage:parallel           # Parallel execution
pnpm cypress:coverage:verbose            # Verbose output
pnpm cypress:coverage:parallel:verbose   # Both flags

pnpm coverage:all                        # All tests with coverage
pnpm coverage:all:parallel               # Parallel execution
pnpm coverage:all:verbose                # Verbose output
pnpm coverage:all:parallel:verbose       # Both flags

pnpm coverage:merge:cypress              # Merge Cypress coverage reports
pnpm coverage:merge:all                  # Merge all coverage reports
```

**Custom Scripts:** `tools/run-tests.cjs` orchestrates test execution and coverage collection.

#### Visual Regression (Percy)

```bash
pnpm cypress:percy          # Run tests with Percy visual testing
```

**Process:**
1. Runs E2E tests with Percy enabled
2. Runs component tests with Percy enabled
3. Finalizes Percy build

### Testing - Vitest (Unit Tests)

```bash
pnpm test                   # Run Vitest in watch mode
pnpm test:coverage          # Run with coverage report
pnpm test:ui                # Run with Vitest UI
```

**Coverage Output:** `coverage-vitest/` directory

---

## CI/CD Pipeline & Deployment

The frontend uses GitHub Actions for CI (defined in `.github/workflows/check.yml` and `check-frontend.yml`) and Gradle + Jenkins for production packaging. Key tools in the pipeline: Vite, Istanbul, Cypress, Vitest, SonarCloud, Percy, and Lighthouse.

For the complete CI pipeline breakdown — including all 9 parallel Cypress jobs, artifact flow, coverage merge, environment variables, quality gates, and production deployment via Gradle and Jenkins — see [Build and Deployment](./BUILD_AND_DEPLOYMENT.md).

---

## Configuration Files

| File | Purpose |
|------|---------|
| `vite.config.ts` | Vite build configuration, dev server, proxy |
| `tsconfig.json` | Root TypeScript configuration |
| `tsconfig.app.json` | App-specific TypeScript settings |
| `tsconfig.node.json` | Node scripts TypeScript settings |
| `eslint.config.mjs` | ESLint flat config with all rules |
| `.prettierrc.cjs` | Prettier formatting rules |
| `cypress.config.ts` | Cypress test configuration |
| `vitest.config.ts` | Vitest unit test configuration |
| `package.json` | Dependencies, scripts, project metadata |

---

## Path Aliases & Module Resolution

```typescript
// Path aliases (defined in tsconfig.json and vite.config.ts)
@/        → src/
@datahub/ → src/extensions/datahub/
@cypr/    → cypress/

// Usage examples:
import { Button } from '@/components/Button'
import { usePolicyChecksStore } from '@datahub/hooks/usePolicyChecksStore'
import { workspacePage } from '@cypr/pages'
```

---

## Key Architectural Patterns

### API Client Generation
- OpenAPI specs → TypeScript client
- Location: `src/api/__generated__/`
- Never manually edit generated files
- React Query hooks wrap generated client: `src/api/hooks/`

### Testing Strategy
- **Component Tests:** Cypress component testing for UI components
- **E2E Tests:** Cypress for full user flows
- **Unit Tests:** Vitest for utility functions, hooks
- **Accessibility:** Every component test includes `cy.checkAccessibility()`
- **Coverage:** Combined coverage from all test types

### State Management Strategy
- **Server State:** React Query (caching, synchronization)
- **Client State:** Zustand stores (workspace, DataHub, UI)
- **Form State:** React Hook Form
- **URL State:** React Router params/search

### Code Quality Gates
- **TypeScript:** Strict mode, no `any` types
- **ESLint:** 0 errors in CI; warnings are reported but non-blocking (see the warning policy above)
- **Prettier:** Automatic formatting
- **Accessibility:** Automated axe checks in every component test
- **Test Coverage:** Tracked with Istanbul/NYC

---

## Version Information & Dependency Management

### Current Versions

**Frontend Version:** 0.0.31 (from package.json)
**Node Version:** 24 (required, `engines.node`)
**pnpm Version:** 11 (required, `engines.pnpm`; pinned via `packageManager`)

---

### Deprecations & Required Migrations

#### openapi-typescript-codegen (Current: 0.31.0)

**Status:** Deprecated - No longer maintained

**Current Implementation:**
- Package: `openapi-typescript-codegen@0.31.0`
- Command: `pnpm dev:openAPI`
- Configuration: Direct invocation in `package.json` scripts
- Input: `../hivemq-edge-openapi/dist/bundle.yaml`
- Output: `src/api/__generated__/`
- Client Type: Axios-based client

**Migration Target:** `@hey-api/openapi-ts`

**Action Required:**
- Analyze current generated code structure
- Evaluate `@hey-api/openapi-ts` compatibility
- Identify breaking changes in generated API
- Plan migration strategy for React Query hooks
- Create migration document

**Migration Document:** To be created in `.tasks/{issue-id}-openapi-codegen-migration/` when migration is approved

**Impact:**
- Generated client code structure may change
- React Query hooks in `src/api/hooks/` may need updates
- Type definitions may change
- Build scripts need updating

---

#### Chakra UI v2 → v3

**Status:** Available, but **deliberately not taken** (decided 2026-08-04). The React 19 upgrade was
completed on Chakra v2; v3 was never a prerequisite for it. The notes below are retained for a future
standalone attempt, not as pending work.

**Current Implementation:**
- Package: `@chakra-ui/react@2.10.10`
- Custom theme: `src/modules/Theme/themeHiveMQ.ts`
- Widespread usage across all UI components
- Custom variants: `primary`, `outline`, `ghost`, `danger`

**Migration Target:** Chakra UI v3

**Known Breaking Changes:**
- Component API changes
- Theme structure updates
- Style prop modifications
- Provider configuration changes
- ColorScheme → ColorPalette naming

**Action Required:**
- Comprehensive component audit
- Migration plan for custom theme
- Update all component usage
- Test all UI interactions
- Update testing patterns

**Migration Document:** To be created in `.tasks/{issue-id}-chakra-ui-v3-migration/` when migration is approved

**Resources:**
- MCP Tool: `mcp__chakra-ui__v2_to_v3_code_review` (available for migration guidance)
- Official migration guides
- Community migration examples

**Impact:**
- High - Affects every component in the application
- Requires thorough testing
- May require design review for visual changes
- Significant time investment

---

## Additional Resources

- **Architecture Documentation:** `.tasks/DATAHUB_ARCHITECTURE.md`
- **Testing Guidelines:** `.tasks/TESTING_GUIDELINES.md`, `.tasks/CYPRESS_TESTING_GUIDELINES.md`
- **Design Guidelines:** `.tasks/DESIGN_GUIDELINES.md`
- **RJSF Patterns:** `.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md`
- **Workspace Testing:** `.tasks/WORKSPACE_TESTING_GUIDELINES.md`

---

**Document Maintained By:** Development Team
**Last Review:** 2026-08-05
