# Monaco Editor Testing Guide for Cypress

Monaco Editor is challenging to test in Cypress because it manages its own DOM and doesn't respond well to standard Cypress commands like `.type()`. Here are three approaches to test Monaco Editor in your Cypress tests:

## ⚠️ Important: Avoid Arbitrary Waits

**Never use `cy.wait()` with arbitrary time periods** - it makes tests slow and flaky. Instead, use Cypress's built-in retry mechanism with assertions:

```typescript
// ❌ BAD - Arbitrary wait
cy.wait(1000)
cy.get('.monaco-editor').should('be.visible')

// ✅ GOOD - Wait for actual condition
cy.get('.monaco-editor').should('be.visible')
cy.get('.monaco-editor .view-lines').should('exist')
```

Cypress automatically retries assertions until they pass (up to the default timeout), making tests more reliable and faster.

## Approach 1: Use Custom Commands (Recommended for Simple Cases)

```typescript
// Wait for Monaco to load properly using assertions, not arbitrary waits
cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
cy.get('#root_schemaSource').find('.monaco-editor .view-lines').should('exist')

// Set value
cy.get('#root_schemaSource').setMonacoEditorValue('{"title": "Modified"}')

// Verify value
cy.get('#root_schemaSource').getMonacoEditorValue().should('contain', 'Modified')
```

## Approach 2: Test UI State Changes (Recommended for Most Cases)

Instead of manipulating Monaco directly, test the form state transitions:

```typescript
it('should control the editing flow', () => {
  // Test 1: Create a draft schema
  cy.get('#root_name-label + div').click()
  cy.get('#root_name-label + div').type('new-schema')
  cy.get('[role="option"]').first().click()

  cy.get('#root_version-label + div').should('contain.text', 'DRAFT')

  // Wait for Monaco using assertions, not cy.wait()
  cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')

  // Test 2: Switch to existing schema
  cy.get('#root_name-label + div').click()
  cy.get('#root_name-label + div').type('existing-schema')
  cy.get('[role="option"]').first().click()

  cy.get('#root_version-label + div').should('contain.text', '1')
  cy.get('#root_type-label + div input').should('be.disabled')

  // Monaco should show the loaded content (verify by checking it exists)
  cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
})
```

## Approach 3: Test Business Logic with E2E Tests

For testing actual Monaco editing and the MODIFIED state, use E2E tests instead:

```typescript
// In E2E test
cy.visit('/your-page')
cy.get('#root_schemaSource').find('.monaco-editor').click()
cy.focused().type('{ctrl+a}') // Select all
cy.focused().type('new content') // Type new content
cy.get('#root_version-label + div').should('contain.text', 'MODIFIED')
```

## Why Monaco is Difficult to Test

1. **Virtual DOM**: Monaco renders a virtual DOM that doesn't respond to standard DOM events
2. **Web Workers**: Monaco uses web workers for syntax highlighting and validation
3. **CDN Loading**: Monaco loads from CDN, which can cause timing issues
4. **Internal State**: Monaco maintains its own internal state separate from React

## Best Practice: Component Tests vs E2E Tests

### Component Tests (Cypress Component)

- ✅ Test form state transitions
- ✅ Test schema selection/loading
- ✅ Test validation rules
- ✅ Test readonly states
- ✅ Verify Monaco renders
- ❌ Avoid testing Monaco editing directly

### E2E Tests (Cypress E2E)

- ✅ Test full user workflows including Monaco editing
- ✅ Test the MODIFIED state after editing
- ✅ Test form submission with edited content

## Example: Comprehensive Test Without Monaco Manipulation

```typescript
it('should handle schema lifecycle', () => {
  cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchema] })
  cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

  // Test initial state
  cy.get('#root_name-label + div').should('contain.text', 'Select...')
  cy.get('#root_type-label + div').should('contain.text', 'JSON')

  // Create draft
  cy.get('#root_name-label + div').click().type('new-schema')
  cy.get('[role="option"]').first().click()
  cy.get('#root_version-label + div').should('contain.text', 'DRAFT')

  // Verify Monaco rendered - use assertions, not cy.wait()
  cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
  cy.get('#root_schemaSource').find('.monaco-editor .view-lines').should('exist')

  // Switch to existing schema
  cy.get('#root_name-label + div').click().clear().type('existing')
  cy.get('[role="option"]').first().click()
  cy.get('#root_version-label + div').should('contain.text', '1')

  // Verify type is disabled for existing schema
  cy.get('#root_type-label + div input').should('be.disabled')

  // Verify version selector is enabled
  cy.get('#root_version-label + div input').should('not.be.disabled')

  // Test version switching
  cy.get('#root_version-label + div').click()
  cy.get('#root_version-label + div [role="option"]').should('exist')
})
```

## Troubleshooting

### Monaco not found error

```typescript
// ❌ BAD - Using arbitrary wait
cy.wait(1000)

// ✅ GOOD - Wait for Monaco to be available using window check
cy.window().should((win) => {
  expect(win.monaco).to.exist
  expect(win.monaco.editor).to.exist
})

// Or even better - wait for the actual DOM element
cy.get('.monaco-editor').should('be.visible')
cy.get('.monaco-editor .view-lines').should('exist')
```

### Content not updating

```typescript
// ❌ BAD - Arbitrary wait
cy.wait(500)
cy.get('.monaco-editor').should('be.visible')

// ✅ GOOD - Wait for specific element that indicates Monaco is ready
cy.get('.monaco-editor').should('be.visible')
cy.get('.monaco-editor .view-lines').should('exist')
cy.get('.monaco-editor .view-line').should('have.length.gt', 0)
```

### Test is flaky

```typescript
// ❌ BAD - Using cy.wait() to stabilize
cy.wait(1000)

// ✅ GOOD - Increase timeout and wait for actual condition
cy.get('#root_schemaSource', { timeout: 10000 })
  .find('.monaco-editor')
  .should('be.visible')
  .find('.view-lines')
  .should('exist')
```

## Key Takeaways

1. **Never use `cy.wait()` with numbers** - always wait for actual conditions
2. **Use assertions** - Cypress will automatically retry until they pass
3. **Wait for DOM elements** - like `.monaco-editor .view-lines` to ensure Monaco is ready
4. **Component tests** = test React logic, not Monaco internals
5. **E2E tests** = test actual user interactions with Monaco

## Recommended Waiting Pattern for Monaco

```typescript
// The most reliable way to ensure Monaco is ready
cy.get('#editor-container').find('.monaco-editor').should('be.visible').find('.view-lines').should('exist')

// Now you can safely interact with Monaco or verify its state
```

This approach:

- ✅ No ESLint warnings
- ✅ Faster tests (no arbitrary delays)
- ✅ More reliable (waits for actual readiness)
- ✅ Self-documenting code (clear what you're waiting for)

---

## Testing Monaco IntelliSense: Integration Tests vs Component Tests

### The Challenge with IntelliSense Testing

Monaco's autocomplete/IntelliSense is particularly difficult to test with real user events in Cypress because:

1. **Hidden textarea**: Monaco uses a hidden `<textarea>` with complex event handling
2. **Non-deterministic timing**: Autocomplete triggers based on internal debouncing and parsing
3. **Virtual rendering**: Suggestion widgets are rendered in a virtual layer
4. **Complex keyboard events**: Monaco intercepts and transforms keyboard events

### Attempted Approach: Real User Events

```typescript
// ❌ This approach is UNRELIABLE in Cypress
it('should show autocomplete when user types', () => {
  cy.get('.monaco-editor').click()
  cy.realType('publish.') // Even with cypress-real-events
  cy.get('.suggest-widget').should('be.visible') // Flaky!
})
```

**Why it fails:**

- Monaco's event handling is too complex for simulated events
- Timing issues: autocomplete may not trigger reliably
- Different behavior in headless mode vs headed mode
- Tests become flaky and hard to maintain

### Recommended Approach: Integration Tests via Monaco API

Instead of simulating user events, test that Monaco is **correctly configured** by using the editor API:

```typescript
describe('Monaco IntelliSense - Integration Tests (Configuration Verification)', () => {
  it('should provide autocomplete for publish parameter with JSDoc', () => {
    const code = `/**
 * @param {Publish} publish
 */
function transform(publish, context) {
  publish.
}`

    cy.mountWithProviders(<JavascriptEditor value={code} />)
    cy.get('.monaco-editor', { timeout: 10000 }).should('be.visible')
    cy.wait(2000) // Allow Monaco to parse and configure

    cy.window().then((win) => {
      const editor = win.monaco.editor.getEditors()[0]

      // Position cursor programmatically
      editor.setPosition({ lineNumber: 5, column: 11 })
      editor.focus()

      // Trigger autocomplete via API
      editor.trigger('test', 'editor.action.triggerSuggest', {})
    })

    // Verify autocomplete appears
    cy.get('.monaco-editor .suggest-widget', { timeout: 5000 }).should('be.visible')
  })
})
```

**What this tests:**

- ✅ Type definitions are loaded correctly
- ✅ JSDoc parsing works
- ✅ IntelliSense configuration is correct
- ✅ Autocomplete widget can appear
- ✅ Suggestions are generated for the types

**What this doesn't test:**

- ❌ Real user keyboard events
- ❌ Natural autocomplete triggering (typing ".")
- ❌ User-perceived behavior in production

### Are These Real Component Tests?

**Honest answer: No, they're integration tests.**

These tests:

- Mount a React component ✅
- But then bypass the component interface ❌
- Manipulate Monaco internals via `window.monaco` ❌
- Test implementation details, not user behavior ❌

**They're better described as:**

- **Configuration verification tests** - ensuring Monaco is set up correctly
- **Integration tests** - testing that our code integrates with Monaco API correctly
- **API contract tests** - verifying the Monaco API behaves as expected

### Recommended Testing Strategy

**1. Integration Tests (Current Approach - Component Test Suite)**

- Test Monaco configuration via API
- Verify type definitions are loaded
- Verify autocomplete can be triggered programmatically
- Fast, reliable, stable

```typescript
// Keep these in CodeEditor.IntelliSense.spec.cy.tsx
describe('Monaco IntelliSense - Integration Tests', () => {
  // Use Monaco API to verify configuration
})
```

**2. E2E Tests (Real User Behavior - E2E Test Suite)**

- Test actual user typing in the full application
- Verify autocomplete appears during real workflow
- Slower, but tests actual user experience

```typescript
// Add to cypress/e2e/datahub/transform-editor.cy.ts
describe('DataHub Transform Editor - User Workflow', () => {
  it('shows autocomplete when editing transform function', () => {
    cy.visit('/datahub/data-policies/new')
    // Navigate to transform editor
    cy.get('.javascript-editor').click()

    // Real user typing (may be flaky, but tests real behavior)
    cy.focused().type('console.')

    // Check autocomplete appears (with generous timeout)
    cy.get('.suggest-widget', { timeout: 10000 }).should('be.visible')
  })
})
```

**3. Manual Testing**

- Final verification that IntelliSense works in real usage
- Cannot be automated reliably

### Summary: Best Practices for Monaco IntelliSense Tests

| Test Type       | What to Test                | How to Test             | Location        |
| --------------- | --------------------------- | ----------------------- | --------------- |
| **Integration** | Monaco configured correctly | Monaco API calls        | Component tests |
| **E2E**         | User can see autocomplete   | Real typing in full app | E2E tests       |
| **Manual**      | Natural user experience     | Human testing           | N/A             |

**For IntelliSense tests:**

1. ✅ Keep current integration tests - they verify the feature is configured
2. ✅ Rename describe blocks to be honest: "Integration Tests (Configuration Verification)"
3. ✅ Add 1-2 E2E tests for real user workflow
4. ✅ Accept that some things need manual verification

**Key Insight:** Integration tests via Monaco API are **good enough** for CI/CD because:

- They catch configuration bugs (missing types, wrong setup)
- They're fast and stable
- They don't break on minor UI changes
- Real user behavior can be verified in E2E tests + manual testing

---

## Recommendation

For your SchemaPanel tests, I recommend:

1. Test the form state logic (schema selection, version changes)
2. Verify Monaco renders correctly using `.should('exist')` assertions
3. Use E2E tests for actual editing workflows
4. Mock or stub onChange handlers if you need to test MODIFIED state in component tests
