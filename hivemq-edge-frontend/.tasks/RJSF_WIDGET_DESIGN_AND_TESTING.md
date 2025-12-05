# RJSF Widget Design & Testing Guidelines

**Last Updated:** November 28, 2025  
**Purpose:** Unified guide for designing RJSF custom widgets with react-select and testing them correctly

---

## 🎯 Critical Pattern: RJSF + React-Select Integration

### The Challenge

When creating RJSF custom widgets using `chakra-react-select` (or `react-select`), there are **strict constraints** that must be followed to ensure:

1. ✅ Proper accessibility (screen readers, keyboard navigation)
2. ✅ RJSF form integration works correctly
3. ✅ Page Object Model (POM) testing patterns work
4. ✅ Component can be tested in isolation

**This pattern was established through task 37937 after extensive debugging of MessageTypeSelect component.**

---

## 📐 Design Requirements (Widget Implementation)

### Required Props Pattern

Assuming the RJSF property name generates `id = "root_propertyName"`:

```typescript
import type { WidgetProps } from '@rjsf/utils'
import { FormControl, FormLabel } from '@chakra-ui/react'
import { CreatableSelect } from 'chakra-react-select'

export const MyCustomWidget: FC<WidgetProps> = (props) => {
  const { id, value, onChange, required, disabled, readonly, schema, rawErrors } = props
  const hasErrors = rawErrors && rawErrors.length > 0

  return (
    <FormControl
      data-testid={id}              // ✅ REQUIRED: Identifies the whole property group
      isDisabled={disabled || readonly}
      isRequired={required}
      isReadOnly={readonly}
      isInvalid={hasErrors}
    >
      <FormLabel
        htmlFor={id}                // ✅ Links label to input for accessibility
        id={`${id}-label`}          // ✅ REQUIRED: PMO selector target
      >
        {schema.title || 'Field Label'}
      </FormLabel>

      <CreatableSelect
        id={`${id}-widget`}         // ✅ REQUIRED: Identifies complete select component
        name={id}                   // ✅ REQUIRED: Matches htmlFor of label
        instanceId={id}             // ✅ REQUIRED: Stub for internal react-select elements
        inputId={id}                // ✅ REQUIRED: Internal input matches label htmlFor
        options={options}
        value={selectedValue}
        onChange={handleChange}
        isDisabled={disabled || readonly}
        isClearable={!required}
        placeholder="Select..."
      />
    </FormControl>
  )
}
```

### Why Each Prop Matters

| Prop                         | Value          | Purpose                       | Impact if Missing                    |
| ---------------------------- | -------------- | ----------------------------- | ------------------------------------ |
| `FormControl data-testid`    | `id`           | Identifies the property group | PMO `field()` selector fails         |
| `FormLabel id`               | `${id}-label`  | Label selector for tests      | PMO `label` selector fails           |
| `FormLabel htmlFor`          | `id`           | Accessibility link to input   | Screen readers can't associate label |
| `CreatableSelect id`         | `${id}-widget` | Identifies complete widget    | Hard to target in tests              |
| `CreatableSelect name`       | `id`           | Form field name               | Form submission may fail             |
| `CreatableSelect instanceId` | `id`           | React-select internal prefix  | Live regions get wrong IDs           |
| `CreatableSelect inputId`    | `id`           | Input element ID              | Label htmlFor doesn't work           |

### What React-Select Generates

With this pattern, react-select creates these elements:

```html
<div data-testid="root_messageType">                    <!-- FormControl -->
  <label id="root_messageType-label" for="root_messageType">  <!-- FormLabel -->
    Message Type
  </label>

  <div id="root_messageType-widget">                    <!-- CreatableSelect wrapper -->
    <div class="react-select__control">
      <div class="react-select__value-container">
        <div id="react-select-root_messageType-placeholder">Select...</div>
        <input
          id="root_messageType"                         <!-- inputId -->
          name="root_messageType"                       <!-- name -->
          type="text"
          aria-labelledby="root_messageType-label"
        />
      </div>
    </div>
    <input name="root_messageType" type="hidden" value="actualValue" />  <!-- Hidden input with value -->
  </div>

  <div id="react-select-root_messageType-live-region" aria-live="polite">
    <!-- Accessibility announcements -->
  </div>
</div>
```

---

## 🧪 Testing Patterns (Component Tests)

### Using Page Object Model (RJSFormField)

With the correct design pattern above, the RJSFormField PMO works perfectly:

```typescript
import { rjsf } from '@/../../cypress/pages/RJSF/RJSFormField.ts'

describe('MyComponent with RJSF', () => {
  it('should work with PMO', () => {
    cy.mountWithProviders(<MyComponent />)

    // Access field using PMO
    rjsf.field('messageType').label.should('have.text', 'Message Type')
    rjsf.field('messageType').select.click()
    cy.contains('[role="option"]', 'Option 1').click()

    // Verify selection
    rjsf.field('messageType').select.should('contain.text', 'Option 1')
  })
})
```

### Without PMO (Direct Selectors)

**⚠️ Component tests should NOT use PMO directly** (not properly set up for component tests). Use these patterns instead:

#### Pattern 1: Access the Select Widget

```typescript
// Click to open dropdown
cy.get('label#root_messageType-label + div').click()

// Or use the widget ID directly
cy.get('#root_messageType-widget').click()
```

#### Pattern 2: Verify Selected Value

```typescript
// Check the displayed value
cy.get('label#root_messageType-label + div').should('contain.text', 'Selected Value')
```

#### Pattern 3: Select an Option

```typescript
// Open dropdown
cy.get('label#root_messageType-label + div').click()

// Click option
cy.contains('[role="option"]', 'My Option').click()

// Verify onChange was called
cy.get('@onChangeSpy').should('have.been.calledWith', 'My Option')
```

#### Pattern 4: Test Disabled State

```typescript
// For disabled widgets, the input inside is disabled
cy.get('#root_messageType').should('be.disabled')
```

#### Pattern 5: Test Required Field

```typescript
// FormControl should have required indicator
cy.get('[data-testid="root_messageType"]').within(() => {
  cy.get('[role="presentation"]').should('exist') // Required asterisk
})
```

---

## 🔍 Common Testing Issues & Solutions

### Issue 1: "Element not found: `label#root_type-label + div`"

**Cause:** Field doesn't exist or hasn't rendered yet (conditional field based on form state)

**Solution:** Wait for field to appear

```typescript
cy.get('label#root_messageType-label').should('be.visible')
cy.get('label#root_messageType-label + div').click()
```

### Issue 2: "Save button disabled even after filling all fields"

**Cause:** PROTOBUF schemas require `messageType` field - it's conditionally required

**Solution:** Check JSON schema for conditional requirements

```typescript
// After switching to PROTOBUF type, wait for messageType field
cy.get('label#root_messageType-label').should('be.visible')
cy.get('label#root_messageType-label + div').click()
cy.get('[role="option"]').first().click()
```

### Issue 3: "Can't verify selected value"

**Cause:** React-select doesn't set `input.value` directly - value is in hidden input

**Solution:** Check displayed text, not input value

```typescript
// ❌ Wrong
cy.get('#root_type').should('have.value', 'JSON')

// ✅ Correct
cy.get('label#root_type-label + div').should('contain.text', 'JSON')
```

### Issue 4: "Field doesn't respond to clicks"

**Cause:** Field might be readonly or disabled

**Solution:** Check the actual state

```typescript
cy.get('#root_messageType').should('not.be.disabled')
cy.get('label#root_messageType-label + div').should('not.have.attr', 'aria-disabled', 'true')
```

---

## 📋 Widget Implementation Checklist

When creating a new RJSF widget with react-select:

- [ ] `FormControl` has `data-testid={id}`
- [ ] `FormLabel` has `id={`${id}-label`}`
- [ ] `FormLabel` has `htmlFor={id}`
- [ ] `CreatableSelect` has `id={`${id}-widget`}`
- [ ] `CreatableSelect` has `name={id}`
- [ ] `CreatableSelect` has `instanceId={id}`
- [ ] `CreatableSelect` has `inputId={id}`
- [ ] Handle `onChange` correctly (extract value from option)
- [ ] Handle `disabled` and `readonly` props
- [ ] Handle `required` prop
- [ ] Handle `rawErrors` for validation display
- [ ] Widget is registered in `datahubRJSFWidgets`
- [ ] UISchema uses correct widget reference

---

## 🎨 Example: Complete Widget Implementation

See `MessageTypeSelect.tsx` for reference implementation:

```typescript
export const MessageTypeSelect: FC<WidgetProps> = (props) => {
  const {
    id,
    value,
    onChange,
    required,
    disabled,
    readonly,
    schema,
    rawErrors,
    formContext
  } = props

  const { t } = useTranslation('datahub')
  const [options, setOptions] = useState<Option[]>([])

  // Access cross-field data via formContext
  const context = formContext as MyFormContext | undefined
  const liveData = context?.currentData

  const selectedValue = useMemo(() => {
    if (!value) return null
    return { label: value, value }
  }, [value])

  const handleChange = useCallback(
    (newValue: OnChangeValue<Option, false>, actionMeta: ActionMeta<Option>) => {
      if (actionMeta.action === 'select-option' && newValue) {
        onChange(newValue.value)
      }
      if (actionMeta.action === 'clear') {
        onChange(undefined)
      }
    },
    [onChange]
  )

  const hasErrors = rawErrors && rawErrors.length > 0

  return (
    <FormControl
      data-testid={id}
      isDisabled={disabled || readonly}
      isRequired={required}
      isReadOnly={readonly}
      isInvalid={hasErrors}
    >
      <FormLabel htmlFor={id} id={`${id}-label`}>
        {schema.title || t('field.label')}
      </FormLabel>

      <CreatableSelect<Option, false>
        id={`${id}-widget`}
        name={id}
        instanceId={id}
        inputId={id}
        options={options}
        value={selectedValue}
        onChange={handleChange}
        isClearable={!required}
        isDisabled={disabled || readonly}
        placeholder={t('field.placeholder')}
      />

      {hasErrors && (
        <FormErrorMessage id={`${id}-error`}>
          {rawErrors[0]}
        </FormErrorMessage>
      )}
    </FormControl>
  )
}
```

---

## 🧪 Example: Complete Test Implementation

```typescript
describe('MessageTypeSelect', () => {
  const getMockProps = (): WidgetProps => ({
    id: 'root_messageType',
    name: 'messageType',
    label: 'Message Type',
    onChange: cy.stub(),
    onBlur: cy.stub(),
    onFocus: cy.stub(),
    schema: { title: 'Message Type', type: 'string' },
    options: {},
    formContext: { currentSchemaSource: 'protobuf code...' },
  })

  it('should select a message type', () => {
    const onChangeSpy = cy.spy().as('onChange')
    const props = { ...getMockProps(), onChange: onChangeSpy }

    cy.mountWithProviders(<MessageTypeSelect {...props} />)

    // Wait for field to be ready
    cy.get('label#root_messageType-label').should('be.visible')

    // Open dropdown using label + div pattern
    cy.get('label#root_messageType-label + div').click()

    // Select option
    cy.contains('[role="option"]', 'Person').click()

    // Verify onChange called with value
    cy.get('@onChange').should('have.been.calledOnce')
    cy.get('@onChange').should('have.been.calledWith', 'Person')

    // Verify displayed value
    cy.get('label#root_messageType-label + div').should('contain.text', 'Person')
  })

  it('should be disabled when readonly', () => {
    const props = { ...getMockProps(), readonly: true }

    cy.mountWithProviders(<MessageTypeSelect {...props} />)

    // Input should be disabled
    cy.get('#root_messageType').should('be.disabled')
  })
})
```

---

## 📚 Related Documentation

- **RJSFormField PMO**: `cypress/pages/RJSF/RJSFormField.ts` - Page Object Model selectors
- **React-Select Docs**: https://react-select.com/home
- **Chakra React-Select**: https://github.com/csandman/chakra-react-select
- **RJSF Custom Widgets**: https://rjsf-team.github.io/react-jsonschema-form/docs/advanced-customization/custom-widgets-fields

---

## 🎓 Key Lessons from Task 37937

1. **Never assume tests pass without running them** - MessageTypeSelect tests were claimed passing but never actually run
2. **Use `.only` during development** - Run ONE test at a time, fix it, then move to next
3. **Understand the component before testing** - Read the code, understand the data requirements (e.g., PROTOBUF needs messageType)
4. **React-select needs special handling** - Can't check `input.value`, must check displayed text
5. **RJSF widgets need formContext for cross-field data** - Widgets don't have access to full formData
6. **Design and testing are interconnected** - The widget MUST follow this pattern for tests to work

---

**This pattern is MANDATORY for all RJSF custom widgets using select components.**

**Last verified:** Task 37937, MessageTypeSelect component, November 28, 2025
