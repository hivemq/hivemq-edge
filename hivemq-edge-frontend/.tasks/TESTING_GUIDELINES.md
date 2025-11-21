# HiveMQ Edge Frontend - Testing Guidelines

**Last Updated:** November 12, 2025

---

## 🚨 CRITICAL RULE: NEVER Declare Test Work Complete Without Running Tests

**ABSOLUTE REQUIREMENT: If you create, modify, or update ANY test file, you MUST run those tests and verify they pass BEFORE declaring the work complete.**

### The Non-Negotiable Rule

**NEVER:**

- ❌ Say "tests are complete" without running them
- ❌ Write "all tests passing" without seeing actual results
- ❌ Create completion documentation without test verification
- ❌ Claim "tests should work" or make assumptions
- ❌ Mark test-related work as done without green test results
- ❌ Run every Cypress test unless instructed to do so

**ALWAYS:**

- ✅ Run the actual test command
- ✅ Read and verify the test output
- ✅ See the actual pass/fail counts
- ✅ Fix failures immediately
- ✅ Include real test results in completion documentation
- ✅ Run individual Cypress tests, using the --spec option

### Required Test Commands

**Component Tests:**

```bash
pnpm cypress:run:component --spec "path/to/Component.spec.cy.tsx"
```

**E2E Tests:**

```bash
pnpm cypress:run:e2e --spec "cypress/e2e/path/to/test.spec.cy.ts"
```

**Multiple Test Files:**

```bash
# Use glob patterns for related tests
pnpm cypress:run:e2e --spec "cypress/e2e/workspace/workspace-layout*.spec.cy.ts"
```

### What "Test-Related Work" Means

Work involves tests if it includes:

- Creating new test files
- Modifying existing tests
- Updating Page Objects or test utilities
- Changing components that have tests
- Updating test selectors or test-ids
- Fixing test failures

### Proper Completion Documentation

**Required format when completing test work:**

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

### Why This Rule Exists

**Past Issues:**

- Tests declared complete but actually failing
- User runs tests and finds failures
- Wasted time and broken trust
- Overconfident claims without verification

**The Solution:**

- Always run tests before claiming completion
- Show real results, not assumptions
- Fix issues immediately
- Build trust through verification

---

## Table of Contents

1. [Cypress Testing Guidelines](#cypress-testing-guidelines) ⚠️ **COMPREHENSIVE CYPRESS REFERENCE**
2. [Accessibility Testing Patterns](#accessibility-testing-patterns)
3. [Component Testing Patterns](#component-testing-patterns)
4. [Selector Best Practices](#selector-best-practices)
5. [Screenshot Documentation](#screenshot-documentation)
6. [Page Object Linking](#page-object-linking)

---

## Cypress Testing Guidelines

**⚠️ Comprehensive Cypress reference has been consolidated into a single document.**

### 📖 Reference Document

**All Cypress testing guidelines including critical rules, best practices, logging configuration, and debugging techniques are now in:**

### 👉 **[CYPRESS_TESTING_GUIDELINES.md](./CYPRESS_TESTING_GUIDELINES.md)**

This document covers:

- **Critical Rules** (5 essential rules all tests must follow)
- **Selector Strategy** (best practices for finding elements)
- **Assertion Best Practices** (specific and reliable assertions)
- **Test Organization** (templates and structure)
- **Logging Configuration** (debugging setup - already configured!)
- **Avoiding Flaky Tests** (common pitfalls and solutions)
- **Test Naming Conventions** (clear, descriptive test names)
- **Accessibility Testing** (mandatory patterns)
- **Workspace-Specific Testing** (mock data, page objects)
- **Quick Reference Commands** (copy-paste ready)

**If you're writing Cypress tests, START with that document!**

---

### 📚 Additional Resources

**Related testing documentation:**

- **[CYPRESS_LOGGING_INDEX.md](./CYPRESS_LOGGING_INDEX.md)** - Master index for logging documentation (if you need detailed logging info)
- **[WORKSPACE_TESTING_GUIDELINES.md](./WORKSPACE_TESTING_GUIDELINES.md)** - Workspace-specific mock data and patterns

---

### 🎯 Quick Links to Common Patterns

**From CYPRESS_TESTING_GUIDELINES.md:**

- [Critical Rules](./CYPRESS_TESTING_GUIDELINES.md#critical-rules-start-here) - Start here
- [Selector Strategy](./CYPRESS_TESTING_GUIDELINES.md#selector-strategy--best-practices)
- [Test Organization](./CYPRESS_TESTING_GUIDELINES.md#test-organization--structure)
- [Logging Configuration](./CYPRESS_TESTING_GUIDELINES.md#cypress-logging-configuration)
- [Common Testing Patterns](./CYPRESS_TESTING_GUIDELINES.md#working-with-menus-and-dropdowns)
- [Accessibility Testing](./CYPRESS_TESTING_GUIDELINES.md#accessibility-testing)
- [Workspace Testing](./CYPRESS_TESTING_GUIDELINES.md#workspace-specific-testing)

---

- https://github.com/archfz/cypress-terminal-report

**Key Options:**

- `printLogsToConsole`: `'always'` | `'onFail'` | `'never'`
- `includeSuccessfulHookLogs`: Show/hide beforeEach/afterEach logs
- `collectTypes`: Which log types to capture (default: all)

**When to read docs:**

- Need to filter specific log types
- Want to customize log format
- Need to integrate with CI tools
- Want to save logs to files

---

## Custom Cypress Commands

### ✅ MANDATORY: Use Custom Commands for Common Patterns

**CRITICAL REQUIREMENT:** Always use custom Cypress commands instead of verbose native Cypress code for better readability and consistency.

#### data-testid Selector

**ALWAYS use `cy.getByTestId()` instead of `cy.get('[data-testid="..."]')`**

❌ **WRONG - Verbose and inconsistent:**

```typescript
cy.get('[data-testid="my-button"]').click()
cy.get('[data-testid="dialog-title"]').should('be.visible')
```

✅ **CORRECT - Clean and consistent:**

```typescript
cy.getByTestId('my-button').click()
cy.getByTestId('dialog-title').should('be.visible')
```

**Benefits:**

- Shorter and more readable
- Consistent across all tests
- Easier to refactor if selector strategy changes
- Clear intent (looking for test IDs specifically)

#### Other Custom Commands

**Available custom commands you MUST use:**

- `cy.mountWithProviders()` - Mount React components with providers

---

## React Flow Component Testing

### ✅ MANDATORY: Use ReactFlowTesting Wrapper for Components Using React Flow

**CRITICAL:** Components that use `useReactFlow()` or depend on nodes/edges require special setup.

#### The Problem

Components using `useReactFlow().getNodes()` or `useReactFlow().getEdges()` need nodes/edges to be in React Flow's internal state, not just the workspace store.

#### The Solution: ReactFlowTesting Wrapper

**Location:** `src/__test-utils__/react-flow/ReactFlowTesting.tsx`

**Example Pattern:**

```typescript
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'
import type { Node } from '@xyflow/react'

const mockNodes: Node[] = [
  {
    id: 'adapter-1',
    type: 'ADAPTER_NODE',
    position: { x: 100, y: 100 },
    data: { id: 'adapter-1', label: 'My Adapter' },
  },
]

const getWrapperWith = (initialNodes?: Node[]) => {
  const Wrapper: React.JSXElementConstructor<{ children: React.ReactNode }> = ({ children }) => {
    return (
      <ReactFlowTesting
        config={{
          initialState: {
            nodes: initialNodes,
            edges: [],
          },
        }}
      >
        {children}
      </ReactFlowTesting>
    )
  }
  return Wrapper
}

// In your test:
cy.mountWithProviders(<MyComponent />, { wrapper: getWrapperWith(mockNodes) })
```

**What ReactFlowTesting Does:**

1. Resets workspace store
2. Calls `onAddNodes()` and `onAddEdges()` to properly add items
3. Wraps in EdgeFlowProvider and ReactFlowProvider
4. Ensures React Flow's `getNodes()`/`getEdges()` return the test data

**When to Use:**

- ✅ Component uses `useReactFlow()`
- ✅ Component calls `getNodes()` or `getEdges()`
- ✅ Component depends on workspace nodes/edges being in React Flow state

**Real Example:** See `src/modules/Workspace/components/drawers/DevicePropertyDrawer.spec.cy.tsx`

**⚠️ Known Limitation:**
ReactFlowTesting uses `useEffect` to add nodes/edges, which is async. Components that call `getNodes()` immediately on mount will see an empty array initially, then re-render when nodes are added. If testing such components:

1. Wait for workspace store to have nodes: `cy.wrap(null).should(() => expect(useWorkspaceStore.getState().nodes.length).to.be.greaterThan(0))`
2. Wait for the specific DOM elements that depend on nodes to appear
3. Use longer timeouts if needed
4. Consider if the component can be tested differently (e.g., test props/events instead of node rendering)

- `cy.injectAxe()` - Inject axe-core for accessibility testing
- `cy.checkAccessibility()` - Run accessibility checks (DO NOT use `cy.checkA11y()` directly!)
- `cy.getByTestId(id)` - Select elements by data-testid (DO NOT use `cy.get('[data-testid="..."]')`)

**Always check for existing custom commands before writing verbose alternatives!**

---

## Accessibility Testing

### ✅ MANDATORY: Accessibility Test for Every Component

**CRITICAL REQUIREMENT:** Every Cypress component test file MUST include an accessibility test.

#### Required Test Pattern

```tsx
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<YourComponent {...meaningfulProps} />)
  cy.checkAccessibility()
})
```

#### ⚠️ CRITICAL: Use cy.checkAccessibility() NOT cy.checkA11y()

**ALWAYS use the custom command `cy.checkAccessibility()` instead of `cy.checkA11y()` directly.**

❌ **WRONG - DO NOT USE:**

```tsx
cy.checkA11y() // Never use this directly!
```

✅ **CORRECT - Use custom command:**

```tsx
cy.checkAccessibility() // Always use our custom command
```

**Why:**

- `cy.checkAccessibility()` provides better and consistent props
- Standardized error handling
- Team-wide consistency
- Easier to update globally if needed

#### Key Requirements

1. **Test Name:** MUST be exactly `"should be accessible"`
2. **Order:** Should typically be the last test in the describe block
3. **Setup:** Must call `cy.injectAxe()` before mounting
4. **Command:** MUST use `cy.checkAccessibility()` NOT `cy.checkA11y()`
5. **Props:** Use meaningful, representative props that show the component's real usage
6. **Interactions:** Optionally test accessibility during/after user interactions
7. **Screenshot:** Optionally capture a screenshot for PR documentation

### ⚠️ CRITICAL: Select Components MUST Have Accessible Names

**All `<Select>` components MUST have an accessible name via `aria-label`.**

❌ **WRONG - Will fail accessibility:**

```tsx
<Select value={currentValue} onChange={handleChange}>
  <option>...</option>
</Select>
```

✅ **CORRECT - Accessible:**

```tsx
<Select aria-label={t('component.selector.ariaLabel')} value={currentValue} onChange={handleChange}>
  <option>...</option>
</Select>
```

**Why this matters:**

- Screen readers need to announce what the select control is for
- Axe accessibility scanner will fail without an accessible name
- See: https://dequeuniversity.com/rules/axe/4.10/select-name

**Alternative approaches (also valid):**

- Use `aria-labelledby` to reference a label element by ID
- Wrap in a `<FormControl>` with a `<FormLabel>` (Chakra UI will connect them)

---

## Accessibility Testing Patterns

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

## Component Testing Patterns

### Basic Component Test Structure

```typescript
describe('ComponentName', () => {
  beforeEach(() => {
    cy.viewport(800, 600)

    // Reset store before each test
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
        'color-contrast': { enabled: false },
      },
    })
  })
})
```

### Testing Loading States

**Pattern:** `LoaderSpinner` always has `data-testid="loading-spinner"`

```typescript
// Wait for loading spinner to appear
cy.getByTestId('loading-spinner').should('be.visible')

// Wait for loading to finish
cy.getByTestId('loading-spinner').should('not.exist')
```

### Testing Components with Drawer Context

```typescript
const mountComponent = () => {
  const Wrapper = () => (
    <Drawer isOpen={true} onClose={() => {}} placement="right" size="lg">
      <DrawerOverlay />
      <DrawerContent>
        <YourComponent />
      </DrawerContent>
    </Drawer>
  )
  return cy.mountWithProviders(<Wrapper />)
}
```

### Testing Scrollable Content

```typescript
// Split scrollIntoView and assertion for ESLint compliance
cy.contains('My Element').scrollIntoView()
cy.contains('My Element').should('be.visible')
```

---

## Selector Best Practices

### NEVER Use CSS Classnames as Selectors

**CRITICAL RULE: DO NOT use classnames like `.chakra-grid`, `.css-xyz123` in tests!**

**Why classnames are forbidden:**

- Implementation details that change with library updates
- Generated classes like `.css-xyz123` are random/unstable
- Break when switching CSS-in-JS libraries
- Break when updating Chakra UI or other UI libraries

**Priority order (best to worst):**

1. **`data-testid`** - Best, explicit test identifier
2. **`id`** - Good if stable and semantic
3. **ARIA roles** - `[role="search"]`, `[role="list"]`, `[role="dialog"]`
4. **ARIA labels** - `[aria-label="Close"]`, `[aria-labelledby="..."]`
5. **Text content** - `cy.contains('Button Text')`
6. **Semantic elements** - `button`, `input[type="text"]`

**Examples:**

```typescript
// ✅ Use data-testid
cy.getByTestId('loading-spinner')

// ✅ Use id
cy.get('#facet-search-input')

// ✅ Use ARIA role
cy.get('[role="search"]').should('be.visible')
cy.get('[role="list"]').should('be.visible')

// ✅ Use ARIA label
cy.get('[aria-label="Close"]').click()

// ✅ Use text content
cy.contains('Submit').click()

// ✅ Combine for specificity
cy.get('[role="dialog"]').within(() => {
  cy.contains('button', 'Save').click()
})
```

### Testing Layout/Structure (Don't Test CSS)

```typescript
// ❌ WRONG - Testing CSS classes
it('should show grid layout', () => {
  cy.get('.chakra-grid').should('exist')
})

// ✅ CORRECT - Test the visible elements
it('should show search panel and protocols side by side', () => {
  cy.get('[role="search"]').should('be.visible')
  cy.get('[role="list"]').should('be.visible')
  cy.get('#facet-search-input').should('be.visible')
})
```

---

## Critical Testing Requirements

### ✅ MANDATORY: All Mocks Must Be Typed

**CRITICAL:** Every mock object in tests MUST have explicit TypeScript typing.

```typescript
// ❌ Wrong - Untyped Mock
const mockData = {
  id: '123',
  name: 'Test',
}

// ✅ Correct - Typed Mock
const mockData: MyDataType = {
  id: '123',
  name: 'Test',
}
```

### ✅ MANDATORY: Use Enums, Not String Literals

**CRITICAL:** Always use the correct enum types from the API, never string literals.

```typescript
// ❌ Wrong - String Literals
const mockMapping = {
  type: 'ADAPTER', // String literal
}

// ✅ Correct - Enum Types
const mockMapping: DataCombining = {
  type: EntityType.ADAPTER, // Enum
}
```

### ✅ MANDATORY: No Arbitrary Waits

**CRITICAL:** Never use `cy.wait()` with arbitrary time periods.

```typescript
// ❌ Wrong - Arbitrary Wait
cy.wait(500)
cy.getByTestId('data').should('be.visible')

// ✅ Correct - Wait for Conditions
cy.getByTestId('data').should('be.visible')
```

---

## Waiting for CSS Animations Before Screenshots

### Problem: Transparent or Incomplete Elements

When taking screenshots of components with CSS animations, the screenshot may capture mid-animation.

### ✅ Correct - Wait for Animation Completion

```typescript
it('should be accessible', { tags: ['@percy'] }, () => {
  cy.injectAxe()
  cy.mountWithProviders(<MyModal isOpen={true} />)

  cy.getByTestId('modal').should('be.visible')
  cy.checkAccessibility()
  cy.percySnapshot('My Modal')

  // ✅ Wait for opacity to reach 1 (animation complete)
  cy.getByTestId('modal').should('have.css', 'opacity', '1')

  // Now screenshot will show fully rendered modal
  cy.screenshot('my-modal', { capture: 'viewport', overwrite: true })
})
```

### Order of Operations

```typescript
// 1. Verify element visibility
cy.getByTestId('modal').should('be.visible')

// 2. Run accessibility checks
cy.checkAccessibility()

// 3. Take Percy snapshot
cy.percySnapshot('My Modal')

// 4. Wait for animation completion
cy.getByTestId('modal').should('have.css', 'opacity', '1')

// 5. Take Cypress screenshot (LAST)
cy.screenshot('modal-state', { capture: 'viewport', overwrite: true })
```

---

## Screenshot Documentation

### Using Cypress Screenshots for PR Docs

```typescript
// Basic screenshot
cy.screenshot('my-component', {
  capture: 'viewport',
  overwrite: true,
})

// Fullpage screenshot
cy.screenshot('my-component-full', {
  capture: 'fullPage',
  overwrite: true,
})
```

### Using Percy for Visual Regression

```typescript
// Percy snapshot (for visual regression testing)
cy.percySnapshot('Component Name')
```

---

## Test Naming Conventions

### Use Descriptive Test Names

```typescript
// ✅ Good - Descriptive and specific
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
```

---

## Page Object Linking

### Workspace Page Object Usage

```typescript
import { loginPage, workspacePage } from 'cypress/pages'

beforeEach(() => {
  cy_interceptCoreE2E()
  loginPage.visit('/app/workspace')
  loginPage.loginButton.click()
  workspacePage.navLink.click()
})

it('should apply layout', () => {
  workspacePage.canvasToolbar.expandButton.click()
  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  workspacePage.layoutControls.applyButton.click()
  workspacePage.edgeNode.should('be.visible')
})
```

---

## Checklist for New Component Tests

When creating a new Cypress component test:

- [ ] File follows naming: `{ComponentName}.spec.cy.tsx`
- [ ] Includes `/// <reference types="cypress" />` at top
- [ ] Uses `cy.mountWithProviders()` for mounting
- [ ] Tests core functionality and edge cases
- [ ] Uses meaningful `data-testid` attributes
- [ ] **Includes `"should be accessible"` test** ✅ MANDATORY
- [ ] Accessibility test uses `cy.injectAxe()` before mount
- [ ] Accessibility test uses representative props
- [ ] **All mocks are properly typed** ✅ MANDATORY
- [ ] **Uses correct enum types (not string literals)** ✅ MANDATORY
- [ ] **No arbitrary waits (`cy.wait()` with numbers)** ✅ MANDATORY
- [ ] No CSS classname selectors
- [ ] All tests pass locally before committing

---

## Quick Test Command Reference

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
```

---

## Additional Resources

- **Cypress Official Docs:** https://docs.cypress.io
- **WAI-ARIA Patterns:** https://www.w3.org/WAI/ARIA/apg/patterns/
- **Workspace Testing:** See `WORKSPACE_TESTING_GUIDELINES.md`
- **User Documentation:** See `USER_DOCUMENTATION_GUIDELINE.md`
