# HiveMQ Edge Frontend - Testing Guidelines

**Last Updated:** October 31, 2025

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

**ALWAYS:**

- ✅ Run the actual test command
- ✅ Read and verify the test output
- ✅ See the actual pass/fail counts
- ✅ Fix failures immediately
- ✅ Include real test results in completion documentation

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

## 🧪 Proof-of-Concept (POC) Testing Strategy

**Purpose:** Establish basic test coverage for POC implementations without requiring full test suites.

### POC Testing Requirements

For features marked as **Proof-of-Concept**, implement minimal but meaningful tests:

#### 1. Component Tests (Required)

**Every new component MUST have a Cypress component test file** with at minimum:

```tsx
describe('MyPOCComponent', () => {
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MyPOCComponent {...representativeProps} />)
    cy.checkAccessibility()
  })
})
```

**Benefits:**

- ✅ Validates component renders without errors
- ✅ Ensures basic accessibility compliance
- ✅ Provides runtime component context for debugging
- ✅ Establishes test structure for future expansion
- ✅ Helps AI agents understand component behavior

**Optional additions:**

- Basic interaction test (if component has critical interactions)
- Screenshot for visual reference

#### 2. E2E Tests (Required)

**Every new feature MUST have a basic E2E test** that verifies access to the feature in context:

```tsx
describe('POC Feature Access', () => {
  it('should access the new feature', () => {
    // Navigate to feature location
    cy.visit('/workspace')
    cy.getByTestId('edge-node').click()
    cy.getByTestId('open-panel-button').click()

    // Verify feature is accessible
    cy.getByTestId('new-feature-tab').should('be.visible')
    cy.getByTestId('new-feature-tab').click()
    cy.getByTestId('new-feature-content').should('be.visible')
  })
})
```

**Required E2E Test Structure:**

- ✅ Create or update relevant Page Object
- ✅ Test only the actions required to access the feature
- ✅ Verify feature renders/displays correctly
- ✅ Use `data-testid` selectors via `cy.getByTestId()`

**Benefits:**

- ✅ Validates feature integration with existing UI
- ✅ Ensures navigation/access patterns work
- ✅ Documents feature location for future reference
- ✅ Provides E2E context for debugging
- ✅ Prevents accidental breakage of access paths

#### 3. Page Object Updates (Required if applicable)

If the feature is accessed through existing pages, **update or create Page Objects**:

```typescript
// cypress/pages/WorkspaceEdgePanel.ts
export class WorkspaceEdgePanel {
  // ...existing code...

  openNetworkGraphTab() {
    cy.getByTestId('ontology-panel-tab-network').click()
    return this
  }

  verifyNetworkGraphVisible() {
    cy.getByTestId('edge-panel-network-graph').should('be.visible')
    return this
  }
}
```

### POC Testing Exemptions

POC implementations are **exempt from**:

- ❌ Full test coverage requirements
- ❌ Edge case testing
- ❌ Complex interaction testing
- ❌ Performance testing
- ❌ Multiple browser testing

### Transition from POC to Production

When promoting POC to production-ready:

1. ✅ Add comprehensive test coverage
2. ✅ Test edge cases and error states
3. ✅ Add performance tests if applicable
4. ✅ Test keyboard navigation thoroughly
5. ✅ Test screen reader compatibility
6. ✅ Add visual regression tests

### Example: Complete POC Test Suite

**Component Test:** `NetworkGraphView.spec.cy.tsx`

```tsx
describe('NetworkGraphView (POC)', () => {
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NetworkGraphView />)
    cy.checkAccessibility()
  })
})
```

**E2E Test:** `network-graph-access.spec.cy.ts`

```tsx
import { WorkspaceEdgePanel } from '@pages/WorkspaceEdgePanel'

describe('Network Graph Access (POC)', () => {
  beforeEach(() => {
    cy.visit('/workspace')
  })

  it('should access network graph visualization', () => {
    // Navigate to edge panel
    cy.getByTestId('workspace-edge-node').click()
    cy.getByTestId('edge-open-panel').click()

    // Access network graph tab
    const edgePanel = new WorkspaceEdgePanel()
    edgePanel.openNetworkGraphTab()
    edgePanel.verifyNetworkGraphVisible()
  })
})
```

---

## Table of Contents

1. [Accessibility Testing](#accessibility-testing)
2. [Component Testing Patterns](#component-testing-patterns)
3. [Screenshot Documentation](#screenshot-documentation)
4. [Test Naming Conventions](#test-naming-conventions)
5. [Dynamic IDs and Partial Selectors](#dynamic-ids-and-partial-selectors)
6. [Page Object Linking](#page-object-linking)

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

## Component Testing Patterns

### Basic Accessibility Test

```tsx
describe('MyComponent', () => {
  it('should render correctly', () => {
    // ... rendering tests
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MyComponent title="Example" isOpen={true} />)
    cy.checkAccessibility()
  })
})
```

### Accessibility Test with Interactions

Test accessibility during common user interactions:

```tsx
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<MyModal isOpen={true} onClose={cy.stub()} />)

  // Test initial state
  cy.checkAccessibility()

  // Test after interaction
  cy.getByTestId('modal-button').click()
  cy.checkAccessibility()

  // Test keyboard navigation
  cy.get('body').type('{tab}')
  cy.checkAccessibility()
})
```

### Accessibility Test with Screenshot

Capture a visual reference for PR documentation:

```tsx
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<MyComponent {...representativeProps} />)

  // Verify accessibility
  cy.checkAccessibility()

  // Capture screenshot for PR documentation
  cy.screenshot('my-component-accessible-state', {
    capture: 'fullPage',
    overwrite: true,
  })
})
```

---

## Screenshot Documentation

### When to Capture Screenshots

Screenshots in accessibility tests serve multiple purposes:

1. **PR Documentation**: Visual reference for reviewers
2. **Design Review**: Show component appearance to stakeholders
3. **Regression Detection**: Baseline for visual testing
4. **Accessibility Audit**: Document accessible states

### Screenshot Best Practices

```tsx
it('should be accessible', () => {
  cy.viewport(1280, 900) // Consistent viewport size
  cy.injectAxe()

  cy.mountWithProviders(<MyComponent {...meaningfulProps} />)

  // Wait for animations/loading
  cy.wait(500)

  // Check accessibility first
  cy.checkAccessibility()

  // Then capture screenshot
  cy.screenshot('component-name-description', {
    capture: 'fullPage',
    overwrite: true,
  })
})
```

### Screenshot Naming Convention

Format: `{component-name}-{state-description}`

Examples:

- `duplicate-combiner-modal-with-mappings`
- `combiner-mappings-list-empty-state`
- `toolbar-multiple-selection`

---

## Test Naming Conventions

### Standard Test Names

Use consistent, descriptive test names:

✅ **Good Examples:**

```tsx
it('should render the modal with combiner information', () => {})
it('should call onSubmit when form is submitted', () => {})
it('should disable button when loading', () => {})
it('should be accessible', () => {})
```

❌ **Bad Examples:**

```tsx
it('works', () => {})
it('test modal', () => {})
it('check if accessible', () => {}) // Wrong name!
```

### Accessibility Test Name

**MUST BE:** `"should be accessible"`

This exact naming ensures:

- Easy identification in test reports
- Consistent grep/search results
- Team-wide understanding
- Automated tooling compatibility

---

## Complete Example

Here's a complete component test file following all guidelines:

```tsx
/// <reference types="cypress" />

import MyModal from './MyModal'

describe('MyModal', () => {
  const mockProps = {
    isOpen: true,
    onClose: cy.stub(),
    title: 'Example Modal',
    items: [
      { id: '1', name: 'Item 1' },
      { id: '2', name: 'Item 2' },
    ],
  }

  beforeEach(() => {
    cy.viewport(1280, 900)
  })

  it('should render the modal with title and items', () => {
    cy.mountWithProviders(<MyModal {...mockProps} />)

    cy.getByTestId('modal-title').should('contain.text', 'Example Modal')
    cy.getByTestId('modal-items').children().should('have.length', 2)
  })

  it('should call onClose when cancel button is clicked', () => {
    const onClose = cy.stub().as('onClose')

    cy.mountWithProviders(<MyModal {...mockProps} onClose={onClose} />)

    cy.getByTestId('modal-button-cancel').click()
    cy.get('@onClose').should('have.been.calledOnce')
  })

  it('should support keyboard navigation', () => {
    cy.mountWithProviders(<MyModal {...mockProps} />)

    cy.get('body').type('{esc}')
    // Assert expected behavior
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MyModal {...mockProps} />)

    // Test initial accessible state
    cy.checkAccessibility()

    // Test accessibility after interaction
    cy.getByTestId('modal-button').click()
    cy.checkAccessibility()

    // Optional: Capture screenshot for PR documentation
    cy.screenshot('my-modal-accessible', {
      capture: 'fullPage',
      overwrite: true,
    })
  })
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
- [ ] Optional: Captures screenshot for PR documentation
- [ ] All tests pass locally before committing

---

## Critical Testing Requirements

### ✅ MANDATORY: All Mocks Must Be Typed

**CRITICAL:** Every mock object in tests MUST have explicit TypeScript typing.

#### ❌ Wrong - Untyped Mock

```tsx
it('should render correctly', () => {
  // BAD: No type annotation
  const mockData = {
    id: '123',
    name: 'Test',
  }

  cy.mountWithProviders(<MyComponent data={mockData} />)
})
```

#### ✅ Correct - Typed Mock

```tsx
it('should render correctly', () => {
  // GOOD: Explicit type annotation
  const mockData: MyDataType = {
    id: '123',
    name: 'Test',
  }

  cy.mountWithProviders(<MyComponent data={mockData} />)
})
```

**Why this matters:**

- Catches type errors at compile time
- Ensures mocks match actual data structures
- Prevents runtime errors from mock data mismatches
- Makes tests more maintainable when types change

---

### ✅ MANDATORY: Use Enums, Not String Literals

**CRITICAL:** Always use the correct enum types from the API, never string literals.

#### ❌ Wrong - String Literals

```tsx
const mockMapping = {
  id: 'mapping-1',
  sources: {
    primary: {
      id: 'source-1',
      type: 'ADAPTER', // ❌ Wrong! String literal
    },
  },
}
```

#### ✅ Correct - Enum Types

```tsx
import { DataIdentifierReference } from '@/api/__generated__'

const mockMapping: DataCombining = {
  id: 'mapping-1',
  sources: {
    primary: {
      id: 'source-1',
      type: DataIdentifierReference.type.TAG, // ✅ Correct! Enum
    },
  },
}
```

**Common enum types:**

- `EntityType.ADAPTER`, `EntityType.BRIDGE`, `EntityType.EDGE_BROKER`, etc. (for combiner sources)
- `DataIdentifierReference.type.TAG`, `DataIdentifierReference.type.TOPIC_FILTER`, `DataIdentifierReference.type.PULSE_ASSET` (for mapping sources)

**Why this matters:**

- Type safety - compiler catches invalid values
- Refactoring - changes to enums update everywhere
- Consistency - ensures test data matches production data
- Documentation - makes valid values obvious

---

### ✅ MANDATORY: No Arbitrary Waits

**CRITICAL:** Never use `cy.wait()` with arbitrary time periods.

#### ❌ Wrong - Arbitrary Wait

```tsx
it('should display data', () => {
  cy.mountWithProviders(<MyComponent />)

  cy.wait(500) // ❌ Wrong! Arbitrary wait
  cy.getByTestId('data').should('be.visible')
})
```

#### ✅ Correct - Wait for Conditions

```tsx
it('should display data', () => {
  cy.mountWithProviders(<MyComponent />)

  // GOOD: Wait for specific condition
  cy.getByTestId('data').should('be.visible')

  // GOOD: Wait for network request
  cy.intercept('GET', '/api/data').as('getData')
  cy.wait('@getData')

  // GOOD: Wait for element state
  cy.getByTestId('spinner').should('not.exist')
  cy.getByTestId('content').should('be.visible')
})
```

**Alternatives to arbitrary waits:**

- `cy.get().should()` - Wait for element conditions
- `cy.wait('@alias')` - Wait for intercepted network requests
- `cy.should()` assertions - Automatically retry until condition met
- Custom commands with built-in retrying

**Why this matters:**

- Flaky tests - arbitrary waits may be too short or too long
- Slow tests - waiting longer than necessary
- False positives - test passes during wait, fails after
- Maintainability - magic numbers are unclear

**ESLint will error:** `cypress/no-unnecessary-waiting`

---

## Waiting for CSS Animations Before Screenshots

### Problem: Transparent or Incomplete Elements in Screenshots

When taking screenshots of components with CSS animations (modals, dialogs, tooltips), the screenshot may capture the element mid-animation, resulting in:

- Partial transparency/opacity
- Incomplete slide-in animations
- Blurred or distorted elements
- Unprofessional appearance in PR documentation

#### ❌ Wrong - Taking Screenshot Immediately

```tsx
it('should be accessible', { tags: ['@percy'] }, () => {
  cy.injectAxe()
  cy.mountWithProviders(<MyModal isOpen={true} />)

  cy.getByTestId('modal').should('be.visible')
  cy.checkAccessibility()

  // ❌ Bad: Screenshot may capture animation mid-flight
  cy.screenshot('my-modal', { capture: 'viewport' })
})
```

#### ❌ Also Wrong - Using Arbitrary Wait

```tsx
// ❌ Bad: Arbitrary wait triggers ESLint warning
cy.wait(400) // cypress/no-unnecessary-waiting
cy.screenshot('my-modal', { capture: 'viewport' })
```

#### ✅ Correct - Wait for Animation Completion via CSS Property

```tsx
it('should be accessible', { tags: ['@percy'] }, () => {
  cy.injectAxe()
  cy.mountWithProviders(<MyModal isOpen={true} />)

  cy.getByTestId('modal').should('be.visible')
  cy.checkAccessibility()
  cy.percySnapshot('My Modal')

  // ✅ Good: Wait for opacity to reach 1 (animation complete)
  cy.getByTestId('modal').should('have.css', 'opacity', '1')

  // Now screenshot will show fully rendered modal
  cy.screenshot('my-modal', { capture: 'viewport', overwrite: true })
})
```

### How It Works

**Chakra UI Animations**: Components using `motionPreset` (like `"slideInBottom"`, `"scale"`, etc.) animate CSS properties including:

- `opacity`: 0 → 1
- `transform`: translateY/scale changes
- `transition`: Smooth easing over ~200-400ms

**Cypress Retry-ability**: The `.should('have.css', 'opacity', '1')` assertion:

- Automatically retries until opacity reaches exactly `1`
- Adapts to different system speeds
- No arbitrary timing needed
- Self-documenting code

### Common CSS Properties to Check

Depending on the animation type, check the appropriate property:

```tsx
// For fade-in animations
cy.getByTestId('element').should('have.css', 'opacity', '1')

// For slide animations (check transform is at final position)
cy.getByTestId('element').should('have.css', 'transform', 'none')
// or verify it's not "matrix(...)" which indicates ongoing transform

// For visibility-based animations
cy.getByTestId('element').should('be.visible')
cy.getByTestId('element').should('not.have.class', 'animating')
```

### Real Example: Duplicate Combiner Modal

From `cypress/e2e/workspace/duplicate-combiner.spec.cy.ts`:

```typescript
it('should be accessible', { tags: ['@percy'] }, () => {
  cy.injectAxe()

  // Create combiner and trigger duplicate modal
  workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
  workspacePage.toolbar.combine.click()

  // Modal becomes visible
  workspacePage.duplicateCombinerModal.modal.should('be.visible')

  // Check accessibility
  cy.checkAccessibility(undefined, {
    rules: {
      region: { enabled: false },
      'color-contrast': { enabled: false },
    },
  })

  // Percy snapshot (can handle animations)
  cy.percySnapshot('Workspace - Duplicate Combiner Modal')

  // ✅ Wait for modal slide-in animation to complete by checking opacity
  workspacePage.duplicateCombinerModal.modal.should('have.css', 'opacity', '1')

  // ✅ Screenshot for PR template (last command, after animation complete)
  cy.screenshot('pr-screenshots/after-modal-empty-state', {
    capture: 'viewport',
    overwrite: true,
  })
})
```

### Best Practices for Screenshot Timing

**1. Order of Operations:**

```tsx
// 1. Verify element visibility
cy.getByTestId('modal').should('be.visible')

// 2. Run accessibility checks (don't need fully rendered UI)
cy.checkAccessibility()

// 3. Take Percy snapshot (handles animations automatically)
cy.percySnapshot('My Modal')

// 4. Wait for animation completion
cy.getByTestId('modal').should('have.css', 'opacity', '1')

// 5. Take Cypress screenshot (LAST - needs fully rendered UI)
cy.screenshot('modal-state', { capture: 'viewport', overwrite: true })
```

**2. Why Accessibility Checks Come Before Animation Wait:**

- Accessibility checks test semantic structure, not visual appearance
- No need to wait for full visual rendering
- If accessibility fails, we don't waste time on screenshot
- Screenshots are only taken if all previous checks pass

**3. Screenshot Position:**
Screenshots should **always be the last command** in a test because:

- Only capture if all validations pass
- Ensure fully rendered state
- No wasted screenshots from failed tests
- Clean test output

### When to Use This Pattern

Use CSS property checks before screenshots when:

- ✅ Component uses Chakra UI `motionPreset`
- ✅ Component has CSS transitions/animations
- ✅ Taking screenshots for PR documentation
- ✅ Visual appearance is critical (modals, tooltips, popovers)
- ✅ Percy snapshots show transparency issues

Don't use for:

- ❌ Simple static components without animations
- ❌ Tests that don't take screenshots
- ❌ Components that are already fully rendered

### Troubleshooting

**Screenshot still looks transparent:**

- Check if multiple elements animate (e.g., modal + overlay)
- Verify the correct element selector
- Inspect actual CSS in browser DevTools
- Consider checking multiple properties (opacity + transform)

**Animation takes too long:**

- Default Cypress timeout is 4000ms - should be plenty
- Check if animation is actually completing
- Verify CSS transitions are defined correctly
- Look for JavaScript animation conflicts

### ESLint Compliance

✅ **This pattern avoids ESLint warnings:**

- No `cy.wait()` with numbers
- Uses built-in Cypress retry-ability
- Self-documenting assertions
- Follows Cypress best practices

---

## Checklist for New Component Tests

### axe-core Integration

The `cy.checkAccessibility()` command uses [axe-core](https://github.com/dequelabs/axe-core) to detect:

- Missing ARIA labels
- Insufficient color contrast
- Invalid HTML structure
- Keyboard navigation issues
- Focus management problems
- Screen reader compatibility

### Custom Accessibility Checks

For specific accessibility requirements:

```tsx
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<MyComponent />)

  // Check all accessibility issues
  cy.checkAccessibility()

  // Or check specific rules
  cy.checkAccessibility({
    runOnly:
```
