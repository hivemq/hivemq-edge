# React-Select Testing Patterns

**Last Updated:** November 28, 2025  
**Component:** chakra-react-select (used extensively in HiveMQ Edge Frontend)

---

## Overview

This document provides testing patterns for `chakra-react-select` components, which wrap `react-select` with Chakra UI styling. These patterns were established during task 37937 (MessageTypeSelect testing).

**Key Challenge:** react-select uses complex DOM structures that don't map directly to standard form inputs, making testing non-obvious.

---

## Basic Patterns

### Pattern 1: Opening the Dropdown

```typescript
// Open dropdown by clicking the control
cy.get('#select-id').click()

// Alternative: Click the dropdown indicator
cy.get('[aria-label="Open menu"]').click()
```

### Pattern 2: Verifying Options

```typescript
// After opening dropdown, check options
cy.get('#select-id').click()
cy.get('[role="option"]').should('have.length', 3)
cy.get('[role="option"]').eq(0).should('contain.text', 'Option 1')
cy.get('[role="option"]').eq(1).should('contain.text', 'Option 2')
cy.get('[role="option"]').eq(2).should('contain.text', 'Option 3')
```

### Pattern 3: Selecting an Option

```typescript
// Open and select by clicking
cy.get('#select-id').click()
cy.get('[role="option"]').first().click()

// Or select specific option
cy.get('[role="option"]').contains('My Option').click()
```

### Pattern 4: Testing onChange Handler

```typescript
const onChangeSpy = cy.spy().as('onChangeSpy')

cy.mountWithProviders(<MySelect onChange={onChangeSpy} />)

cy.get('#select-id').click()
cy.get('[role="option"]').first().click()

cy.get('@onChangeSpy').should('have.been.calledOnce')
cy.get('@onChangeSpy').should('have.been.calledWith', expectedValue)
```

---

## Common Pitfalls & Solutions

### ❌ Pitfall 1: Checking Input Value

**Wrong:**

```typescript
cy.get('#select-id').should('have.value', 'Selected Option')
```

**Why it fails:** react-select uses a hidden input or the input's value is empty. The displayed value is in a separate element.

**Right:**

```typescript
// Check that the option appears in the control
cy.get('[class*="react-select"]').should('contain.text', 'Selected Option')

// Or open dropdown and verify options are available
cy.get('#select-id').click()
cy.get('[role="option"]').should('contain.text', 'Selected Option')
```

---

### ❌ Pitfall 2: Checking Displayed Selected Value

**Wrong:**

```typescript
cy.get('#select-id').parent().should('contain.text', 'Selected')
```

**Why it fails:** The parent selector is too broad and may match unrelated elements.

**Right:**

```typescript
// Use class selector for react-select container
cy.get('[class*="react-select"]').should('contain.text', 'Selected')

// Or open dropdown and verify selection by checking onChange was called
const onChangeSpy = cy.spy().as('spy')
cy.mountWithProviders(<MySelect value="Initial" onChange={onChangeSpy} />)
cy.get('#select-id').click()
cy.get('[role="option"]').contains('New Value').click()
cy.get('@spy').should('have.been.calledWith', 'New Value')
```

---

### ❌ Pitfall 3: Testing Placeholder

**Wrong:**

```typescript
cy.get('#select-id').should('have.attr', 'placeholder', 'Select...')
```

**Why it fails:** The placeholder is not an attribute on the input element.

**Right:**

```typescript
// Check the visible placeholder text
cy.get('[class*="react-select"]').should('contain.text', 'Select...')

// Or check that it's visible before opening
cy.contains('Select...').should('be.visible')
```

---

## Advanced Patterns

### Pattern 5: Testing Disabled State

```typescript
// For disabled select
cy.mountWithProviders(<MySelect disabled={true} />)
cy.get('#select-id').should('be.disabled')

// For readonly (same behavior in react-select)
cy.mountWithProviders(<MySelect readonly={true} />)
cy.get('#select-id').should('be.disabled')
```

### Pattern 6: Testing Multi-Select

```typescript
cy.get('#multi-select').click()

// Select multiple options
cy.get('[role="option"]').eq(0).click()
cy.get('[role="option"]').eq(2).click()

// Verify selected values are displayed
cy.get('[class*="multi-value"]').should('have.length', 2)
cy.get('[class*="multi-value"]').eq(0).should('contain.text', 'Option 1')
cy.get('[class*="multi-value"]').eq(1).should('contain.text', 'Option 3')
```

### Pattern 7: Testing Clearable Select

```typescript
// Select a value first
cy.get('#select-id').click()
cy.get('[role="option"]').first().click()

// Click the clear button
cy.get('[aria-label="Clear"]').click()

// Verify onChange called with undefined
cy.get('@onChangeSpy').should('have.been.calledWith', undefined)
```

### Pattern 8: Testing CreatableSelect

```typescript
cy.get('#creatable-select').click()
cy.get('#creatable-select').type('New Custom Value')

// Verify "Create" option appears
cy.get('[role="option"]').should('contain.text', 'Create')
cy.get('[role="option"]').contains('Create').click()

// Verify onChange called with the new value
cy.get('@onChangeSpy').should('have.been.calledWith', 'New Custom Value')
```

### Pattern 9: Testing Search/Filter

```typescript
cy.get('#select-id').click()
cy.get('#select-id').type('search term')

// Options should be filtered
cy.get('[role="option"]').should('have.length', 2) // Only matching options
cy.get('[role="option"]').first().should('contain.text', 'Matching Item')
```

### Pattern 10: Testing Dynamic Options (onMenuOpen)

```typescript
// Component loads options when dropdown opens
cy.mountWithProviders(<MySelect onMenuOpen={loadOptions} />)

// Initially no options
cy.get('#select-id').click()

// Wait for options to load
cy.get('[role="option"]').should('have.length.gt', 0)
cy.get('[role="option"]').first().should('be.visible')
```

---

## Testing with formContext

**Pattern specific to RJSF widgets (like MessageTypeSelect):**

```typescript
// Widget needs access to live data from parent form
const mockProps = {
  id: 'my-select',
  onChange: cy.spy().as('onChange'),
  formContext: {
    // Pass live data via formContext
    currentData: 'some live data from parent form',
  },
}

cy.mountWithProviders(<MyWidget {...mockProps} />)

// Widget can access formContext.currentData
// and update options based on it
```

**Key Discovery:** `WidgetProps` and `FieldProps` in RJSF do NOT have access to full form data. Use `formContext` to pass cross-field dependencies.

---

## Common Selectors

| Element                | Selector                   | Use Case                           |
| ---------------------- | -------------------------- | ---------------------------------- |
| Input                  | `#select-id`               | Clicking to open, typing to filter |
| Options list           | `[role="option"]`          | Selecting options, verifying list  |
| Selected value display | `[class*="react-select"]`  | Checking displayed value           |
| Clear button           | `[aria-label="Clear"]`     | Clearing selection                 |
| Dropdown indicator     | `[aria-label="Open menu"]` | Opening dropdown                   |
| Multi-value tags       | `[class*="multi-value"]`   | Multi-select values                |
| Remove tag button      | `[aria-label="Remove"]`    | Removing multi-select values       |

---

## Accessibility Testing

```typescript
it('should be accessible', () => {
  cy.injectAxe()
  cy.mountWithProviders(<MySelect {...props} />)

  // May need to disable specific rules for react-select
  cy.checkAccessibility(undefined, {
    rules: {
      // react-select doesn't always tag properly
      'aria-input-field-name': { enabled: false },
      'scrollable-region-focusable': { enabled: false },
    },
  })
})
```

---

## Real-World Example: MessageTypeSelect

From task 37937:

```typescript
it('should extract and display multiple message types', () => {
  const mockProps = {
    ...getMockProps(),
    formContext: {
      currentSchemaSource: `
        syntax = "proto3";
        message Person { string name = 1; }
        message Address { string street = 1; }
      `,
    },
  }

  cy.mountWithProviders(<MessageTypeSelect {...mockProps} />)

  // Open dropdown
  cy.get('#messageType').click()

  // Verify both options exist
  cy.get('[role="option"]').should('have.length', 2)
  cy.get('[role="option"]').eq(0).should('contain.text', 'Person')
  cy.get('[role="option"]').eq(1).should('contain.text', 'Address')
})
```

---

## Best Practices

1. **Always open dropdown first** before checking options
2. **Use `[role="option"]`** for option assertions
3. **Use spy for onChange** instead of checking displayed values
4. **Test behavior, not implementation** - verify onChange called correctly
5. **Use class selectors cautiously** - they may change with library updates
6. **Prefer ARIA roles** - more stable than class names

---

## Related Components

This pattern applies to all components using `chakra-react-select`:

- `ResourceNameCreatableSelect`
- `SchemaNameSelect` / `ScriptNameSelect`
- `MessageTypeSelect`
- `FunctionCreatableSelect`
- `TransitionSelect`
- Any other select widgets in the codebase

---

**See Also:**

- [TESTING_GUIDELINES.md](.tasks/TESTING_GUIDELINES.md)
- [CYPRESS_TESTING_GUIDELINES.md](.tasks/CYPRESS_TESTING_GUIDELINES.md)
- Task 37937 - MessageTypeSelect tests as reference implementation
