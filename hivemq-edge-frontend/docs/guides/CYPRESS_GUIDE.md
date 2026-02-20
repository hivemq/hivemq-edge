---
title: "Cypress Guide"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Cypress-specific patterns, rules, and best practices"
audience: "Developers writing Cypress tests"
maintained_at: "docs/guides/CYPRESS_GUIDE.md"
---

# Cypress Guide

---

## Table of Contents

- [Critical Rules](#critical-rules)
- [Selector Strategy](#selector-strategy)
- [Custom Commands](#custom-commands)
- [Page Objects](#page-objects)
- [Assertion Patterns](#assertion-patterns)
- [Network Intercepts](#network-intercepts)
- [Common Pitfalls](#common-pitfalls)
- [Debugging](#debugging)

---

## Critical Rules

### Rule 1: Never Use `cy.wait()` with Arbitrary Timeouts

**❌ Wrong:**
```typescript
cy.get('button').click()
cy.wait(1000)  // NEVER DO THIS
cy.get('.result').should('be.visible')
```

**✅ Correct:**
```typescript
cy.get('button').click()
cy.get('.result').should('be.visible')  // Cypress retries automatically
```

**Exception:** Wait for network requests only:
```typescript
cy.intercept('/api/data').as('getData')
cy.get('button').click()
cy.wait('@getData')  // This is OK
```

---

### Rule 2: Never Chain After Action Commands

**❌ Wrong:**
```typescript
cy.get('#button').click().should('be.visible')
cy.get('input').type('text').should('have.value', 'text')
```

**✅ Correct:**
```typescript
cy.get('#button').click()
cy.get('#button').should('be.visible')

cy.get('input').type('text')
cy.get('input').should('have.value', 'text')
```

**Why:** Action commands (`.click()`, `.type()`, `.select()`) don't reliably return subjects for chaining.

---

### Rule 3: Always Use --spec for Test Execution

**❌ Wrong:**
```bash
pnpm cypress:run:component  # Runs ALL tests
```

**✅ Correct:**
```bash
pnpm cypress:run:component --spec "src/components/Button.spec.cy.tsx"
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/workspace-*.spec.cy.ts"
```

**Why:** Running all tests is slow and unnecessary.

---

### Rule 4: Avoid `cy.contains()`

**❌ Wrong:**
```typescript
cy.contains('Save').should('be.visible')  // Matches multiple elements
cy.contains('Layout').click()  // Partial text matching
```

**✅ Correct:**
```typescript
cy.getByTestId('save-button').should('be.visible')
cy.get('button[aria-label="Save layout"]').click()
```

**Why:** `cy.contains()` can match multiple elements or partial text, causing flaky tests.

---

### Rule 5: Use Properly Typed React Flow Nodes

**❌ Wrong:**
```typescript
import type { Node } from '@xyflow/react'

const mockNode: Node = {
  id: 'node-1',
  type: NodeTypes.ADAPTER_NODE,
  data: {
    label: 'Wrong Field'  // Adapter doesn't have label!
  }
}
```

**✅ Correct:**
```typescript
import type { NodeAdapterType } from '@/modules/Workspace/types'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

const mockNode: NodeAdapterType = {
  id: 'adapter-1',
  type: NodeTypes.ADAPTER_NODE,
  position: { x: 0, y: 0 },
  data: mockAdapter  // Use real API data structure
}
```

**Why:** Generic `Node` type allows incorrect mock data that doesn't match reality.

---

## Selector Strategy

### Priority Order

1. **data-testid** (best)
2. **ARIA attributes** (good, semantic)
3. **Role + content** (semantic)
4. **Text content** (fragile)
5. **CSS classes** (NEVER)

### Examples

**✅ Good Selectors:**
```typescript
// 1. data-testid (best)
cy.getByTestId('save-button')
cy.getByTestId('adapter-list')

// 2. ARIA attributes
cy.get('button[aria-label="Save configuration"]')
cy.get('[role="dialog"]')

// 3. Role + content
cy.get('[role="menu"]').within(() => {
  cy.get('[role="menuitem"]').first()
})

// 4. Semantic HTML
cy.get('nav').within(() => {
  cy.get('a[href="/workspace"]')
})
```

**❌ Bad Selectors:**
```typescript
// NEVER use CSS classes
cy.get('.chakra-button')  // Changes with Chakra updates
cy.get('.css-abc123')  // Arbitrary class name

// NEVER use contains() alone
cy.contains('Save')  // Ambiguous

// NEVER use position-based
cy.get('button').eq(2)  // Brittle
```

---

## Custom Commands

### Mounting Commands

**cy.mountWithProviders():**
```typescript
cy.mountWithProviders(<Component prop="value" />)
```

**Why not `cy.mount()`?** Components need React Query, Router, Theme providers.

**cy.mountWithProviders() with wrapper:**
```typescript
cy.mountWithProviders(<Component />, {
  wrapper: ({ children }) => (
    <ReactFlowTesting config={{ initialState: { nodes: [], edges: [] } }}>
      {children}
    </ReactFlowTesting>
  )
})
```

**Location:** `cypress/support/component.ts`

### Selector Commands

**cy.getByTestId():**
```typescript
cy.getByTestId('button-name')
// NOT: cy.get('[data-testid="button-name"]')
```

**cy.getByAriaLabel():**
```typescript
cy.getByAriaLabel('Close dialog')
// NOT: cy.get('[aria-label="Close dialog"]')
```

**Location:** `cypress/support/commands.ts`

### Accessibility Commands

**cy.checkAccessibility():**
```typescript
cy.injectAxe()
cy.checkAccessibility()
// NOT: cy.checkA11y()
```

**With options:**
```typescript
cy.checkAccessibility(undefined, {
  rules: {
    region: { enabled: false }
  }
})
```

**Location:** `cypress/support/commands.ts`

---

## Page Objects

E2E tests use the Page Object pattern — never use raw selectors in test files. All selectors live in `cypress/pages/`.

### Why Page Objects

- Selectors change in one place, not in every test
- Tests read like documentation: `workspacePage.layoutControls.applyButton.click()`
- Nested objects group related UI elements (toolbar, modal, drawer)

### Import Pattern

```typescript
// Always import from the central index
import { loginPage, workspacePage } from 'cypress/pages'
```

### Using Page Objects

```typescript
describe('Workspace', () => {
  beforeEach(() => {
    // Navigate using loginPage (provides visit + auth)
    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()

    // Navigate to feature using page object's navLink
    workspacePage.navLink.click()
  })

  it('should apply layout', () => {
    workspacePage.canvasToolbar.expandButton.click()
    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.applyButton.click()
    workspacePage.edgeNode.should('be.visible')
  })
})
```

### Page Object Structure

| Class | Extends | Use For |
|-------|---------|---------|
| `Page` | — | `visit()`, `pageHeader`, `toast` |
| `ShellPage` | `Page` | App shell: navigation, toasts, routing |
| Feature Pages | `ShellPage` | Feature-specific selectors and actions |
| `LoginPage` | `Page` | Login form (no shell navigation needed) |

**See:** [Testing Guide — Page Object Pattern](./TESTING_GUIDE.md#page-object-pattern) for full documentation including how to create Page Objects, nested objects, and parameterized getters.

---

## Assertion Patterns

### Specific Over Generic

**✅ Good:**
```typescript
cy.get('input').should('have.value', 'Expected text')
cy.getByTestId('status').should('have.text', 'Active')
cy.getByTestId('count').should('contain', '5')
```

**❌ Bad:**
```typescript
cy.get('input').should('exist')  // Too generic
cy.get('div').should('be.visible')  // Not specific enough
```

### Multiple Assertions

**✅ Separate assertions:**
```typescript
cy.getByTestId('button')
  .should('be.visible')
  .and('not.be.disabled')
  .and('have.text', 'Save')
```

**❌ Don't assert in middle of chain:**
```typescript
cy.getByTestId('button')
  .should('be.visible')
  .click()  // ❌ Breaks chain
  .should('have.class', 'active')
```

---

## Network Intercepts

**See:** [Type-Safe Cypress Intercepts](../api/CYPRESS_INTERCEPT_API.md) for full documentation on
`cy.interceptApi()`, the `API_ROUTES` registry, ESLint enforcement, and IDE migration support.

### Preferred: cy.interceptApi()

Use `cy.interceptApi()` for all static `/api/` responses. TypeScript validates the response shape
against the OpenAPI model at the call site:

```typescript
import { API_ROUTES } from '@cypr/support/__generated__/apiRoutes'

// TypeScript enforces BridgeList shape
cy.interceptApi(API_ROUTES.bridges.getBridges, { items: [mockBridge] }).as('getBridges')

// Status-only shorthand (disables polling)
cy.interceptApi(API_ROUTES.protocolAdapters.getAdaptersStatus, { statusCode: 202, log: false })

cy.wait('@getBridges')
cy.getByTestId('bridge-list').should('be.visible')
```

### Core Intercepts Helper

`cy_interceptCoreE2E` configures all core API intercepts needed for E2E workspace tests. See [`cypress/utils/intercept.utils.ts`](../../cypress/utils/intercept.utils.ts) for the full implementation.

### Dynamic Intercepts (callback handlers)

Use bare `cy.intercept()` with a callback only for stateful or dynamic scenarios that
`cy.interceptApi()` cannot cover:

```typescript
// CRUD mock database — response depends on runtime state
cy.intercept('GET', '/api/v1/management/bridges', (req) => {
  req.reply(200, { items: factory.bridge.getAll() })
})

// Dynamic response based on request URL
cy.intercept('GET', '/api/v1/adapters/**', (req) => {
  const id = req.url.split('/').pop()
  req.reply({ id, name: `Adapter ${id}` })
})
```

### Disabling Polling

```typescript
// Return 202 to disable status polling in tests
cy.interceptApi(API_ROUTES.protocolAdapters.getAdaptersStatus, { statusCode: 202, log: false })
```

---

## Common Pitfalls

### 1. Missing Intercepts

**Problem:** Test fails with network errors

**Solution:** Intercept ALL API calls the component makes

```typescript
// Check browser console for unmocked requests
cy.intercept('/api/v1/**', (req) => {
  console.log('Unmocked request:', req.url)
  req.reply({ items: [] })
})
```

### 2. React Flow Context Errors

**Problem:** `Cannot read property 'useStore' of null`

**Solution:** Use ReactFlowTesting wrapper

```typescript
cy.mountWithProviders(<NodeComponent />, {
  wrapper: ({ children }) => (
    <ReactFlowTesting config={{ initialState: { nodes: [], edges: [] } }}>
      {children}
    </ReactFlowTesting>
  )
})
```

### 3. Fixture Path Errors

**Problem:** 404 on fixture file

**Solution:** Use correct relative path from `cypress/fixtures/`

```typescript
// ❌ Wrong
cy.fixture('/path/to/file.json')

// ✅ Correct
cy.fixture('path/to/file.json')  // No leading slash
```

---

## Debugging

### Debug Output

**Document snapshot:**
```typescript
cy.document().then((doc) => {
  cy.writeFile('cypress/debug-test.html', doc.documentElement.outerHTML)
})
```

**Console logging:**
```typescript
cy.getByTestId('element').then(($el) => {
  console.log('Element:', $el)
  console.log('Text:', $el.text())
})
```

### Run Specific Test

**Using `.only()`:**
```typescript
it.only('the failing test', () => {
  // Only this test runs
})
```

**Using --spec:**
```bash
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"
```

### Interactive Mode

```bash
pnpm cypress:open:component  # Open Cypress UI for component tests
pnpm cypress:open:e2e        # Open Cypress UI for E2E tests
```

**Benefits:**
- See test execution in real browser
- Use browser DevTools
- Pause and step through tests
- See command log

---

## Related Documentation

**Testing Guides:**
- [Testing Guide](./TESTING_GUIDE.md) - General testing patterns, Page Object full documentation, E2E structure
- [Workspace Testing Guide](./WORKSPACE_TESTING_GUIDE.md) - React Flow component and E2E testing

**Architecture:**
- [Testing Architecture](../architecture/TESTING_ARCHITECTURE.md) - 7-layer testing pyramid, CI/CD
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)
- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md)

**API Mocking:**
- [Type-Safe Cypress Intercepts](../api/CYPRESS_INTERCEPT_API.md) - cy.interceptApi(), API_ROUTES registry, ESLint + IDE integration
- [MSW API Mocking](../api/MSW_MOCKING.md) - Component test API mocking

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md)
