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
  - [Page Object Pattern](#page-object-pattern)
  - [Page Object Hierarchy](#page-object-hierarchy)
  - [Creating Page Objects](#creating-page-objects)
  - [Using Page Objects in Tests](#using-page-objects-in-tests)
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

E2E tests are organized by feature in `cypress/e2e/` and mirror the Page Object structure in `cypress/pages/`:

```
cypress/
├── e2e/                        # E2E test files (*.spec.cy.ts)
│   ├── Login/
│   │   ├── login.spec.cy.ts
│   │   └── home.spec.cy.ts
│   ├── adapters/
│   │   ├── adapters.spec.cy.ts
│   │   ├── opcua.spec.cy.ts
│   │   └── http.spec.cy.ts     # One file per protocol adapter
│   ├── bridges/
│   │   └── bridges.spec.cy.ts
│   ├── datahub/
│   │   ├── datahub.spec.cy.ts
│   │   └── policy-report.spec.cy.ts
│   ├── eventLog/
│   │   └── eventLog.spec.cy.ts
│   ├── mappings/
│   │   └── combiner.spec.cy.ts
│   ├── pulse/
│   │   └── activation.spec.cy.ts
│   └── workspace/
│       ├── workspace.spec.cy.ts
│       ├── workspace-layout-basic.spec.cy.ts
│       ├── workspace-status.spec.cy.ts
│       └── wizard/
│           ├── wizard-create-adapter.spec.cy.ts
│           ├── wizard-create-bridge.spec.cy.ts
│           ├── wizard-create-combiner.spec.cy.ts
│           └── wizard-create-group.spec.cy.ts
├── pages/                      # Page Object files (organized by feature)
│   ├── Page.ts                 # Abstract base class
│   ├── ShellPage.ts            # App shell (nav, toasts)
│   ├── index.ts                # Central exports
│   ├── Login/
│   │   └── LoginPage.ts
│   ├── Workspace/
│   │   ├── WorkspacePage.ts
│   │   └── WizardPage.ts
│   ├── Protocols/
│   │   └── AdapterPage.ts
│   ├── Bridges/
│   │   └── BridgePage.ts
│   ├── DataHub/
│   │   ├── DatahubPage.ts
│   │   └── DesignerPage.ts
│   ├── EventLog/
│   │   └── EventLogPage.ts
│   ├── Pulse/
│   │   ├── AssetsPage.ts
│   │   └── ActivationFormPage.ts
│   ├── RJSF/
│   │   └── RJSFormField.ts
│   ├── Monaco/
│   │   └── MonacoEditor.ts
│   └── Home/
│       └── HomePage.ts
└── utils/                      # Shared utilities
    ├── intercept.utils.ts       # cy_interceptCoreE2E and mock factories
    ├── intercept-pulse.utils.ts # Pulse-specific intercepts
    ├── constants.utils.ts       # Menu link indices, enums
    ├── interactions.utils.ts    # Shared interaction helpers
    └── common_fields.utils.ts   # Form field utilities
```

---

### Page Object Pattern

Page Objects encapsulate all selectors and actions for a feature page. Tests interact with the UI through Page Objects, never with raw selectors.

**Why Page Objects:**
- **Single point of change:** Update a selector once, all tests follow
- **Readable tests:** `workspacePage.layoutControls.applyButton.click()` reads like documentation
- **Consistent selectors:** Prevents selector drift across tests
- **Reusability:** Same Page Object used across multiple test files

---

### Page Object Hierarchy

The inheritance chain defines shared capabilities at each level:

```
Page (abstract base)
└── ShellPage (app shell: nav, toasts, routing)
    └── WorkspacePage (workspace-specific: canvas, toolbar, nodes)
    └── WizardPage (wizard-specific: progress, selection, config)

Page (abstract base)
└── LoginPage (login form only, no shell nav)
└── AdapterPage (protocol adapter management)
└── BridgePage (bridge management)
```

**`Page` (abstract base) — `cypress/pages/Page.ts`:**
- `visit(route?)` — Navigate and set viewport to 1400×1016
- `pageHeader` — Main page `<h1>`
- `pageHeaderSubTitle` — Subtitle below header
- `toast` — Toast notifications (`shouldContain`, `close`)

**`ShellPage` — `cypress/pages/ShellPage.ts`:**
Extends `Page`. Adds app shell navigation and notifications:
- `navLinks` — The `nav [role="list"]` navigation elements
- `toasts` — All visible toast notifications
- `location` — Current URL pathname
- `toast.success` / `toast.error` / `toast.close()` — Typed toast helpers

**Feature Page Objects** extend either `Page` or `ShellPage` depending on whether they need navigation access.

---

### Creating Page Objects

#### Minimal Page Object

```typescript
// cypress/pages/MyFeature/MyFeaturePage.ts
import { ShellPage } from '../ShellPage.ts'

export class MyFeaturePage extends ShellPage {
  // Navigation link in sidebar
  get navLink() {
    cy.get('nav [role="list"]').eq(0).within(() => {
      cy.get('li').eq(EDGE_MENU_LINKS.MY_FEATURE).as('link')
    })
    return cy.get('@link')
  }

  // Simple element getter
  get createButton() {
    return cy.getByTestId('my-feature-create')
  }

  // Aria-label getter
  get configureButton() {
    return cy.getByAriaLabel('Configure feature')
  }
}

export const myFeaturePage = new MyFeaturePage()
```

#### Nested Object for Complex UI Sections

Group selectors for related UI sections into nested objects:

```typescript
export class WorkspacePage extends ShellPage {
  // Nested object for the layout controls panel
  layoutControls = {
    get panel() {
      return cy.getByTestId('layout-controls-panel')
    },

    get algorithmSelector() {
      return cy.getByTestId('workspace-layout-selector')
    },

    get applyButton() {
      return cy.getByTestId('workspace-apply-layout')
    },

    // Nested deeper for modal within section
    savePresetModal: {
      get nameInput() {
        return cy.getByTestId('workspace-preset-input')
      },

      get saveButton() {
        return cy.getByTestId('workspace-preset-save')
      },
    },
  }
}
```

**When to use nested objects:**
- UI section is a distinct sub-panel (toolbar, modal, drawer)
- Section has 3+ related elements
- Section appears conditionally (triggered by user action)

#### Parameterized Getters for Dynamic Content

Use methods when the selector depends on runtime data (node IDs, indices):

```typescript
export class WorkspacePage extends ShellPage {
  // Dynamic node selectors by ID
  adapterNode(id: string) {
    return cy.get(`[role="group"][data-id="adapter@${id}"]`)
  }

  bridgeNode(id: string) {
    return cy.get(`[role="group"][data-id="bridge@${id}"]`)
  }

  // Index-based selector
  combinerNodeContent(id: string) {
    return {
      get title() {
        return cy.get(`[role="group"][data-id="${id}"] [data-testid="combiner-description"]`)
      },
      get topic() {
        return cy.get(`[role="group"][data-id="${id}"] [data-testid="topic-wrapper"]`)
      },
    }
  }
}
```

#### Action Methods

For complex multi-step interactions that would be verbose in tests:

```typescript
export class WorkspacePage extends ShellPage {
  act = {
    /**
     * Multi-select React Flow nodes using meta-click sequence.
     * The double-back sequence is required by React Flow for multi-selection.
     */
    selectReactFlowNodes(nodes: string[]) {
      const [first, ...rest] = nodes
      workspacePage.adapterNode(first).type('{meta}', { release: false, force: true })
      workspacePage.adapterNode(first).click()
      rest.forEach((node) => {
        workspacePage.adapterNode(node).click()
      })
      workspacePage.adapterNode(first).type('{meta}', { force: true })
    },
  }
}
```

#### JSDoc on Page Objects

Add JSDoc when the selector is non-obvious or references a specific component:

```typescript
duplicateCombinerModal = {
  /**
   * Badge showing count of mappings
   * @see CombinerMappingsList
   */
  get mappingsCountBadge() {
    return cy.getByTestId('mappings-count-badge')
  },

  /**
   * Container for the list of mappings.
   * @note Use `cy.get('[data-testid^="mapping-item-"]')` for items with dynamic UUIDs
   */
  get mappingsList() {
    return cy.getByTestId('mappings-list')
  },
}
```

#### Adding to the Central Export

All Page Objects must be exported from `cypress/pages/index.ts`:

```typescript
// cypress/pages/index.ts
export { myFeaturePage } from './MyFeature/MyFeaturePage.ts'
```

Tests import from the index:
```typescript
import { myFeaturePage } from 'cypress/pages'
```

---

### Using Page Objects in Tests

#### Standard Import Pattern

```typescript
import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'
```

#### Navigation Setup (beforeEach)

Every E2E test suite follows the same navigation pattern:

```typescript
beforeEach(() => {
  // 1. Set up core intercepts (auth, config, notifications, etc.)
  cy_interceptCoreE2E()

  // 2. Feature-specific intercepts with aliases
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [mockAdapter_OPCUA],
  }).as('getAdapters')

  // 3. Navigate via loginPage.visit() (sets viewport + localStorage)
  loginPage.visit('/app/workspace')

  // 4. Submit login
  loginPage.loginButton.click()

  // 5. Navigate to feature (if not landing directly)
  workspacePage.navLink.click()

  // 6. Wait for initial data load
  cy.wait('@getAdapters')
})
```

#### Interacting Through Page Objects

```typescript
it('should display layout controls', () => {
  // Expand panel section
  workspacePage.canvasToolbar.expandButton.click()

  // Assert visibility
  workspacePage.layoutControls.panel.should('be.visible')
  workspacePage.layoutControls.algorithmSelector.should('be.visible')

  // Select value
  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  workspacePage.layoutControls.algorithmSelector.should('have.value', 'DAGRE_TB')

  // Click action
  workspacePage.layoutControls.applyButton.click()

  // Verify outcome
  workspacePage.edgeNode.should('be.visible')
})
```

#### Dynamic Nodes

```typescript
it('should display adapter nodes', () => {
  // Select by runtime ID from mock data
  workspacePage.adapterNode(mockAdapter_OPCUA.id).should('be.visible')
  workspacePage.bridgeNode(mockBridge.id).should('be.visible')

  // Access nested content
  workspacePage.combinerNodeContent('combiner-1').title.should('contain', 'Combiner')
})
```

#### Modal Interaction

```typescript
it('should detect duplicate combiner', () => {
  workspacePage.duplicateCombinerModal.modal.should('be.visible')
  workspacePage.duplicateCombinerModal.title.should('contain', 'Duplicate')
  workspacePage.duplicateCombinerModal.buttons.useExisting.click()
})
```

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

```typescript
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Layout - Basic', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [mockAdapter_OPCUA],
    }).as('getAdapters')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    cy.wait('@getAdapters')
    cy.wait('@getBridges')
    workspacePage.toolbox.fit.click()
  })

  it('should display layout controls in workspace', () => {
    workspacePage.canvasToolbar.expandButton.click()
    workspacePage.layoutControls.panel.should('be.visible')
    workspacePage.layoutControls.algorithmSelector.should('be.visible')
    workspacePage.layoutControls.applyButton.should('be.visible')
  })

  it('should allow selecting different layout algorithms', () => {
    workspacePage.canvasToolbar.expandButton.click()

    workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
    workspacePage.layoutControls.algorithmSelector.should('have.value', 'DAGRE_TB')
  })

  // Accessibility test is always LAST in the describe block
  it('should be accessible', () => {
    cy.injectAxe()
    cy.checkAccessibility()
  })
})
```

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
