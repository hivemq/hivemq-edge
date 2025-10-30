# Cypress Testing Guidelines for HiveMQ Edge Frontend

## Critical Rules

### 1. **Always Use Grep for Test Execution** ⚠️

**NEVER** run all Cypress tests at once - there are too many and it takes too long.

**Always use grep to select specific tests:**

```bash
# Component tests - specific file
npm run cypress:run:component -- --spec "src/path/to/Component.spec.cy.tsx"

# Component tests - specific directory
npm run cypress:run:component -- --spec "src/modules/Workspace/components/layout/**/*.spec.cy.tsx"

# E2E tests - specific feature
npm run cypress:run:e2e -- --spec "cypress/e2e/workspace/workspace-layout-*.cy.ts"

# Using grep tags
npm run cypress:run:component -- --env grep="layout"
npm run cypress:run:e2e -- --env grep="@workspace"
```

### 2. **Avoid `cy.contains().should("be.visible")`** ⚠️

**Problem:** `cy.contains()` can match multiple elements or substrings, leading to flaky tests.

**❌ Bad:**

```typescript
cy.contains('Save').should('be.visible') // Might match multiple buttons
cy.contains('Layout').click() // Could match partial text
```

**✅ Good:**

```typescript
// Use data-testid
cy.getByTestId('save-button').should('be.visible')
cy.getByTestId('save-button').should('have.text', 'Save Layout')

// Use specific selectors with assertion
cy.get('[role="dialog"]').within(() => {
  cy.get('button').contains('Save').should('have.text', 'Save')
})

// Use aria-label for accessibility
cy.get('button[aria-label="Save layout"]').click()
```

## Best Practices

### Selector Hierarchy (in order of preference)

1. **data-testid** - Most stable, semantic

   ```typescript
   cy.getByTestId('workspace-layout-selector')
   ```

2. **ARIA attributes** - Good for accessibility testing

   ```typescript
   cy.get('button[aria-label="Layout options"]')
   cy.get('[role="dialog"]')
   ```

3. **Semantic selectors** - Use roles and tags

   ```typescript
   cy.get('[role="menu"]').within(() => {
     cy.get('[role="menuitem"]').first()
   })
   ```

4. **Class/ID** - Last resort, avoid if possible
   ```typescript
   cy.get('.chakra-button') // Fragile, avoid
   ```

### Component Test Structure

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
        // Add exceptions for known issues
        region: { enabled: false },
        'color-contrast': { enabled: false },
      },
    })
  })
})
```

### Assertions

**Prefer specific assertions over generic ones:**

```typescript
// ❌ Too generic
cy.get('button').should('exist')

// ✅ Specific and clear
cy.getByTestId('apply-layout').should('be.visible')
cy.getByTestId('apply-layout').should('have.text', 'Apply Layout')
cy.getByTestId('apply-layout').should('not.be.disabled')
```

### Avoid Flaky Tests

**Don't test UI feedback that's timing-dependent:**

```typescript
// ❌ Flaky - tooltips have timing issues
cy.getByTestId('button').trigger('mouseenter')
cy.get('[role="tooltip"]').should('be.visible')

// ❌ Flaky - toasts may not appear in test environment
cy.get('[role="alert"]').should('contain.text', 'Success')

// ✅ Test actual behavior instead
cy.window().then(() => {
  const state = useWorkspaceStore.getState()
  expect(state.layoutConfig.presets).to.have.length(1)
})
```

**Never use `cy.wait()` with arbitrary timeouts:**

```typescript
// ❌ Bad - arbitrary wait
workspacePage.layoutControls.applyButton.click()
cy.wait(1000) // Don't do this!
workspacePage.edgeNode.should('be.visible')

// ✅ Good - rely on Cypress's automatic retry
workspacePage.layoutControls.applyButton.click()
workspacePage.edgeNode.should('be.visible') // Cypress retries automatically

// ❌ Bad - wait for drawer animation
workspacePage.layoutControls.optionsButton.click()
cy.wait(500)
cy.percySnapshot('Drawer')

// ✅ Good - wait for element to be visible
workspacePage.layoutControls.optionsButton.click()
workspacePage.layoutControls.optionsDrawer.drawer.should('be.visible')
cy.percySnapshot('Drawer')
```

**The only acceptable use of `cy.wait()` is for network requests:**

```typescript
// ✅ Acceptable - waiting for network request
cy.wait('@getAdapters')
cy.wait('@getBridges')
```

### Working with Menus

```typescript
// Open menu
cy.get('button[aria-label*="preset"]').first().click()

// Select within menu
cy.get('[role="menu"]').within(() => {
  cy.get('[role="menuitem"]').first().click()
})

// Avoid ambiguous contains
// ❌ Bad
cy.contains('Delete').click() // Multiple delete buttons?

// ✅ Good
cy.get('[role="menu"]').within(() => {
  cy.get('button[aria-label*="Delete"]').first().click()
})
```

### Working with Modals/Drawers

```typescript
// Open modal
cy.getByTestId('open-modal-button').click()

// Assert modal opened
cy.get('[role="dialog"]').should('be.visible')

// Work within modal scope
cy.get('[role="dialog"]').within(() => {
  cy.get('input[type="text"]').type('My Input')
  cy.get('button').contains('Save').click()
})

// Assert modal closed
cy.get('[role="dialog"]').should('not.exist')
```

### Store Testing

```typescript
// Test store state changes
cy.window().then(() => {
  const state = useWorkspaceStore.getState()
  expect(state.layoutConfig.currentAlgorithm).to.equal(LayoutType.DAGRE_TB)
  expect(state.nodes).to.have.length(2)
})

// Setup store before mount
const wrapper = ({ children }: { children: React.ReactNode }) => {
  const store = useWorkspaceStore.getState()
  store.setLayoutAlgorithm(LayoutType.DAGRE_TB)
  store.onAddNodes([/* nodes */])

  return <Provider>{children}</Provider>
}
```

## Accessibility Testing

**Always include accessibility tests:**

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<Component />, { wrapper })

  cy.checkAccessibility(undefined, {
    rules: {
      // Known issues from React Flow
      region: { enabled: false },

      // Chakra UI contrast issues
      'color-contrast': { enabled: false },
    },
  })
})
```

## TypeScript Best Practices

```typescript
// ✅ Always type your test data
const testNodes: Node[] = [
  { id: '1', type: 'adapter', position: { x: 0, y: 0 }, data: {} },
]

const testPreset: LayoutPreset = {
  id: 'test-1',
  name: 'Test Preset',
  // ...all required fields
}

// ✅ Type your wrappers
const wrapper = ({ children }: { children: React.ReactNode }) => (
  <Provider>{children}</Provider>
)

// ❌ Avoid any
const data: any = {}  // Don't do this
```

## Running Tests

### Development

```bash
# Run specific component tests during development
npm run cypress:open:component

# Then use Cypress UI to select specific tests
```

### CI/Pipeline

```bash
# Run only layout-related component tests
npm run cypress:run:component -- --spec "src/modules/Workspace/components/layout/**/*.spec.cy.tsx"

# Run specific E2E tests
npm run cypress:run:e2e -- --spec "cypress/e2e/workspace/workspace-layout-*.cy.ts"
```

## Common Pitfalls

### 1. Cypress Caching

If tests behave unexpectedly, clear Cypress cache:

```bash
rm -rf node_modules/.cache/cypress
```

### 2. Multiple Elements

Always use `.first()`, `.eq()`, or `.within()` when dealing with potentially multiple elements:

```typescript
// ❌ Fails if multiple buttons
cy.get('button[aria-label="Delete"]').click()

// ✅ Explicit selection
cy.get('button[aria-label="Delete"]').first().click()

// ✅ Better - scope it
cy.get('[role="menu"]').within(() => {
  cy.get('button[aria-label="Delete"]').first().click()
})
```

### 3. Store State Management

Always reset store in `beforeEach`:

```typescript
beforeEach(() => {
  useWorkspaceStore.getState().reset()
})
```

### 4. Viewport Sizing

Set appropriate viewport for your component:

```typescript
beforeEach(() => {
  cy.viewport(800, 600) // Adjust based on component needs
})
```

## Examples

See these files for reference:

- `src/modules/Workspace/components/layout/LayoutOptionsDrawer.spec.cy.tsx` - ✅ Well-structured component tests
- `src/modules/Workspace/components/layout/LayoutPresetsManager.spec.cy.tsx` - ✅ Menu and modal testing
- `src/modules/Workspace/components/layout/ApplyLayoutButton.spec.cy.tsx` - ✅ Store integration testing

---

**Remember:** Write tests that are reliable, maintainable, and focused on actual behavior, not implementation details!
