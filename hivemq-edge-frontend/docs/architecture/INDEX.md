# Architecture Documentation

This directory contains high-level architecture documentation explaining how the application is structured and why design decisions were made.

## Documents

- **[OVERVIEW.md](./OVERVIEW.md)** - High-level architecture overview
  - Application structure
  - Core design principles
  - Technology choices rationale
  - System boundaries

- **[DATA_FLOW.md](./DATA_FLOW.md)** - Data flow through the application
  - Complete flow: user action → React Query → useHttpClient → HiveMqClient → backend
  - Authentication layer (Bearer token, 401 handling)
  - Read flow: query cache (5 min staleTime), query keys, polling (5 s interval)
  - Write flow: mutations, invalidate-then-refetch pattern
  - Client state hierarchy (React Query → URL → Zustand → useState)
  - MSW intercept layer in tests

- **[STATE_MANAGEMENT.md](./STATE_MANAGEMENT.md)** - State management patterns
  - State layer hierarchy (React Query → URL → Zustand → useState)
  - Six Zustand stores inventory (which persist to localStorage, which are ephemeral)
  - Workspace localStorage gap (node positions, no stable backend IDs)
  - Workspace three-sources-of-truth problem (known technical debt)

- **[DATAHUB_ARCHITECTURE.md](./DATAHUB_ARCHITECTURE.md)** - DataHub extension
  - DataHub module structure
  - Policy designer canvas (React Flow)
  - State management (Zustand stores)
  - Validation workflow
  - Publishing workflow
  - Component architecture

- **[WORKSPACE_ARCHITECTURE.md](./WORKSPACE_ARCHITECTURE.md)** - Workspace canvas
  - React Flow integration
  - Node types (active and passive)
  - Dual-status model (runtime + operational)
  - Filter system (EPIC 37322)
  - Layout system (EPIC 25337)
  - Status propagation

- **[PROTOCOL_ADAPTER_ARCHITECTURE.md](./PROTOCOL_ADAPTER_ARCHITECTURE.md)** - Protocol adapter configuration
  - Backend-driven schema architecture
  - 15+ adapter types with RJSF forms
  - Data flow and validation strategy
  - Known issues and gaps (38658 analysis)
  - Testing and remediation status

- **[TESTING_ARCHITECTURE.md](./TESTING_ARCHITECTURE.md)** - Testing strategy and quality assurance
  - 7-layer testing pyramid (Code Quality → Unit → Integration → Accessibility → Visual → Performance → Monitoring)
  - Vitest for unit tests (logic/utilities)
  - Cypress for component and E2E tests
  - Mandatory accessibility testing (cypress-axe, WCAG 2.1 AA)
  - Visual regression with Percy
  - Performance audits with Lighthouse
  - Production monitoring (Heap, Sentry)
  - Custom parallel test runner
  - Coverage aggregation strategy
  - Complete Cypress custom commands reference
  - CI/CD pipeline (9 parallel jobs)

## Purpose

Architecture documents explain:
- **What:** The overall structure and components
- **Why:** The reasoning behind design decisions
- **How:** High-level implementation approaches

These are NOT detailed code-level documentation - for that, see [guides/](../guides/).

## Audience

- Developers new to the codebase needing to understand the big picture
- Tech leads making architectural decisions
- AI agents needing context for complex features
- Developers working on cross-cutting concerns

## Relationship to Guides

**Architecture docs** explain the "what" and "why" at a high level.

**Guides** explain the "how" with practical examples and patterns.

Example:
- Architecture: "We use React Query for server state management"
- Guide: "How to create a new React Query hook for an API endpoint"

---

**See:** [Documentation Index](../INDEX.md) for complete table of contents
