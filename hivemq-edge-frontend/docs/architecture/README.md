# Architecture Documentation

This directory contains high-level architecture documentation explaining how the application is structured and why design decisions were made.

## Documents

- **OVERVIEW.md** _(TODO)_ - High-level architecture overview
  - Application structure
  - Core design principles
  - Technology choices rationale
  - System boundaries

- **DATA_FLOW.md** _(TODO)_ - Data flow through the application
  - User interactions → API calls
  - Server state management
  - Client state updates
  - UI rendering pipeline

- **STATE_MANAGEMENT.md** _(TODO)_ - State management patterns
  - React Query for server state
  - Zustand for client state
  - Form state with React Hook Form
  - URL state with React Router

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

- **TESTING_ARCHITECTURE.md** _(TODO)_ - Testing strategy
  - Testing pyramid
  - Component vs E2E tests
  - Coverage strategy
  - Mocking patterns

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
