# Cypress Testing Guidelines - Comprehensive Reference

**Last Updated:** November 24, 2025  
**Purpose:** Complete guide for writing, running, and debugging Cypress tests in HiveMQ Edge Frontend  
**Audience:** AI Agents and Developers

---

## 🚨 Critical Rules (Start Here)

### Rule 1: NEVER Use `cy.wait()` with Arbitrary Timeouts

**Problem:** Arbitrary waits cause slow, flaky tests.

**❌ Bad:**

```typescript
cy.get('button').click()
cy.wait(1000) // ❌ NEVER DO THIS
cy.get('.result').should('be.visible')
```

**✅ Good:**

```typescript
cy.get('button').click()
cy.get('.result').should('be.visible') // Cypress retries automatically
```

**Exception:** Use `cy.wait()` ONLY for network requests:

```typescript
cy.intercept('/api/data').as('getData')
cy.get('button').click()
cy.wait('@getData') // ✅ This is OK
cy.get('.result').should('contain', 'Success')
```

---

### Rule 2: NEVER Chain Commands After Action Commands

**Problem:** Commands like `.click()`, `.type()`, `.select()` don't reliably return a subject for chaining.

**❌ Bad:**

```typescript
cy.get('#button').click().should('be.visible')
cy.get('input').type('text').should('have.value', 'text')
cy.get('select').select('option').should('have.value', 'option')
```

**✅ Good:**

```typescript
cy.get('#button').click()
cy.get('#button').should('be.visible')

cy.get('input').type('text')
cy.get('input').should('have.value', 'text')

cy.get('select').select('option')
cy.get('select').should('have.value', 'option')
```

---

### Rule 3: ALWAYS Use Grep for Test Execution

**Problem:** Running all Cypress tests is slow and unnecessary. There are hundreds of tests.

**❌ Bad:**

```bash
npm run cypress:run:component  # Runs ALL tests, takes forever
npm run cypress:run:e2e        # Same problem
```

**✅ Good:**

```bash
# Run specific component test file
npm run cypress:run:component -- --spec "src/modules/Workspace/components/layout/LayoutSelector.spec.cy.tsx"

# Run related tests by glob pattern
npm run cypress:run:component -- --spec "src/modules/Workspace/components/**/*.spec.cy.tsx"

# Run E2E tests matching pattern
npm run cypress:run:e2e -- --spec "cypress/e2e/workspace/workspace-layout*.spec.cy.ts"

# Use grep tags
npm run cypress:run:component -- --env grep="@accessibility"
npm run cypress:run:e2e -- --env grep="@workspace"
```

---

### Rule 4: AVOID `cy.contains().should("be.visible")`

**Problem:** `cy.contains()` can match multiple elements or partial text, causing flaky tests.

**❌ Bad:**

```typescript
cy.contains('Save').should('be.visible') // Might match multiple buttons
cy.contains('Layout').click() // Could match partial text
cy.contains('Error').should('contain.text') // Fragile
```

**✅ Good:**

```typescript
// Use data-testid (most stable)
cy.getByTestId('save-button').should('be.visible')
cy.getByTestId('save-button').should('have.text', 'Save Layout')

// Use ARIA attributes (accessible)
cy.get('button[aria-label="Save layout"]').click()
cy.get('[role="dialog"]').should('be.visible')

// Use semantic selectors with role
cy.get('[role="menu"]').within(() => {
  cy.get('[role="menuitem"]').first().click()
})
```

---

### Rule 5: NEVER Declare Test Work Complete Without Running Tests

**Requirement:** If you create, modify, or update ANY test file, you MUST run those tests and verify they pass.

**❌ Never:**

```markdown
- ❌ Say "tests are complete" without running them
- ❌ Write "all tests passing" without actual results
- ❌ Create completion documentation without test verification
- ❌ Claim "tests should work" or make assumptions
```

**✅ Always:**

```markdown
## Test Verification

Command: `pnpm cypress:run:component --spec "src/components/Toolbar.spec.cy.tsx"`

Results:
```

Toolbar
✓ should render correctly (234ms)
✓ should handle clicks (156ms)
✓ should be accessible (89ms)

3 passing (2s)

```

✅ All tests verified passing.
```

---

### Rule 6: ALWAYS Use Properly Typed React Flow Nodes

**Problem:** Using generic `Node` type allows creating incorrect mock data that doesn't match real node structures, leading to tests that pass but don't reflect reality.

**Critical:** Since React Flow nodes have `type` properties that determine their structure, you MUST use the corresponding TypeScript type.

**❌ Bad - Generic Node with made-up data:**

```typescript
import type { Node } from '@xyflow/react'

// ❌ Wrong: Uses generic Node type with arbitrary data structure
const mockAdapterNode: Node = {
  id: 'adapter-1',
  type: NodeTypes.ADAPTER_NODE,
  position: { x: 100, y: 100 },
  data: {
    id: 'adapter-1',
    label: 'Temperature Sensor', // ❌ Adapter doesn't have a label field!
    adapterId: 'adapter-1', // ❌ Wrong field name!
  },
}
```

**✅ Good - Properly typed with real API data:**

```typescript
import type { NodeAdapterType, NodeBridgeType } from '@/modules/Workspace/types'
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

// ✅ Correct: Uses typed node with actual API data structure
const mockAdapterNode: NodeAdapterType = {
  id: 'adapter-1',
  type: NodeTypes.ADAPTER_NODE,
  position: { x: 100, y: 100 },
  data: {
    ...mockAdapter, // ✅ Uses real Adapter data structure
    id: 'adapter-1',
  },
}

const mockBridgeNode: NodeBridgeType = {
  id: 'bridge-1',
  type: NodeTypes.BRIDGE_NODE,
  position: { x: 300, y: 100 },
  data: {
    ...mockBridge, // ✅ Uses real Bridge data structure
    id: 'bridge-1',
  },
}
```

**Available Node Types:**

```typescript
// Import from @/modules/Workspace/types
NodeAdapterType // For NodeTypes.ADAPTER_NODE
NodeBridgeType // For NodeTypes.BRIDGE_NODE
NodeDeviceType // For NodeTypes.DEVICE_NODE
NodeGroupType // For NodeTypes.CLUSTER_NODE
NodeCombinerType // For NodeTypes.COMBINER_NODE
NodeListenerType // For NodeTypes.LISTENER_NODE
NodePulseType // For NodeTypes.PULSE_NODE
NodeEdgeType // For NodeTypes.EDGE_NODE
NodeHostType // For NodeTypes.HOST_NODE
NodeAssetsType // For NodeTypes.ASSETS_NODE
```

**Where to find mock data:**

```typescript
// Adapter mocks
import { mockAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

// Bridge mocks
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

// Protocol/Device mocks
import { mockProtocolAdapter } from '@/api/hooks/useProtocolAdapters/__handlers__'

// Combiner mocks
import { mockCombiner } from '@/api/hooks/useCombiners/__handlers__'

// Pre-built node mocks (preferred when available)
import {
  MOCK_NODE_ADAPTER,
  MOCK_NODE_BRIDGE,
  MOCK_NODE_DEVICE,
  MOCK_NODE_GROUP,
  // ... etc
} from '@/__test-utils__/react-flow/nodes'
```

**Why this matters:**

- ✅ Type safety catches incorrect data structures at compile time
- ✅ Tests reflect real API data shapes
- ✅ Prevents false positives where tests pass but code would fail in production
- ✅ Makes tests more maintainable when API changes

**Enforcement:** TypeScript should warn you if data doesn't match the expected type. If you see `any` or ignore type errors, you're doing it wrong!

---

## Selector Strategy & Best Practices

### Selector Preference Order

**Use selectors in this order (most preferred first):**

1. **data-testid** - Most stable, purpose-built for testing

   ```typescript
   cy.getByTestId('workspace-layout-selector')
   cy.getByTestId('apply-layout-button')
   ```

2. **ARIA attributes** - Good for accessibility, semantic meaning

   ```typescript
   cy.get('button[aria-label="Layout options"]')
   cy.get('[role="dialog"]')
   cy.get('[role="menu"]')
   ```

3. **Semantic roles** - Use roles for component types

   ```typescript
   cy.get('[role="menuitem"]')
   cy.get('[role="tab"]')
   cy.get('[role="tabpanel"]')
   ```

4. **Class/ID** - Last resort, fragile and maintenance-heavy
   ```typescript
   cy.get('.chakra-button') // ❌ Avoid - will break if CSS changes
   cy.get('#my-element') // ❌ Avoid if possible
   ```

---

## Assertion Best Practices

### Be Specific, Not Generic

**❌ Bad - Too vague:**

```typescript
cy.get('button').should('exist')
cy.get('.item').should('be.visible')
cy.get('#status').should('not.be.empty')
```

**✅ Good - Specific and clear:**

```typescript
cy.getByTestId('apply-layout').should('be.visible')
cy.getByTestId('apply-layout').should('have.text', 'Apply Layout')
cy.getByTestId('apply-layout').should('not.be.disabled')
```

### Chain Multiple Assertions Safely

```typescript
// ✅ Multiple assertions on same query - this is safe
cy.get('input').should('be.visible').should('have.value', 'test').should('not.be.disabled')

// ✅ Use callback for complex checks
cy.get('.items').should(($items) => {
  expect($items).to.have.length.greaterThan(0)
  expect($items.first()).to.contain('First Item')
})

// ✅ Check window/document properties
cy.window().should((win) => {
  expect(win.monaco).to.exist
  expect(win.monaco.editor).to.be.a('object')
})
```

---

## Test Organization & Structure

### Component Test Template

```typescript
describe('ComponentName', () => {
  beforeEach(() => {
    cy.viewport(800, 600)
    useWorkspaceStore.getState().reset()
  })

  it('should render component', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<ComponentName />, { wrapper })
    cy.getByTestId('component-name').should('be.visible')
  })

  it('should handle user interactions', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.mountWithProviders(<ComponentName />, { wrapper })
    cy.getByTestId('action-button').click()
    cy.getByTestId('result').should('be.visible')
  })

  it('should be accessible', () => {
    const wrapper = ({ children }: { children: React.ReactNode }) => (
      <EdgeFlowProvider>
        <ReactFlowProvider>{children}</ReactFlowProvider>
      </EdgeFlowProvider>
    )

    cy.injectAxe()
    cy.mountWithProviders(<ComponentName />, { wrapper })
    cy.checkAccessibility(undefined, {
      rules: {
        region: { enabled: false },
      },
    })
  })
})
```

### E2E Test Template

```typescript
describe('Feature Name', { tags: ['@feature'] }, () => {
  beforeEach(() => {
    cy_interceptCoreE2E()
    cy.intercept('/api/v1/management/adapters', { items: [mockAdapter] }).as('getAdapters')
    cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()

    cy.wait('@getAdapters')
    cy.wait('@getBridges')
  })

  it('should perform expected action', () => {
    // Act
    workspacePage.someControl.click()

    // Assert
    workspacePage.expectedResult.should('be.visible')
  })
})
```

### Keep Tests Focused

**❌ Bad - Testing too much:**

```typescript
it('should create, edit, and delete an item', () => {
  // Way too many steps, hard to debug when it fails
})
```

**✅ Good - One responsibility per test:**

```typescript
it('should create an item', () => {
  cy.get('#create-button').click()
  cy.get('#item-list').should('contain', 'New Item')
})

it('should edit an existing item', () => {
  cy.get('#edit-button').click()
  cy.get('input').clear().type('Updated')
  cy.get('#save-button').click()
  cy.get('#item-list').should('contain', 'Updated')
})

it('should delete an item', () => {
  cy.get('#delete-button').click()
  cy.get('#item-list').should('not.contain', 'Item')
})
```

---

## Cypress Logging Configuration

### 🎯 Why Logging Matters

**Without proper logging, you CANNOT see:**

- Accessibility violations (the ACTUAL rule that failed)
- Console errors from your components
- `cy.log()` debug statements
- Network request details
- Assertion failure details

**Result:** You'll spend HOURS debugging blind!

### ✅ Current Configuration (Already Set Up)

The project is **ALREADY CONFIGURED** for proper logging. Here's what's in place:

#### cypress.config.ts - Logging & Retry Settings

```typescript
retries: { runMode: 0, openMode: 0 }, // Development: 0 (fast feedback)
                                       // CI: 2 (flaky resilience)

component: {
  video: true,
  setupNodeEvents(on, config) {
    codeCoverage(on, config)
    installLogsPrinter(on, {
      printLogsToConsole: 'always',      // ✅ Shows all logs in terminal
      includeSuccessfulHookLogs: false,  // ✅ Keeps output clean
    })
    cypressGrepPlugin(config)
    return config
  },
}
```

#### package.json - Command Configuration

```json
{
  "cypress:run:component": "cypress run --component" // ✅ NO -q flag = verbose
}
```

**Development:** Verbose output for debugging  
**CI:** Consider adding `-q` flag for cleaner logs: `"cypress run -q --component"`

#### cypress/support/component.ts - Support File

```typescript
// ✅ CORRECT - NO installLogsCollector here!
import 'cypress-axe'
import 'cypress-each'
import './commands'
// ... other imports

// ❌ DO NOT ADD:
// import installLogsCollector from 'cypress-terminal-report/src/installLogsCollector'
// installLogsCollector() // ❌ THIS BREAKS EVERYTHING
```

### Example Output With Proper Logging

When a test fails with logging enabled, you see:

```
WizardSelectionPanel
  Accessibility
    1) should be accessible
      cons:error ✘  Warning: Invalid hook call...
      cy:command ✘  uncaught exception
      cy:log ✱  a11y error! scrollable-region-focusable on 1 Node
      log scrollable-region-focusable, [.chakra-card__body],
          <div class="chakra-card__body css-1att3eq">
```

**Without logging:** You only see "1 accessibility violation was detected" (useless!)  
**With logging:** You see EXACT rule + EXACT element (immediately fixable!)

### Debugging with Cypress UI (Best for Accessibility Issues)

```bash
pnpm cypress:open:component
```

**Then:**

1. Click test file in UI
2. Press F12 to open browser DevTools
3. Go to Console tab
4. See EVERYTHING:
   - Component `console.log()` output
   - Accessibility violations with DOM snapshots
   - Full stack traces
   - Network details

### ⚠️ Common Logging Mistakes

**❌ Mistake 1: Adding installLogsCollector to support file**

```typescript
// cypress/support/component.ts
import installLogsCollector from 'cypress-terminal-report/src/installLogsCollector'
installLogsCollector() // ❌ BREAKS ALL TESTS
```

**Result:** All tests fail with React Hook errors  
**Fix:** Delete these lines! Only use `installLogsPrinter` in `cypress.config.ts`

**❌ Mistake 2: Using `-q` flag in development**

```json
{
  "cypress:run:component": "cypress run -q --component" // ❌ Silences output
}
```

**Result:** Can't see what failed  
**Fix:** Remove `-q` during development

**❌ Mistake 3: Setting printLogsToConsole to 'never'**

```typescript
installLogsPrinter(on, {
  printLogsToConsole: 'never', // ❌ Can't debug!
})
```

**Result:** Clean tests but blind debugging  
**Fix:** Use `'always'` during development

---

## Working with Menus and Dropdowns

### Menu Pattern

```typescript
// Open menu
cy.get('button[aria-label*="preset"]').first().click()

// Select within menu
cy.get('[role="menu"]').within(() => {
  cy.get('[role="menuitem"]').first().click()
})

// Or more specific
cy.get('[role="menu"]').within(() => {
  cy.get('[role="menuitem"]').contains('Option Name').click()
})
```

### Dropdown Pattern

```typescript
// Open dropdown
cy.getByTestId('algorithm-selector').click()

// Select option
cy.get('[role="option"]').contains('Dagre Vertical').click()

// Or using select element (if native HTML select)
cy.get('select').select('DAGRE_TB')
```

---

## Avoiding Flaky Tests

### Don't Test UI Feedback with Timing Dependencies

**❌ Flaky - Tooltips have timing issues:**

```typescript
cy.getByTestId('button').trigger('mouseenter')
cy.get('[role="tooltip"]').should('be.visible')
```

**❌ Flaky - Toasts may not appear in test environment:**

```typescript
cy.get('[role="alert"]').should('contain.text', 'Success')
```

**✅ Test actual behavior instead:**

```typescript
cy.window().then(() => {
  const state = useWorkspaceStore.getState()
  expect(state.layoutConfig.presets).to.have.length(1)
})
```

### Don't Rely on Animation Timing

**❌ Flaky - Drawer animation timing:**

```typescript
cy.getByTestId('settings-button').click()
cy.wait(500) // Animation timeout is fragile
cy.percySnapshot('Drawer')
```

**✅ Wait for element visibility:**

```typescript
cy.getByTestId('settings-button').click()
cy.getByTestId('options-drawer').should('be.visible') // Cypress retries
cy.percySnapshot('Drawer')
```

---

## Test Naming Conventions

### Use Descriptive Test Names

**❌ Bad - Too vague:**

```typescript
it('works')
it('renders')
it('handles input')
it('test layout')
```

**✅ Good - Descriptive and specific:**

```typescript
it('should render layout selector with all algorithm options')
it('should apply dagre vertical layout when button clicked')
it('should save custom preset with unique name')
it('should display error message when validation fails')
it('should be accessible with proper aria labels')
```

### Use "should" in Test Names

```typescript
// ✅ Preferred format
it('should render the component')
it('should handle user click')
it('should display error message')
it('should be accessible')

// Also acceptable
it('renders the component')
it('handles user click')
```

---

## Accessibility Testing

### Template for Accessibility Tests

```typescript
it('should be accessible', () => {
  cy.injectAxe()

  cy.mountWithProviders(<ComponentName />, { wrapper })

  cy.checkAccessibility(undefined, {
    rules: {
      // Disable rules that are legitimately unsupported
      'scrollable-region-focusable': { enabled: false },
      'color-contrast': { enabled: false },
    },
  })
})
```

### Critical Accessibility Rules (Don't Disable Without Reason)

These rules must always pass:

- ✅ `select-name` - Select elements MUST have accessible names
- ✅ `button-name` - Buttons MUST have accessible names
- ✅ `link-name` - Links MUST have accessible names
- ✅ `form-field-multiple-labels` - Form fields shouldn't have multiple labels
- ✅ `input-button-name` - Input buttons need accessible names

**Example - Fix for Select without Name:**

```typescript
// ❌ Bad - Select has no accessible name
<Select placeholder="Choose layout">

// ✅ Good - Select has aria-label
<Select
  placeholder="Choose layout"
  aria-label={t('workspace.autoLayout.selector.ariaLabel')}
/>
```

---

## Workspace-Specific Testing

### Mock Data Setup

```typescript
import { MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'

beforeEach(() => {
  cy.intercept('/api/v1/management/protocol-adapters/types', {
    items: [MOCK_PROTOCOL_HTTP, MOCK_PROTOCOL_OPC_UA],
  }).as('getProtocols')

  cy.intercept('/api/v1/management/bridges', { items: [mockBridge] }).as('getBridges')

  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [mockAdapter_OPCUA],
  }).as('getAdapters')
})
```

### Workspace Page Object Usage

```typescript
import { loginPage, workspacePage } from 'cypress/pages'

beforeEach(() => {
  cy_interceptCoreE2E()
  // ... additional intercepts ...

  loginPage.visit('/app/workspace')
  loginPage.loginButton.click()
  workspacePage.navLink.click()

  cy.wait('@getAdapters')
  cy.wait('@getBridges')
  workspacePage.toolbox.fit.click()
})

it('should apply layout', () => {
  workspacePage.canvasToolbar.expandButton.click()
  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  workspacePage.layoutControls.applyButton.click()

  workspacePage.edgeNode.should('be.visible')
})
```

---

## Test Verification Checklist

Before declaring test work complete:

- [ ] Tests run with `--spec` flag (not all tests)
- [ ] Tests pass with 100% success rate
- [ ] No arbitrary `cy.wait()` calls
- [ ] No chaining after action commands
- [ ] No `cy.contains().should()` patterns
- [ ] Accessibility tests included
- [ ] Test names are descriptive
- [ ] Assertions are specific
- [ ] Page Objects used correctly
- [ ] Mock data properly configured

---

## Quick Reference Commands

```bash
# Run specific component test
pnpm cypress:run:component --spec "src/path/to/Component.spec.cy.tsx"

# Run E2E tests matching pattern
pnpm cypress:run:e2e --spec "cypress/e2e/feature/test-*.spec.cy.ts"

# Run tests matching grep tag
pnpm cypress:run:component -- --env grep="@accessibility"

# Open Cypress UI (best for debugging)
pnpm cypress:open:component
pnpm cypress:open:e2e

# Run with specific browser
pnpm cypress:run:component --browser chrome --spec "..."
pnpm cypress:run:component --browser firefox --spec "..."
```

---

## Additional Resources

- **Cypress Official Docs:** https://docs.cypress.io
- **WAI-ARIA Patterns:** https://www.w3.org/WAI/ARIA/apg/patterns/
- **Workspace Testing:** See `WORKSPACE_TESTING_GUIDELINES.md`
- **User Documentation:** See `USER_DOCUMENTATION_GUIDELINE.md`

---

## Version History

| Version | Date         | Changes                                                                                                     |
| ------- | ------------ | ----------------------------------------------------------------------------------------------------------- |
| 1.0     | Nov 12, 2025 | Consolidated from CYPRESS*BEST_PRACTICES.md, CYPRESS_TESTING_BEST_PRACTICES.md, CYPRESS_LOGGING*\*.md files |
