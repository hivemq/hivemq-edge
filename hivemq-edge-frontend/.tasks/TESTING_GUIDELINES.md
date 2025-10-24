# HiveMQ Edge Frontend - Testing Guidelines

**Last Updated:** October 24, 2025

---

## Table of Contents

1. [Accessibility Testing](#accessibility-testing)
2. [Component Testing Patterns](#component-testing-patterns)
3. [Screenshot Documentation](#screenshot-documentation)
4. [Test Naming Conventions](#test-naming-conventions)
5. [Dynamic IDs and Partial Selectors](#dynamic-ids-and-partial-selectors)
6. [Page Object Linking](#page-object-linking)

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

#### Key Requirements

1. **Test Name:** MUST be exactly `"should be accessible"`
2. **Order:** Should typically be the last test in the describe block
3. **Setup:** Must call `cy.injectAxe()` before mounting
4. **Props:** Use meaningful, representative props that show the component's real usage
5. **Interactions:** Optionally test accessibility during/after user interactions
6. **Screenshot:** Optionally capture a screenshot for PR documentation

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
    runOnly: {
      type: 'tag',
      values: ['wcag2a', 'wcag2aa'],
    },
  })

  // Or exclude certain rules if necessary (document why!)
  cy.checkAccessibility({
    rules: {
      'color-contrast': { enabled: false }, // Explain: Using brand colors, verified manually
    },
  })
})
```

---

## Benefits of This Approach

1. **Consistency**: Every component has accessibility verification
2. **Early Detection**: Catch accessibility issues during development
3. **Documentation**: Screenshots provide visual references
4. **Compliance**: Helps meet WCAG standards
5. **Maintainability**: Easy to find and update accessibility tests
6. **PR Review**: Visual documentation aids code review

---

## Related Guidelines

- [Design Guidelines](./.tasks/DESIGN_GUIDELINES.md) - Button variants and UI patterns
- [Task Documentation](./.tasks/README.md) - Development workflow

---

## References

- Task 33168 - Duplicate Combiner Modal (example implementation)
- [axe-core Documentation](https://github.com/dequelabs/axe-core)
- [Cypress Accessibility Testing](https://docs.cypress.io/guides/testing-strategies/accessibility-testing)
- [WCAG Guidelines](https://www.w3.org/WAI/WCAG21/quickref/)

---

## Dynamic IDs and Partial Selectors

### Problem: UUID-Based TestIds

When components generate testIds with dynamic UUIDs (e.g., `mapping-item-{uuid}`), you cannot use exact testId matches in your tests. This is common for list items, dynamically created elements, or any component that uses generated IDs.

#### ❌ Wrong - Attempting to Use Unknown UUID

```typescript
// This will fail because the UUID is generated at runtime
cy.getByTestId('mapping-item-abc-123-def-456')
cy.getByTestId('mapping-item-0') // Index doesn't work either
```

#### ✅ Correct - Use Attribute Starts-With Selector

```typescript
// Use CSS attribute selector to match testIds that start with a pattern
cy.get('[data-testid^="mapping-item-"]').first()
cy.get('[data-testid^="mapping-item-"]').eq(0) // First item
cy.get('[data-testid^="mapping-item-"]').eq(1) // Second item
cy.get('[data-testid^="mapping-item-"]').should('have.length', 3) // Count items
```

### When to Use Partial Selectors

Use attribute starts-with selectors (`^=`) when:

- **Dynamic IDs**: UUIDs, timestamps, or any generated values
- **List items**: When you need to select by position/index
- **Multiple instances**: When the exact ID is unknowable at test time
- **Generated content**: Items created dynamically by the application

### Common Patterns

#### Selecting First/Last Item

```typescript
// First item
cy.get('[data-testid^="mapping-item-"]').first()

// Last item
cy.get('[data-testid^="mapping-item-"]').last()

// By index
cy.get('[data-testid^="mapping-item-"]').eq(2) // Third item (0-indexed)
```

#### Selecting by Content

```typescript
// Find item containing specific text
cy.get('[data-testid^="mapping-destination-"]').contains('my/destination').should('be.visible')
```

#### Counting Items

```typescript
// Verify number of items
cy.get('[data-testid^="mapping-item-"]').should('have.length', 5)
```

#### Iterating Over Items

```typescript
// Loop through all items
cy.get('[data-testid^="mapping-item-"]').each(($item, index) => {
  cy.wrap($item).should('be.visible')
  cy.wrap($item).find('[data-testid^="mapping-destination-"]').should('exist')
})
```

### Real Example: CombinerMappingsList

From the duplicate combiner modal tests:

```typescript
it('should display existing mappings in modal', () => {
  // ... create combiner with mappings ...

  // Attempt duplicate to show modal
  workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
  workspacePage.toolbar.combine.click()

  // ✅ Verify mappings list container
  workspacePage.duplicateCombinerModal.mappingsList.should('be.visible')

  // ✅ Select first mapping item (UUID is unknown)
  cy.get('[data-testid^="mapping-item-"]').first().should('be.visible')

  // ✅ Verify destination text in first mapping
  cy.get('[data-testid^="mapping-destination-"]').first().should('contain.text', 'my / destination')

  // ✅ Count total mappings
  cy.get('[data-testid^="mapping-item-"]').should('have.length', 1)
})
```

---

## Page Object Linking

### Why Link Page Objects to Components

Page Objects should include JSDoc comments that link directly to the source components they represent. This creates a direct path from test code to implementation code.

**Benefits:**

- ✅ **IDE Navigation**: Ctrl+Click (or Cmd+Click) to jump directly to the component
- ✅ **Clear Source of Truth**: No guessing where testIds are defined
- ✅ **Easier Maintenance**: When components change, you know which tests to update
- ✅ **Documentation**: New team members understand the relationship immediately
- ✅ **Refactoring Safety**: Find all usages when renaming components or testIds

### Required Pattern

**Step 1: Import the component types** (even if unused, suppress ESLint warning)

```typescript
// eslint-disable-next-line @typescript-eslint/no-unused-vars
import { DuplicateCombinerModal, CombinerMappingsList } from '@/modules/Workspace/components/modals'
```

**Step 2: Use `@see ComponentName` in JSDoc** (WebStorm will link automatically)

```typescript
/**
 * Page Object for the Duplicate Combiner Detection Modal
 * @see DuplicateCombinerModal
 * @see CombinerMappingsList
 */
duplicateCombinerModal = {
  /**
   * Container for the list of combiner mappings
   * @see CombinerMappingsList
   * @note Use `cy.get('[data-testid^="mapping-item-"]')` to select individual mappings with dynamic UUIDs
   */
  get mappingsList() {
    return cy.getByTestId('mappings-list')
  },

  /**
   * Empty state message when no mappings exist
   * @see CombinerMappingsList
   */
  get mappingsListEmpty() {
    return cy.getByTestId('mappings-list-empty')
  },
}
```

### What to Include

1. **Component-level JSDoc**: At the top of the page object group
2. **Property-level JSDoc**: For each getter
3. **@see Links**: Path to the source component file
4. **@note Annotations**: Special instructions (e.g., dynamic IDs, partial selectors)
5. **Multiple Links**: If the page object spans multiple components

### Real-World Example

```typescript
export class WorkspacePage extends ShellPage {
  /**
   * Page Object for the Duplicate Combiner Detection Modal
   * @see {@link src/modules/Workspace/components/modals/DuplicateCombinerModal.tsx}
   * @see {@link src/modules/Workspace/components/modals/CombinerMappingsList.tsx}
   */
  duplicateCombinerModal = {
    /**
     * Main modal container
     * @see {@link src/modules/Workspace/components/modals/DuplicateCombinerModal.tsx}
     */
    get modal() {
      return cy.getByTestId('duplicate-combiner-modal')
    },

    /**
     * Container for the list of combiner mappings
     * @see {@link src/modules/Workspace/components/modals/CombinerMappingsList.tsx}
     * @note Use `cy.get('[data-testid^="mapping-item-"]')` to select individual mappings with dynamic UUIDs
     */
    get mappingsList() {
      return cy.getByTestId('mappings-list')
    },

    /**
     * Action buttons in modal footer
     * @see {@link src/modules/Workspace/components/modals/DuplicateCombinerModal.tsx}
     */
    buttons: {
      /**
       * Cancel button - closes modal without action
       */
      get cancel() {
        return cy.getByTestId('modal-button-cancel')
      },

      /**
       * Use Existing button - navigates to existing combiner (primary action)
       */
      get useExisting() {
        return cy.getByTestId('modal-button-use-existing')
      },
    },
  }
}
```

### Maintenance Workflow

When updating a component:

1. **Component Change**: Update testId in component file
2. **Find Usages**: IDE shows all page objects that reference it via `@see` link
3. **Update Page Object**: Update the testId and JSDoc if needed
4. **Update Tests**: Tests using the page object are automatically correct

### Checklist for Page Objects

- [ ] Component-level JSDoc with `@see` link
- [ ] Property-level JSDoc for each getter
- [ ] `@see` links point to correct component file paths
- [ ] `@note` annotations for special cases (dynamic IDs, partial selectors)
- [ ] Links verified with Ctrl+Click navigation in IDE
- [ ] Comments describe the element's purpose, not just its testId

---

## Cypress Aliases for Dynamic Data

### Problem: Managing Dynamic IDs Across Test Steps

When tests create resources with dynamic IDs (e.g., combiners, mappings), you need to track those IDs to use them in later assertions or interactions.

#### ❌ Wrong - Using Constants or Arrays

```typescript
// BAD: Fixed constant doesn't work for multiple items
const COMBINER_ID = 'combiner-123'

// BAD: Arrays are difficult to manage in Cypress
const createdIds: string[] = []
cy.intercept('POST', '/api/combiners', (req) => {
  createdIds.push(req.body.id) // Hard to clean up between tests
})
```

#### ✅ Correct - Use Cypress Aliases

```typescript
// GOOD: Store dynamic ID in Cypress alias
cy.intercept('POST', '/api/v1/management/combiners', (req) => {
  req.continue((res) => {
    const combiner = res.body as Combiner
    cy.wrap(combiner.id).as('firstCombinerId') // ✅ Store in alias
  })
}).as('postCombiner')

// Later: Retrieve and use the alias
cy.get('@firstCombinerId').then((combinerId) => {
  cy.getByTestId(`combiner-node-${combinerId}`).should('be.visible')
})
```

### Why Aliases Are Better

- ✅ **Automatic Cleanup**: Aliases are reset between tests
- ✅ **Idiomatic Cypress**: Standard Cypress pattern
- ✅ **Type-Safe**: Works with `.then()` for proper typing
- ✅ **No Manual State**: No arrays or variables to manage
- ✅ **Chainable**: Integrates with Cypress command chain

### Common Patterns

#### Storing Multiple IDs

```typescript
let isFirstPost = true

cy.intercept('POST', '/api/v1/management/combiners', (req) => {
  req.continue((res) => {
    const combiner = res.body as Combiner
    if (isFirstPost) {
      cy.wrap(combiner.id).as('firstCombinerId')
      isFirstPost = false
    } else {
      cy.wrap(combiner.id).as('secondCombinerId')
    }
  })
}).as('postCombiner')

// Use both IDs later
cy.get('@firstCombinerId').then((id1) => {
  cy.getByTestId(`combiner-${id1}`).should('be.visible')
})

cy.get('@secondCombinerId').then((id2) => {
  cy.getByTestId(`combiner-${id2}`).should('be.visible')
})
```

#### Using in Assertions

```typescript
// Store the ID
cy.intercept('POST', '/api/combiners', (req) => {
  req.continue((res) => {
    cy.wrap(res.body.id).as('combinerId')
  })
})

// Use in URL assertion
cy.get('@combinerId').then((id) => {
  cy.url().should('include', `/combiners/${id}`)
})

// Use in element assertion
cy.get('@combinerId').then((id) => {
  cy.getByTestId(`combiner-item-${id}`).should('contain.text', 'My Combiner')
})
```

### Real Example from Duplicate Combiner Tests

```typescript
it('should navigate to existing combiner when "Use Existing" is clicked', () => {
  // Store the combiner ID when created
  cy.intercept('POST', '/api/v1/management/combiners', (req) => {
    req.continue((res) => {
      const combiner = res.body as Combiner
      cy.wrap(combiner.id).as('combinerId')
    })
  }).as('postCombiner')

  // Create first combiner
  workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
  workspacePage.toolbar.combine.click()
  cy.wait('@postCombiner')

  // Attempt duplicate
  workspacePage.act.selectReactFlowNodes(['opcua-pump', 'opcua-boiler'])
  workspacePage.toolbar.combine.click()

  // Click "Use Existing"
  workspacePage.duplicateCombinerModal.buttons.useExisting.click()

  // Verify navigation to the stored combiner ID
  cy.get('@combinerId').then((id) => {
    cy.url().should('include', id as string)
  })
})
```

### Key Takeaways

1. **Always use aliases** for dynamic IDs generated during tests
2. **Use `cy.wrap().as()`** to store values in intercepts
3. **Use `cy.get('@alias').then()`** to retrieve and use values
4. **Name aliases clearly**: `firstCombinerId`, `newMappingId`, etc.
5. **Don't use arrays or external variables** - they don't clean up properly

---
