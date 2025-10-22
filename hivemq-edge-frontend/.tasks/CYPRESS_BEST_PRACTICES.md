# Cypress Testing Best Practices

This document outlines best practices and common pitfalls when writing Cypress tests, based on ESLint plugin rules and real-world experience.

## Table of Contents

1. [Command Chaining Rules](#command-chaining-rules)
2. [Timing and Waits](#timing-and-waits)
3. [Assertions](#assertions)
4. [Test Organization](#test-organization)
5. [Monaco Editor Testing](#monaco-editor-testing)
6. [Linting](#linting)

---

## Command Chaining Rules

### ⚠️ Rule: `cypress/unsafe-to-chain-command`

**Never chain commands after action commands** like `.click()`, `.type()`, `.select()`, etc. These commands don't reliably return a subject for chaining.

#### ❌ BAD - Unsafe Chaining

```typescript
// Chaining after .click() is unsafe
cy.get('#button').click().should('be.visible')

// Chaining after .type() is unsafe
cy.get('input').type('text').should('have.value', 'text')

// Chaining after .select() is unsafe
cy.get('select').select('option').should('have.value', 'option')
```

#### ✅ GOOD - Break the Chain

```typescript
// Break chain after action command
cy.get('#button').click()
cy.get('#button').should('be.visible')

// Or use separate assertions
cy.get('input').type('text')
cy.get('input').should('have.value', 'text')

// Multi-step interactions
cy.get('#dropdown').click()
cy.get('[role="option"]').first().click()
cy.get('#dropdown').should('contain.text', 'Selected Option')
```

#### Why This Matters

Action commands like `.click()` return `undefined` or may not return the original element, causing subsequent commands in the chain to fail or behave unpredictably.

---

## Timing and Waits

### ⚠️ Rule: `cypress/no-unnecessary-waiting`

**Never use `cy.wait()` with arbitrary time periods.** Always wait for actual conditions using assertions.

#### ❌ BAD - Arbitrary Waits

```typescript
// Slow and unreliable
cy.wait(1000)
cy.get('.loaded').should('be.visible')

// Might be too short or too long
cy.wait(500)
```

#### ✅ GOOD - Wait for Conditions

```typescript
// Cypress automatically retries until assertion passes
cy.get('.loaded').should('be.visible')

// Wait for specific DOM state
cy.get('.monaco-editor').should('be.visible')
cy.get('.monaco-editor .view-lines').should('exist')

// Wait for network request
cy.intercept('/api/data').as('getData')
cy.get('button').click()
cy.wait('@getData') // ✅ This is OK - waiting for network alias
cy.get('.result').should('contain', 'Success')

// Wait with custom timeout
cy.get('.slow-element', { timeout: 10000 }).should('be.visible')
```

#### Benefits of Assertion-Based Waiting

- ✅ **Faster**: No unnecessary delays
- ✅ **More reliable**: Waits exactly as long as needed
- ✅ **Self-documenting**: Clear what you're waiting for
- ✅ **ESLint compliant**: No warnings

---

## Assertions

### Best Practices for Assertions

#### Use Specific Assertions

```typescript
// ❌ BAD - Vague
cy.get('#status').should('exist')

// ✅ GOOD - Specific
cy.get('#status').should('contain.text', 'Active')
cy.get('#status').should('have.class', 'status-active')
```

#### Chain Multiple Assertions Safely

```typescript
// ✅ Multiple assertions on same element
cy.get('input').should('be.visible').should('have.value', 'test').should('not.be.disabled')

// ❌ Don't chain after actions
cy.get('button').click().should('be.visible') // Unsafe!

// ✅ Break chain after action
cy.get('button').click()
cy.get('button').should('be.visible')
```

#### Use `.should()` with Callbacks for Complex Checks

```typescript
// Check multiple conditions
cy.get('.items').should(($items) => {
  expect($items).to.have.length.greaterThan(0)
  expect($items.first()).to.contain('First Item')
})

// Check window/document properties
cy.window().should((win) => {
  expect(win.monaco).to.exist
  expect(win.monaco.editor).to.be.a('object')
})
```

---

## Test Organization

### Structure Your Tests

```typescript
describe('Feature Name', () => {
  beforeEach(() => {
    // Common setup for all tests
    cy.intercept('/api/**', { fixture: 'data.json' })
    cy.mountWithProviders(<Component />, { wrapper })
  })

  it('should handle the happy path', () => {
    // Arrange - setup is in beforeEach

    // Act
    cy.get('button').click()

    // Assert
    cy.get('.result').should('be.visible')
  })

  it('should handle error states', () => {
    // Each test is independent
    cy.intercept('/api/**', { statusCode: 500 })
    cy.get('button').click()
    cy.get('[role="alert"]').should('contain', 'Error')
  })
})
```

### Keep Tests Focused

```typescript
// ❌ BAD - Testing too much
it('should create, edit, and delete an item', () => {
  // Too many steps, hard to debug
})

// ✅ GOOD - Focused tests
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
  cy.get('#confirm').click()
  cy.get('#item-list').should('not.contain', 'Item')
})
```

---

## Monaco Editor Testing

Monaco Editor requires special handling in Cypress tests. See [MONACO_TESTING_GUIDE.md](./MONACO_TESTING_GUIDE.md) for complete details.

### Quick Reference

```typescript
// ✅ Wait for Monaco to load
cy.get('.monaco-editor').should('be.visible')
cy.get('.monaco-editor .view-lines').should('exist')

// ✅ Test component behavior, not Monaco internals
cy.get('#schema-type').click()
cy.get('[role="option"]').contains('PROTOBUF').click()
cy.get('.monaco-editor').should('be.visible')

// ❌ Don't try to .type() into Monaco
cy.get('.monaco-editor').type('code') // Won't work!

// ✅ Use custom commands if you need to manipulate content
cy.get('#editor').setMonacoEditorValue('{"title": "Test"}')
```

---

## Linting

### Run Linters After Making Changes

Always run linters before committing to catch issues early:

```bash
# Run ESLint
pnpm lint:eslint

# Run Prettier
pnpm lint:prettier

# Run all linters
pnpm lint:all

# Auto-fix issues
pnpm lint:eslint:fix
pnpm lint:prettier:write
```

### Common ESLint Rules for Cypress

#### `cypress/unsafe-to-chain-command`

Don't chain after action commands like `.click()`, `.type()`, `.select()`

```typescript
// ❌ BAD
cy.get('button').click().should('be.visible')

// ✅ GOOD
cy.get('button').click()
cy.get('button').should('be.visible')
```

#### `cypress/no-unnecessary-waiting`

Don't use `cy.wait()` with numbers, use assertions instead

```typescript
// ❌ BAD
cy.wait(1000)

// ✅ GOOD
cy.get('.element').should('be.visible')

// ✅ EXCEPTION: Waiting for network requests is OK
cy.wait('@apiRequest')
```

#### `cypress/no-assigning-return-values`

Don't assign Cypress command return values

```typescript
// ❌ BAD
const button = cy.get('button')
button.click()

// ✅ GOOD
cy.get('button').click()

// ✅ GOOD: Use aliases instead
cy.get('button').as('submitButton')
cy.get('@submitButton').click()
```

#### `cypress/assertion-before-screenshot`

Always assert element state before taking screenshots

```typescript
// ❌ BAD
cy.screenshot()

// ✅ GOOD
cy.get('.modal').should('be.visible')
cy.screenshot()
```

---

## Common Patterns

### Interacting with Selects/Dropdowns

```typescript
// ✅ Chakra UI / React Select pattern
cy.get('#dropdown').click()
cy.get('[role="option"]').contains('Option 1').click()
cy.get('#dropdown').should('contain.text', 'Option 1')

// ✅ Native select
cy.get('select').select('option1')
cy.get('select').should('have.value', 'option1')
```

### Working with Forms

```typescript
// ✅ Fill and submit
cy.get('input[name="email"]').type('user@example.com')
cy.get('input[name="password"]').type('password123')
cy.get('button[type="submit"]').click()

// Verify submission
cy.get('.success-message').should('be.visible')

// ❌ Don't chain after type
cy.get('input').type('text').should('have.value', 'text') // Unsafe!

// ✅ Break the chain
cy.get('input').type('text')
cy.get('input').should('have.value', 'text')
```

### Conditional Testing

```typescript
// ✅ Use .should() with callback for conditional logic
cy.get('body').should(($body) => {
  if ($body.find('.modal').length > 0) {
    cy.get('.modal-close').click()
  }
})

// ✅ Or use .then()
cy.get('body').then(($body) => {
  if ($body.find('.error').length > 0) {
    cy.get('.error-dismiss').click()
  }
})
```

---

## Quick Checklist

Before committing your Cypress tests:

- [ ] No `cy.wait()` with numbers (use assertions or network aliases)
- [ ] No chaining after `.click()`, `.type()`, or other actions
- [ ] All assertions are specific and meaningful
- [ ] Tests are focused and independent
- [ ] Run `pnpm lint:eslint` - no errors
- [ ] Run `pnpm lint:prettier` - code is formatted
- [ ] Tests pass consistently: `pnpm cypress:run:component`

---

## Resources

- [Cypress Best Practices (Official)](https://docs.cypress.io/guides/references/best-practices)
- [Cypress ESLint Plugin Rules](https://github.com/cypress-io/eslint-plugin-cypress)
- [Monaco Editor Testing Guide](./MONACO_TESTING_GUIDE.md)
- [Cypress Retry-ability](https://docs.cypress.io/guides/core-concepts/retry-ability)

---

## Examples from This Codebase

### Good Test Example

```typescript
it('should create a draft schema', () => {
  cy.intercept('/api/v1/data-hub/schemas', { items: [mockSchema] })
  cy.mountWithProviders(<SchemaPanel selectedNode="3" />, { wrapper })

  // Verify initial state
  cy.get('#root_name-label + div').should('contain.text', 'Select...')
  cy.get('#root_type-label + div').should('contain.text', 'JSON')

  // Create a draft schema - note: no chaining after .click()
  cy.get('#root_name-label + div').click()
  cy.get('#root_name-label + div').type('new-schema')
  cy.get('#root_name-label + div').find('[role="option"]').first().click()

  // Verify draft schema state
  cy.get('#root_name-label + div').should('contain.text', 'new-schema')
  cy.get('#root_type-label + div').should('contain.text', 'JSON')
  cy.get('#root_version-label + div').should('contain.text', 'DRAFT')

  // Wait for Monaco without cy.wait()
  cy.get('#root_schemaSource').find('.monaco-editor').should('be.visible')
})
```

This test demonstrates:

- ✅ No arbitrary waits
- ✅ No unsafe command chaining
- ✅ Clear, specific assertions
- ✅ Waiting for actual conditions
- ✅ Focused on a single behavior
