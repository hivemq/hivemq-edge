---
title: "Testing Guide"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "General testing patterns and requirements for HiveMQ Edge Frontend"
audience: "Developers writing tests"
maintained_at: "docs/guides/TESTING_GUIDE.md"
---

# Testing Guide

---

## Table of Contents

- [Testing Philosophy](#testing-philosophy)
- [Test Types](#test-types)
- [Component Testing](#component-testing)
- [E2E Testing](#e2e-testing)
  - [Directory Structure](#directory-structure)
  - [Page Objects](#page-objects)
  - [API Interception in E2E Tests](#api-interception-in-e2e-tests)
  - [E2E Test Structure](#e2e-test-structure)
- [Accessibility Testing](#accessibility-testing)
- [Test Execution](#test-execution)
- [Test Organization](#test-organization)

---

## Testing Philosophy

### Code is Not Done Until Tests Pass

**ABSOLUTE REQUIREMENT:** Never declare test work complete without running tests and verifying they pass.

**Never:**
- ❌ Say "tests are complete" without running them
- ❌ Write "all tests passing" without actual results
- ❌ Create completion documentation without test verification
- ❌ Claim "tests should work" or make assumptions

**Always:**
- ✅ Run the actual test command
- ✅ Read and verify the test output
- ✅ See the actual pass/fail counts
- ✅ Fix failures immediately
- ✅ Include real test results in completion documentation

### Test Commands

**Component Tests:**
```bash
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
```

**E2E Tests:**
```bash
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"
```

**Multiple Tests:**
```bash
# Use glob patterns
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/workspace-*.spec.cy.ts"
```

---

## Test Types

### Component Tests

**Purpose:** Test individual React components in isolation

**Location:** Co-located with components (`*.spec.cy.tsx`)

**Framework:** Cypress Component Testing

**Key Requirements:**
- Test rendering with various props
- Test user interactions
- Test state changes
- Test accessibility (mandatory)

### E2E Tests

**Purpose:** Test full user workflows

**Location:** `cypress/e2e/**/*.spec.cy.ts`

**Framework:** Cypress E2E Testing

**Key Requirements:**
- Test complete user journeys
- Test API integration
- Test navigation flows
- Test accessibility (mandatory)

---

## Component Testing

### Standard Structure

```typescript
import { cy_mountWithProviders } from 'cypress/utils/mount.utils'

describe('ComponentName', () => {
  it('should render correctly', () => {
    cy.mountWithProviders(<ComponentName />)
    cy.getByTestId('component-name').should('be.visible')
  })

  it('should handle user interaction', () => {
    cy.mountWithProviders(<ComponentName />)
    cy.getByTestId('button').click()
    cy.getByTestId('result').should('contain', 'Success')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ComponentName />)
    cy.checkAccessibility()  // NOT cy.checkA11y()
  })
})
```

**Key Points:**
- Use `cy.mountWithProviders()` instead of `cy.mount()`
- Accessibility test is always last in the describe block
- Use custom commands (`cy.getByTestId()`, not `cy.get('[data-testid="..."]')`)

### Special Cases

**React Flow Components:**
```typescript
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'

cy.mountWithProviders(<Component />, {
  wrapper: ({ children }) => (
    <ReactFlowTesting config={{ initialState: { nodes: [...], edges: [] } }}>
      {children}
    </ReactFlowTesting>
  )
})
```

**See:** [Workspace Testing Guide](./WORKSPACE_TESTING_GUIDE.md) for React Flow details

---

## E2E Testing

### Directory Structure

E2E tests organize by feature under `cypress/e2e/` and Page Objects under `cypress/pages/`. Run `ls cypress/e2e/` to see the current structure.

**See:** [Cypress Guide](./CYPRESS_GUIDE.md) for the full E2E conventions.

---

### Page Objects

Page Objects encapsulate all selectors and actions for a feature page, keeping raw selectors out of test files. Tests interact with the UI through Page Object getters and methods, so a selector change requires an update in one place rather than across every test. The pattern also keeps test code readable: `workspacePage.layoutControls.applyButton.click()` describes intent without exposing implementation details.

See [Cypress Guide — Page Objects](./CYPRESS_GUIDE.md#page-objects) for the full pattern, naming conventions, and examples.

---

### API Interception in E2E Tests

#### cy_interceptCoreE2E

Every E2E test starts with `cy_interceptCoreE2E()` which stubs all common background requests:

```typescript
// cypress/utils/intercept.utils.ts
export const cy_interceptCoreE2E = () => {
  // Silence non-critical background requests
  cy.intercept('https://api.github.com/**', { statusCode: 202, log: false })
  cy.intercept('/api/v1/frontend/notifications', { statusCode: 202, log: false })
  cy.intercept('/api/v1/management/protocol-adapters/status', { statusCode: 202, log: false })
  cy.intercept('/api/v1/management/bridges/status', { statusCode: 202, log: false })

  // Stub authentication
  cy.intercept('/api/v1/auth/authenticate', mockAuthApi(mockValidCredentials))

  // Stub frontend configuration
  cy.intercept('/api/v1/frontend/configuration', { ...mockGatewayConfiguration })

  // Default adapter stub (tests can override)
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [mockAdapter_OPCUA],
  }).as('getAdapters')

  // Block Pulse
  cy.intercept('/api/v1/management/pulse/asset-mappers', { statusCode: 202, log: false })
}
```

#### Feature-Specific Intercepts

Override default stubs or add feature-specific ones after `cy_interceptCoreE2E()`:

```typescript
beforeEach(() => {
  cy_interceptCoreE2E()

  // Override with feature-specific data
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [mockAdapter_OPCUA, { ...mockAdapter_OPCUA, id: 'opcua-2' }],
  }).as('getAdapters')

  // Add feature-specific endpoints
  cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
  cy.intercept('/api/v1/management/topic-filters', { items: [MOCK_TOPIC_FILTER] }).as('getTopicFilters')

  // Wildcard with dynamic response
  cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/**/tags', (req) => {
    const id = new URL(req.url).pathname.split('/')[6]
    req.reply(200, { items: MOCK_DEVICE_TAGS(id, MockAdapterType.OPC_UA) })
  })
})
```

#### cy_interceptWithMockDB

For CRUD tests that need stateful responses, use the mock database factory:

```typescript
import { cy_interceptWithMockDB } from 'cypress/utils/intercept.utils.ts'
import { factory } from '@mswjs/data'

const db = factory({
  bridge: { id: primaryKey(String), json: String },
  adapter: { id: primaryKey(String), json: String },
})

beforeEach(() => {
  cy_interceptCoreE2E()
  cy_interceptWithMockDB({ bridge: db.bridge, adapter: db.adapter })
})
```

This sets up intercepts that respond with live state from the in-memory database — POST creates entries, DELETE removes them.

---

### E2E Test Structure

#### Standard Test File

For a complete E2E test example using all these patterns together, see
[`cypress/e2e/workspace/workspace-layout-basic.spec.cy.ts`](../../cypress/e2e/workspace/workspace-layout-basic.spec.cy.ts).

**Key Points:**
- Use `cy_interceptCoreE2E()` as the first call in every `beforeEach`
- Set up feature-specific intercepts after the core ones
- Always call `loginPage.visit()` + `loginPage.loginButton.click()` to authenticate
- Wait for data-loading intercepts before proceeding (`cy.wait('@alias')`)
- Accessibility test is always the last `it()` in the describe block
- Never use arbitrary `cy.wait(1000)` — use `cy.wait('@alias')` instead

---

## Accessibility Testing

### Mandatory Requirement

**EVERY component and E2E test suite MUST include an accessibility test.**

### Component Test Pattern

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />)
  cy.checkAccessibility()  // NOT cy.checkA11y()
})
```

**Always last test in the describe block.**

### E2E Test Pattern

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.checkAccessibility(undefined, {
    rules: {
      region: { enabled: false },  // Disable specific rules if needed
    }
  })
})
```

### Common Accessibility Rules

**Disabled rules** (when necessary):
- `region` - For pages without ARIA landmarks
- `color-contrast` - For known issues (document reason)

**Never disable** without documenting why in the test.

---

## Test Execution

### Run Specific Tests

**Single file:**
```bash
pnpm cypress:run:component --spec "src/components/Button.spec.cy.tsx"
```

**Pattern matching:**
```bash
pnpm cypress:run:component --spec "src/components/**/*.spec.cy.tsx"
```

**By tag:**
```bash
pnpm cypress:run:e2e --env grep="@workspace"
```

### Never Run All Tests

**❌ Wrong:**
```bash
pnpm cypress:run:component  # Runs hundreds of tests
```

**✅ Correct:**
```bash
pnpm cypress:run:component --spec "path/to/specific/test.spec.cy.tsx"
```

### Test Output Format

**Include in completion documentation:**

```markdown
## Test Verification

Command: `pnpm cypress:run:component --spec "src/components/Button.spec.cy.tsx"`

Results:
```
Button
  ✓ should render correctly (234ms)
  ✓ should handle clicks (156ms)
  ✓ should be accessible (89ms)

3 passing (2s)
```

✅ All tests verified passing.
```

---

## Test Organization

### File Structure

**Component tests are co-located with their source files:**
```
src/components/Button.tsx
src/components/Button.spec.cy.tsx

src/modules/Workspace/components/Canvas.tsx
src/modules/Workspace/components/Canvas.spec.cy.tsx
```

**E2E tests are grouped by feature in `cypress/e2e/`:**
```
cypress/e2e/
├── Login/               # Authentication and home page flows
├── adapters/            # Protocol adapter management (one file per adapter type)
├── bridges/             # Bridge management
├── datahub/             # DataHub policies and designer
├── eventLog/            # Event log feature
├── mappings/            # Data combiner mappings
├── pulse/               # Pulse integration
└── workspace/           # Workspace canvas and wizard
    └── wizard/          # Sub-feature for workspace wizard flows
```

**Page Objects mirror the E2E structure in `cypress/pages/`:**
```
cypress/pages/
├── Page.ts              # Abstract base (visit, header, toast)
├── ShellPage.ts         # App shell base (nav, toasts, location)
├── index.ts             # Central export for all Page Objects
├── Login/
├── Workspace/
├── Protocols/
├── Bridges/
├── DataHub/
├── EventLog/
├── Pulse/
└── Home/
```

### Naming Conventions

**Component Tests:**
- `ComponentName.spec.cy.tsx`
- Clear, descriptive `it()` statements
- Group related tests in `describe()` blocks

**E2E Tests:**
- `feature-name.spec.cy.ts`
- Describe user workflows, not implementation
- Test complete journeys

### Test Descriptions

**✅ Good:**
```typescript
it('should save adapter configuration when submit button is clicked')
it('should show error message for invalid adapter URL')
it('should navigate to workspace after successful login')
```

**❌ Bad:**
```typescript
it('works')  // Too vague
it('test adapter') // Not descriptive
it('should call API') // Implementation detail
```

---

## Related Documentation

**Testing Guides:**
- [Cypress Guide](./CYPRESS_GUIDE.md) - Cypress-specific rules, selectors, custom commands, debugging
- [Workspace Testing Guide](./WORKSPACE_TESTING_GUIDE.md) - React Flow component testing, mock state
- [Design Guide](./DESIGN_GUIDE.md) - UI component patterns and button variants

**Architecture:**
- [Testing Architecture](../architecture/TESTING_ARCHITECTURE.md) - 7-layer pyramid, CI/CD strategy, metrics
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)
- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md)

**API Mocking:**
- [MSW API Mocking](../api/MSW_MOCKING.md) - Component test mocking with MSW handlers
- [React Query Patterns](../api/REACT_QUERY_PATTERNS.md) - Testing with React Query

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md)
