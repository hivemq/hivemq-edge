# API Reference

This directory contains documentation for API integration, data fetching patterns, and API mocking.

## Documents

- **[CYPRESS_INTERCEPT_API.md](./CYPRESS_INTERCEPT_API.md)** - Type-safe Cypress intercepts
  - cy.interceptApi() command and API_ROUTES registry
  - Parametric routes with withParams()
  - ESLint enforcement (no-bare-cy-intercept rule)
  - IDE integration: WebStorm and VS Code suggestion support

- **[OPENAPI_INTEGRATION.md](./OPENAPI_INTEGRATION.md)** - OpenAPI client generation
  - openapi-typescript-codegen configuration and workflow
  - Generated client structure (HiveMqClient, services, models, schemas)
  - useHttpClient wrapper for runtime configuration
  - Development workflow and regeneration process
  - Troubleshooting and migration notes

- **[REACT_QUERY_PATTERNS.md](./REACT_QUERY_PATTERNS.md)** - React Query patterns
  - Query hooks (basic, conditional, dependent, polling, infinite)
  - Mutation patterns (optimistic updates, sequential, parallel)
  - Caching strategy (query keys, invalidation, staleTime)
  - Error handling (per-query and global)
  - Testing patterns with MSW

- **[MSW_MOCKING.md](./MSW_MOCKING.md)** - API mocking for tests
  - MSW setup and configuration
  - Colocated handler organization (__handlers__/)
  - Mock data patterns and factories
  - Testing patterns (error states, mutations, delays)
  - Cypress integration (component vs E2E)
  - Debugging MSW handlers

## Purpose

API documentation covers:
- **Integration:** How the frontend communicates with backend
- **Patterns:** Standard approaches for data fetching
- **Testing:** How to mock APIs effectively
- **Migration:** Handling API client changes

## Audience

- Developers adding new API endpoints
- Developers writing tests that need API mocking
- AI agents implementing API-dependent features
- Anyone troubleshooting API integration issues

## Related Documentation

**For testing patterns:**
- See [Testing Guide](../guides/TESTING_GUIDE.md)
- See [Cypress Guide](../guides/CYPRESS_GUIDE.md)
- See [Testing Architecture](../architecture/TESTING_ARCHITECTURE.md)

**For forms and schemas:**
- See [RJSF Guide](../guides/RJSF_GUIDE.md)
- See [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md)

---

**See:** [Documentation Index](../INDEX.md) for complete table of contents
