# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Reading

**Before doing any work, read these mandatory documents:**

- `.github/AI_MANDATORY_RULES.md` - Critical rules to prevent wasting hours on common mistakes
- `.tasks/TESTING_GUIDELINES.md` - Testing patterns, accessibility, and Cypress requirements
- `.tasks/DESIGN_GUIDELINES.md` - UI component patterns and button variants

## Build & Development Commands

```bash
# Development
pnpm dev                  # Start dev server (port 3000, proxies /api to :8080)
pnpm build                # TypeScript check + Vite build
pnpm build:tsc            # TypeScript check only

# Linting
pnpm lint:eslint          # ESLint check
pnpm lint:prettier        # Prettier check
pnpm lint:all             # Both ESLint and Prettier

# Testing - Cypress
pnpm cypress:open:component   # Open Cypress component tests (interactive)
pnpm cypress:open:e2e         # Open Cypress E2E tests (interactive)
pnpm cypress:run:component    # Run all component tests headlessly
pnpm cypress:run:e2e          # Run all E2E tests headlessly

# Run specific test file
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"

# Testing - Vitest (unit tests)
pnpm test                 # Run Vitest in watch mode
pnpm test:coverage        # Run with coverage report

# Code generation
pnpm dev:openAPI          # Generate API client from OpenAPI spec
```

## Architecture Overview

### Directory Structure

```
src/
├── api/                  # API layer
│   ├── __generated__/    # Auto-generated OpenAPI client (HiveMqClient)
│   ├── hooks/            # React Query hooks for API calls
│   └── schemas/          # JSON schemas for forms
├── components/           # Shared UI components
├── extensions/datahub/   # DataHub feature module (self-contained)
├── modules/              # Feature modules
│   ├── App/              # Router and main app shell
│   ├── Workspace/        # React Flow canvas for device topology
│   ├── ProtocolAdapters/ # Protocol adapter configuration
│   ├── Bridges/          # MQTT bridge configuration
│   └── ...               # Other feature modules
├── __test-utils__/       # Test utilities, MSW mocks
├── config/               # App configuration
└── hooks/                # Shared React hooks
```

### Key Technologies

- **React 18** with TypeScript
- **Vite** for bundling
- **Chakra UI v2** for components (use `variant="primary"` not `colorScheme`)
- **React Router v6** for routing
- **TanStack React Query** for server state
- **React Flow (@xyflow/react)** for canvas/node graphs
- **RJSF (@rjsf/chakra-ui)** for JSON Schema forms
- **Zustand** for client state
- **MSW** for API mocking in tests
- **Cypress** for component and E2E tests
- **i18next** for internationalization

### Path Aliases

```typescript
@/        → src/
@datahub/ → src/extensions/datahub/
@cypr/    → cypress/
```

## Testing Requirements

### Cypress Component Tests

Every component test MUST include:

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()  // NOT cy.checkA11y()
})
```

### Custom Cypress Commands

- `cy.getByTestId('id')` - Use instead of `cy.get('[data-testid="id"]')`
- `cy.mountWithProviders()` - Mount React components with providers
- `cy.checkAccessibility()` - Run axe accessibility checks

### Test Debugging

When a Cypress test fails:

1. Use `.only()` to isolate the failing test
2. Save HTML snapshot before the failing assertion:
   ```typescript
   cy.document().then((doc) => {
     cy.writeFile('cypress/debug-test.html', doc.documentElement.outerHTML)
   })
   ```
3. Never use arbitrary `cy.wait(ms)` - use conditional waits instead

### Selector Priority

1. `data-testid` (best)
2. ARIA roles/labels
3. Text content
4. Never CSS classes

## DataHub Extension

The DataHub module (`src/extensions/datahub/`) is self-contained:

- Own routing in `routes.tsx`
- Own translations in `locales/en/datahub.json`
- Own API hooks in `api/hooks/`
- Uses React Flow for policy designer canvas
- State managed via Zustand stores (`useDataHubDraftStore`, `usePolicyChecksStore`)

## Common Patterns

### Button Variants

```tsx
<Button variant="primary">Save</Button>      // Primary CTA
<Button variant="outline">Cancel</Button>    // Secondary
<Button variant="ghost">Close</Button>       // Tertiary
<Button variant="danger">Delete</Button>     // Destructive
```

### React Query Hooks

API hooks follow the pattern in `src/api/hooks/`:

```typescript
export const useGetAdapters = () => {
  return useQuery({
    queryKey: ['adapters'],
    queryFn: () => client.adapters.getAll(),
  })
}
```

### MSW Handlers

Test handlers are co-located with hooks in `__handlers__` directories.

## Task Documentation

Task-related documentation is in `.tasks/`:

- `ACTIVE_TASKS.md` - Index of current tasks
- `CYPRESS_TESTING_GUIDELINES.md` - Comprehensive Cypress guide
- `DATAHUB_ARCHITECTURE.md` - DataHub state management details
- `RJSF_WIDGET_DESIGN_AND_TESTING.md` - Form widget patterns

When a user mentions a task, check `.tasks/{task-id}/` for context.
