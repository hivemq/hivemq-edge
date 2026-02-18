# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# ⛔ STOP - MANDATORY PREFLIGHT CHECKLIST ⛔

**YOU MUST COMPLETE THIS CHECKLIST AT THE START OF EVERY CONVERSATION. NO EXCEPTIONS.**

## 🚨 BLOCKING REQUIREMENT: Read These 2 Documents FIRST

**DO NOT proceed with ANY user request until you have used the Read tool to read BOTH documents below.**

### Required Actions (Complete in Order):

1. **USE THE READ TOOL** to read `.github/AI_MANDATORY_RULES.md`

   - This contains critical rules that prevent wasting 1-2 hours on common mistakes
   - Not optional. Not negotiable. READ IT.

2. **USE THE READ TOOL** to read `.tasks/DESIGN_GUIDELINES.md`
   - UI component patterns, button variants, design patterns
   - Required before touching any UI code

### ⚠️ Verification Checklist

Before responding to the user's first request, verify:

- [ ] I used the Read tool on `.github/AI_MANDATORY_RULES.md` (not skimmed - READ)
- [ ] I used the Read tool on `.tasks/DESIGN_GUIDELINES.md` (not skimmed - READ)
- [ ] I understand the key rules from each document
- [ ] I will NOT skip these rules to "appear productive"

### 🔥 Consequences of Skipping This

**If you skip this preflight:**

- You will waste 1-2 hours on problems that take 15 minutes with guidelines
- You will violate critical rules and break tests
- User will have to repeat instructions 3-4 times
- User will waste their time fixing your avoidable mistakes

**Cost of reading:** 3-5 minutes
**Cost of NOT reading:** 1-3 hours wasted

## Why This Section Exists

Past AI instances have:

- Seen "Mandatory Reading" and ignored it
- Treated it as "optional context"
- Started work immediately to "appear productive"
- Wasted hours on problems the guidelines would have prevented

**This time is different. READ THE DOCUMENTS FIRST.**

---

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

### Every Component Test MUST Include

1. **Accessibility test** - Always last in describe block:

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()  // NOT cy.checkA11y()
})
```

2. **Use custom commands**:

```typescript
cy.getByTestId('my-element')           // NOT cy.get('[data-testid="..."]')
cy.mountWithProviders(<Component />)   // NOT cy.mount()
cy.checkAccessibility()                // NOT cy.checkA11y()
```

3. **Debug with `.only()` and HTML snapshots** when tests fail:

```typescript
it.only('the failing test', () => {
  cy.document().then((doc) => {
    cy.writeFile('cypress/debug-test.html', doc.documentElement.outerHTML)
  })
})
```

### Never Use

- CSS classnames as selectors (`.chakra-*`, `.css-*`)
- Arbitrary waits (`cy.wait(500)`)
- `{force: true}` to click covered elements (find the correct element instead)
- String literals for enums (use proper TypeScript enums)

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

### Select Accessibility

All `<Select>` components MUST have `aria-label`:

```typescript
<Select aria-label={t('component.selector.ariaLabel')} />
```

### i18n

All user-facing text must use translation keys:

```typescript
// Correct
<Heading>{t('workspace.wizard.title')}</Heading>

// Wrong - hardcoded string
<Heading>Configure Workspace</Heading>
```

## OpenAPI Client Generation

```bash
# Regenerate API client from OpenAPI spec
pnpm run dev:openAPI
```

Generated files in `src/api/__generated__/` should never be manually edited.

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

## Workspace/React Flow Testing

Components using `useReactFlow()` need special wrapper:

```typescript
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'

cy.mountWithProviders(<Component />, {
  wrapper: ({ children }) => (
    <ReactFlowTesting config={{ initialState: { nodes, edges: [] } }}>
      {children}
    </ReactFlowTesting>
  )
})
```

### MSW Handlers

Test handlers are co-located with hooks in `__handlers__` directories.

## Task Documentation

Task-related documentation is in `.tasks/`:

- `ACTIVE_TASKS.md` - Index of current tasks
- `CYPRESS_TESTING_GUIDELINES.md` - Comprehensive Cypress guide
- `DATAHUB_ARCHITECTURE.md` - DataHub state management details
- `RJSF_WIDGET_DESIGN_AND_TESTING.md` - Form widget patterns
- `WORKSPACE_TESTING_GUIDELINES.md` - Workspace-specific mock data

### Task Directory Structure

Tasks follow the Linear workflow. When a user mentions a task (e.g., "EDG-40"), check `.tasks/{linear-id}-{task-name}/` for context.

**Pattern**: `.tasks/{project-id}-{issue-number}-{short-description}/`

**Examples**:

- Linear issue `EDG-40` → Directory: `.tasks/EDG-40-technical-documentation/`
- Linear issue `EDG-38` → Directory: `.tasks/EDG-38-readonly-schemas/`

**Branch Naming**: Git branches may use slashes (e.g., `feat/EDG-40/technical-documentation`), but task directories use hyphens throughout.
