# React JSON Schema Form (RJSF) Integration Guidelines

**Last Updated:** October 29, 2025  
**Task:** 25337 - Workspace Auto-Layout (Subtask 9)

---

## Overview

This document captures patterns, best practices, and guidelines for using React JSON Schema Form (RJSF) with ChakraUI in the HiveMQ Edge Frontend codebase.

---

## Table of Contents

1. [What is RJSF?](#what-is-rjsf)
2. [Architecture & File Structure](#architecture--file-structure)
3. [Schema Patterns](#schema-patterns)
4. [UI Schema Patterns](#ui-schema-patterns)
5. [Component Integration](#component-integration)
6. [Custom Widgets](#custom-widgets)
7. [Validation](#validation)
8. [Best Practices](#best-practices)
9. [Common Pitfalls](#common-pitfalls)

---

## What is RJSF?

**React JSON Schema Form** is a library that generates forms automatically from JSON Schema definitions. It provides:

- **Automatic form generation** from JSON Schema (v7)
- **ChakraUI integration** for consistent styling
- **Custom widgets** for specialized inputs
- **Validation** built-in from schema constraints
- **Conditional fields** based on form data

### When to Use RJSF

✅ **DO use RJSF when:**

- Form has many fields with similar patterns
- Schema already exists or can be defined declaratively
- Need conditional field visibility
- Want automatic validation from schema
- Form structure changes based on data (dynamic forms)

❌ **DON'T use RJSF when:**

- Form is very simple (2-3 fields)
- Needs highly custom UI that doesn't fit schema patterns
- Performance is critical with very large forms
- Complex interactive behaviors not schema-based

---

## Architecture & File Structure

### Directory Structure

```
src/
├── api/schemas/                    # API-related schemas
│   ├── bridge.json-schema.ts      # JSON Schema definition
│   ├── bridge.ui-schema.ts        # UI Schema definition
│   └── index.ts                    # Schema exports
│
├── modules/[Module]/schemas/       # Module-specific schemas
│   ├── [feature].json-schema.ts
│   ├── [feature].ui-schema.ts
│   └── index.ts
│
└── components/rjsf/                # RJSF customizations
    ├── Form/
    │   └── ChakraRJSForm.tsx      # Main form wrapper
    ├── Widgets/                    # Custom widgets
    ├── Templates/                  # Custom templates
    └── Fields/                     # Custom fields
```

### File Naming Conventions

- **JSON Schema:** `[name].json-schema.ts`
- **UI Schema:** `[name].ui-schema.ts`
- **Index exports:** `index.ts`

---

## Schema Patterns

### Basic JSON Schema Structure

```typescript
import type { JSONSchema7 } from 'json-schema'

export const mySchema: JSONSchema7 = {
  type: 'object',
  required: ['requiredField'],
  properties: {
    stringField: {
      type: 'string',
      title: 'String Field',
      description: 'Helper text for the field',
      default: 'default value',
      minLength: 1,
      maxLength: 100,
    },
    numberField: {
      type: 'number',
      title: 'Number Field',
      minimum: 0,
      maximum: 100,
      default: 50,
    },
    booleanField: {
      type: 'boolean',
      title: 'Boolean Field',
      default: true,
    },
    enumField: {
      type: 'string',
      title: 'Select Field',
      enum: ['option1', 'option2', 'option3'],
      default: 'option1',
    },
    arrayField: {
      type: 'array',
      title: 'Array Field',
      items: {
        type: 'string',
      },
    },
  },
}
```

### Property Definitions

#### Required vs Optional

```typescript
{
  required: ['field1', 'field2'], // These fields must be filled
  properties: {
    field1: { type: 'string' },    // Required
    field2: { type: 'number' },    // Required
    field3: { type: 'string' },    // Optional
  }
}
```

#### Data Types

```typescript
// String
{ type: 'string', format: 'email' | 'uri' | 'date-time' | 'mqtt-topic' }

// Number
{ type: 'number', minimum: 0, maximum: 100, multipleOf: 5 }

// Integer
{ type: 'integer', minimum: 1, maximum: 10 }

// Boolean
{ type: 'boolean', default: false }

// Array
{
  type: 'array',
  items: { type: 'string' },
  minItems: 1,
  maxItems: 10,
  uniqueItems: true
}

// Object
{
  type: 'object',
  properties: { /* nested properties */ }
}

// Enum (dropdown)
{
  type: 'string',
  enum: ['value1', 'value2', 'value3']
}
```

#### Conditional Schema (if/then/else)

```typescript
{
  properties: {
    enabled: { type: 'boolean' }
  },
  if: {
    properties: { enabled: { const: true } }
  },
  then: {
    properties: {
      // Only show these when enabled=true
      option1: { type: 'string' },
      option2: { type: 'number' }
    },
    required: ['option1']
  },
  else: {
    properties: {
      // Show these when enabled=false
      disabledMessage: { type: 'string' }
    }
  }
}
```

### Schema Definitions (Reusable Types)

```typescript
{
  definitions: {
    Address: {
      type: 'object',
      properties: {
        street: { type: 'string' },
        city: { type: 'string' },
      }
    }
  },
  properties: {
    homeAddress: { $ref: '#/definitions/Address' },
    workAddress: { $ref: '#/definitions/Address' },
  }
}
```

---

## UI Schema Patterns

### Basic UI Schema Structure

```typescript
import type { UiSchema } from '@rjsf/utils'
import i18nConfig from '@/config/i18n.config.ts'

export const myUISchema: UiSchema = {
  // Hide default submit button (we add custom one in footer)
  'ui:submitButtonOptions': {
    norender: true,
  },

  // Field order
  'ui:order': ['field1', 'field2', '*', 'field3'],

  // Field customization
  fieldName: {
    'ui:title': i18nConfig.t('my.field.title'),
    'ui:description': i18nConfig.t('my.field.helper'),
    'ui:placeholder': 'Enter value...',
    'ui:widget': 'updown', // Custom widget
    'ui:help': 'Additional help text',
  },
}
```

### Common UI Schema Options

#### Field Titles & Descriptions

```typescript
fieldName: {
  'ui:title': 'Field Label',              // Replaces schema title
  'ui:description': 'Helper text',        // Replaces schema description
  'ui:help': 'Additional help',           // Alternative to description
  'ui:placeholder': 'Placeholder text',
}
```

**⚠️ IMPORTANT: `description` vs `ui:help` vs `ui:description`**

- **`description` (in JSON Schema)**: Displays as help text below the field
- **`ui:description` (in UI Schema)**: **Replaces** the schema `description`
- **`ui:help` (in UI Schema)**: Displays **in addition to** the schema `description`

**Issue:** If you define both `description` in the schema AND `ui:help` in the UI schema, **both will display** below the field, creating duplicate text!

**Best Practice:**

```typescript
// ✅ CORRECT: Use description in schema only
schema: {
  myField: {
    type: 'string',
    description: 'This is the help text'  // Shows below field
  }
}
uiSchema: {
  myField: {
    'ui:widget': 'updown'  // No ui:help needed
  }
}

// ❌ WRONG: Duplicate description
schema: {
  myField: {
    description: 'This is the help text'  // Shows below field
  }
}
uiSchema: {
  myField: {
    'ui:help': 'This is the help text'  // Shows below field again! Duplicate!
  }
}

// ✅ CORRECT: Override description
schema: {
  myField: {
    description: 'Generic help text'
  }
}
uiSchema: {
  myField: {
    'ui:description': 'More specific help text'  // Replaces schema description
  }
}

// ✅ CORRECT: Add extra help (intentionally)
schema: {
  myField: {
    description: 'Basic info'
  }
}
uiSchema: {
  myField: {
    'ui:help': 'Additional context'  // Shows in addition to description
  }
}
```

**Rule of Thumb:**

- Put help text in the **schema `description`** by default
- Only use `ui:help` if you want **additional** text beyond the schema description
- Use `ui:description` to **override** the schema description for a specific UI context

#### Widget Selection

```typescript
// Number input with up/down buttons
{ 'ui:widget': 'updown' }

// Checkbox (for boolean)
{ 'ui:widget': 'checkbox' }

// Toggle switch (custom widget)
{ 'ui:widget': ToggleWidget }

// Hidden field
{ 'ui:widget': 'hidden' }

// Password input
{ 'ui:widget': 'password' }

// Textarea
{ 'ui:widget': 'textarea' }

// Select dropdown (default for enum)
{ 'ui:widget': 'select' }
```

#### Field Ordering

```typescript
{
  // Explicit order, '*' = all other fields
  'ui:order': ['id', 'name', 'email', '*', 'submit'],
}
```

#### Field Visibility

```typescript
{
  fieldName: {
    'ui:options': {
      // Conditional visibility (string expression)
      hidden: '{{formData.otherField === false}}',
    },
  },
}
```

#### Disabled/Readonly

```typescript
{
  fieldName: {
    'ui:disabled': true,              // Gray out, can't edit
    'ui:readonly': true,              // Display only
    'ui:options': {
      disabled: '{{formData.flag}}',  // Conditional
    },
  },
}
```

### Array Fields

```typescript
{
  arrayField: {
    'ui:options': {
      addable: true,         // Show "Add" button
      removable: true,       // Show "Remove" button
      orderable: true,       // Show up/down buttons
    },
    items: {
      'ui:addButton': 'Add Item',
      'ui:collapsable': {
        titleKey: 'name',    // Use field as collapse title
      },

      // Nested field options
      name: {
        'ui:title': 'Item Name',
      },
    },
  },
}
```

### Tabs

```typescript
{
  'ui:tabs': [
    {
      id: 'tab1',
      title: 'Connection',
      properties: ['host', 'port', 'username'],
    },
    {
      id: 'tab2',
      title: 'Security',
      properties: ['tlsEnabled', 'certificate'],
    },
  ],
}
```

### Enum Names (Custom Labels)

```typescript
{
  algorithmType: {
    'ui:enumNames': [
      'Network Simplex (default)',  // Label for enum value 0
      'Tight Tree',                 // Label for enum value 1
      'Longest Path',               // Label for enum value 2
    ],
  },
}
```

---

## Component Integration

### Basic Pattern (Drawer with Form)

```typescriptreact
import type { FC } from 'react'
import type { IChangeEvent } from '@rjsf/core'
import {
  Button,
  ButtonGroup,
  Card,
  CardBody,
  Drawer,
  DrawerBody,
  DrawerFooter,
  DrawerHeader,
  DrawerOverlay,
  DrawerContent,
  DrawerCloseButton,
} from '@chakra-ui/react'

import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { mySchema, myUISchema } from './schemas'

interface MyDrawerProps {
  isOpen: boolean
  onClose: () => void
  initialData: MyData
  onSubmit: (data: MyData) => void
}

const MyDrawer: FC<MyDrawerProps> = ({ isOpen, onClose, initialData, onSubmit }) => {
  const handleSubmit = (event: IChangeEvent<MyData>) => {
    if (event.formData) {
      onSubmit(event.formData)
      onClose()
    }
  }

  const handleCancel = () => {
    onClose()
  }

  return (
    <Drawer
      isOpen={isOpen}
      placement="right"
      size="lg"
      onClose={onClose}
      variant="hivemq"
      id="my-drawer"
    >
      <DrawerOverlay />
      <DrawerContent aria-label="My Form">
        <DrawerCloseButton />
        <DrawerHeader>My Form Title</DrawerHeader>

        <DrawerBody>
          <Card>
            <CardBody>
              <ChakraRJSForm
                id="my-form"
                schema={mySchema}
                uiSchema={myUISchema}
                formData={initialData}
                onSubmit={handleSubmit}
                showNativeWidgets={false}
              />
            </CardBody>
          </Card>
        </DrawerBody>

        <DrawerFooter>
          <ButtonGroup flexGrow={1} justifyContent="flex-end">
            <Button onClick={handleCancel}>Cancel</Button>
            <Button variant="primary" type="submit" form="my-form">
              Save
            </Button>
          </ButtonGroup>
        </DrawerFooter>
      </DrawerContent>
    </Drawer>
  )
}
```

### Key Integration Points

1. **Form ID:** Must match between `<ChakraRJSForm id="my-form">` and `<Button form="my-form">`
2. **Submit button location:** In DrawerFooter, not in form (uses `type="submit" form="form-id"`)
3. **Hidden default submit:** `'ui:submitButtonOptions': { norender: true }` in UI schema
4. **Form wrapper:** Always wrap form in `<Card><CardBody>` for consistent styling
5. **Drawer variant:** Use `variant="hivemq"` for consistent drawer styling

---

## Custom Widgets

### Using Built-in Widgets

```typescript
import ToggleWidget from '@/components/rjsf/Widgets/ToggleWidget.tsx'
import UpDownWidget from '@/components/rjsf/Widgets/UpDownWidget'

export const myUISchema: UiSchema = {
  booleanField: {
    'ui:widget': ToggleWidget, // Toggle switch instead of checkbox
  },
  numberField: {
    'ui:widget': 'updown', // Number with +/- buttons
  },
}
```

### Available Custom Widgets

- **ToggleWidget** - ChakraUI Switch for booleans
- **UpDownWidget** - Number input with stepper buttons
- _(Check `/src/components/rjsf/Widgets/` for full list)_

---

## Validation

### Schema-based Validation

```typescript
{
  properties: {
    email: {
      type: 'string',
      format: 'email',      // Built-in email validation
      minLength: 5,
      maxLength: 100,
    },
    age: {
      type: 'number',
      minimum: 18,          // Must be >= 18
      maximum: 120,
      multipleOf: 1,        // Must be integer
    },
    topic: {
      type: 'string',
      format: 'mqtt-topic', // Custom format validator
    },
  }
}
```

### Custom Validation

```typescript
import type { RJSFValidationError } from '@rjsf/utils'

const customValidate = (formData: MyData, errors: RJSFValidationError) => {
  if (formData.field1 && formData.field2 === formData.field1) {
    errors.field2.addError('Must be different from field1')
  }
  return errors
}

// Usage in component
<ChakraRJSForm
  schema={mySchema}
  customValidate={customValidate}
  // ...
/>
```

---

## Best Practices

### ✅ DO

1. **Separate schema files**

   - Keep JSON schema and UI schema in separate files
   - Export from index.ts for clean imports

2. **Use i18n for labels**

   ```typescript
   'ui:title': i18nConfig.t('my.field.label')
   ```

3. **Hide default submit button**

   ```typescript
   'ui:submitButtonOptions': { norender: true }
   ```

4. **Add custom buttons in footer**

   ```tsx
   <DrawerFooter>
     <Button onClick={onCancel}>Cancel</Button>
     <Button variant="primary" type="submit" form="form-id">
       Save
     </Button>
   </DrawerFooter>
   ```

5. **Use `variant="primary"` for submit**

   - Follows design guidelines
   - Main action button should be primary variant

6. **Wrap form in Card**

   ```tsx
   <Card>
     <CardBody>
       <ChakraRJSForm ... />
     </CardBody>
   </Card>
   ```

7. **Set drawer variant**

   ```tsx
   <Drawer variant="hivemq" ... />
   ```

8. **Add aria-label to drawer**

   ```tsx
   <DrawerContent aria-label="Form Description">
   ```

9. **Use proper sizing**

   - `size="sm"` - Narrow forms
   - `size="md"` - Medium forms (default for options)
   - `size="lg"` - Wide forms (bridges, adapters)

10. **Filter schema properties dynamically**
    ```typescript
    const filteredSchema = useMemo(
      () => ({
        ...baseSchema,
        properties: relevantProperties,
      }),
      [condition]
    )
    ```

### ❌ DON'T

1. **Don't use `colorScheme` on submit buttons**

   ```tsx
   <Button colorScheme="blue">Save</Button>  // ❌ Wrong
   <Button variant="primary">Save</Button>    // ✅ Correct
   ```

2. **Don't put submit button inside form body**

   - Always in `DrawerFooter` or `ModalFooter`

3. **Don't forget form ID matching**

   ```tsx
   <ChakraRJSForm id="my-form" />
   <Button form="my-form" />  // IDs must match!
   ```

4. **Don't inline large schemas**

   - Keep in separate schema files
   - Makes maintenance easier

5. **Don't forget `showNativeWidgets={false}`**

   - Uses custom ChakraUI widgets

6. **Don't create multiple submit buttons**
   - One submit button per form
   - Use type="submit" with form attribute

---

## Common Pitfalls

### Issue: Submit button doesn't work

**Cause:** Form ID doesn't match button's `form` attribute

**Solution:**

```tsx
<ChakraRJSForm id="my-unique-form-id" ... />
<Button type="submit" form="my-unique-form-id">Submit</Button>
```

---

### Issue: Two submit buttons appear

**Cause:** Didn't hide default submit button

**Solution:**

```typescript
uiSchema: {
  'ui:submitButtonOptions': { norender: true },
}
```

---

### Issue: Fields don't show conditional visibility

**Cause:** Using wrong syntax for conditional hiding

**Solution:**

```typescript
fieldName: {
  'ui:options': {
    hidden: '{{formData.otherField === false}}', // String expression!
  }
}
```

---

### Issue: Widget doesn't apply

**Cause:** Widget name is a string when it should be a component reference

**Solution:**

```typescript
// Built-in widget (string)
{ 'ui:widget': 'updown' }

// Custom widget (component reference)
import ToggleWidget from '@/components/rjsf/Widgets/ToggleWidget.tsx'
{ 'ui:widget': ToggleWidget }
```

---

### Issue: Validation doesn't work

**Cause:** Schema constraints not properly defined

**Solution:**

```typescript
// ❌ Wrong
{ type: 'string', min: 5 }

// ✅ Correct
{ type: 'string', minLength: 5 }
```

---

### Issue: Enum dropdown shows values instead of labels

**Cause:** Missing `ui:enumNames`

**Solution:**

```typescript
schema: {
  type: 'string',
  enum: ['value1', 'value2']
}

uiSchema: {
  'ui:enumNames': ['Label 1', 'Label 2']
}
```

---

## Example: Complete Implementation

See the Layout Options Drawer refactoring (Task 25337, Subtask 9) for a complete example:

- **JSON Schema:** `src/modules/Workspace/schemas/layout-options.json-schema.ts`
- **UI Schema:** `src/modules/Workspace/schemas/layout-options.ui-schema.ts`
- **Component:** `src/modules/Workspace/components/layout/LayoutOptionsDrawer.tsx`

This example demonstrates:

- Dynamic schema filtering based on algorithm type
- Conditional field visibility
- Custom widgets (updown, checkbox)
- Drawer integration with footer buttons
- Proper form ID matching
- Card wrapping
- Primary button variant

---

## Resources

- **RJSF Docs:** https://rjsf-team.github.io/react-jsonschema-form/
- **JSON Schema Spec:** https://json-schema.org/
- **ChakraUI Docs:** https://chakra-ui.com/
- **Codebase Examples:**
  - Bridge Form: `src/modules/Bridges/components/BridgeEditorDrawer.tsx`
  - Domain Tags: `src/api/schemas/domain-tags.*`
  - Managed Assets: `src/api/schemas/managed-asset.*`

---

**Last Updated:** October 29, 2025  
**Maintainer:** AI Agent Documentation
