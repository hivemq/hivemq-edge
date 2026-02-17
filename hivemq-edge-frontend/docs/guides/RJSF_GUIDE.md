---
title: 'RJSF Integration Guide'
author: 'Edge Frontend Team'
last_updated: '2026-02-16'
purpose: 'Complete guide for implementing JSON Schema forms with RJSF and Chakra UI'
audience: 'Frontend Developers, AI Agents'
maintained_at: 'docs/guides/RJSF_GUIDE.md'
---

# RJSF Integration Guide

---

## Overview

**React JSON Schema Form (RJSF)** is the primary form generation library used across the entire HiveMQ Edge Frontend application. It automatically generates forms from JSON Schema definitions with Chakra UI styling.

### What is RJSF?

RJSF provides:
- Automatic form generation from JSON Schema (v7)
- Chakra UI integration for consistent styling
- Custom widgets for specialized inputs
- Built-in validation from schema constraints
- Conditional field visibility
- Tab-based organization
- Support for complex nested structures

### When to Use RJSF

✅ **DO use RJSF when:**
- Form has many fields with similar patterns
- Schema already exists or can be defined declaratively
- Need conditional field visibility
- Want automatic validation from schema
- Form structure changes based on data (dynamic forms)
- Backend provides schema via API

❌ **DON'T use RJSF when:**
- Form is very simple (2-3 fields)
- Needs highly custom UI that doesn't fit schema patterns
- Performance is critical with very large forms (>100 fields)
- Complex interactive behaviors not schema-based

---

## Forms Using RJSF

RJSF is used extensively throughout the application. Below is a complete inventory of all forms.

| Form | Component | Schema Source | Location |
|------|-----------|---------------|----------|
| **Protocol Adapters** | `AdapterInstanceDrawer.tsx` | Backend via OpenAPI | `src/modules/ProtocolAdapters/components/drawers/` |
| **Bridges** | `BridgeEditorDrawer.tsx` | Frontend (`api/schemas/bridge.*`) | `src/modules/Bridges/components/` |
| **Domain Tags** | `TagEditorDrawer.tsx` | Frontend (`api/schemas/domain-tags.*`) | `src/modules/Device/components/` |
| **Managed Assets** | `ManagedAssetDrawer.tsx` | Frontend (`api/schemas/managed-asset.*`) | `src/modules/Pulse/components/assets/` |
| **Combiner Mappings** | `MappingForm.tsx` | Frontend (`api/schemas/combiner-mapping.*`, `northbound.*`, `southbound.*`) | `src/modules/Mappings/` |
| **DataHub Schemas** | `SchemaPanel.tsx` | Component-defined | `src/extensions/datahub/designer/schema/` |
| **DataHub Scripts** | `FunctionPanel.tsx` | Component-defined | `src/extensions/datahub/designer/script/` |
| **DataHub Operations** | `OperationPanel.tsx` | Component-defined | `src/extensions/datahub/designer/operation/` |
| **DataHub Transitions** | `TransitionPanel.tsx` | Component-defined | `src/extensions/datahub/designer/transition/` |
| **Workspace Layout** | `LayoutOptionsDrawer.tsx` | Frontend (`modules/Workspace/schemas/layout-options.*`) | `src/modules/Workspace/components/layout/` |
| **Workspace Wizard - Adapter** | `WizardAdapterForm.tsx` | Backend via OpenAPI | `src/modules/Workspace/components/wizard/steps/` |
| **Workspace Wizard - Bridge** | `WizardBridgeForm.tsx` | Frontend (`api/schemas/bridge.*`) | `src/modules/Workspace/components/wizard/steps/` |

### Schema Source Patterns

**Backend-Provided Schemas:**
- Protocol adapters receive JSON Schema + UI Schema from backend OpenAPI endpoint
- Fetched via `useGetAdapterTypes()` hook
- Backend generates schemas from Java annotations

**Frontend-Defined Schemas:**
- Located in `src/api/schemas/` for cross-module schemas
- Located in `src/modules/[Module]/schemas/` for module-specific schemas
- Defined as TypeScript files (`.json-schema.ts`, `.ui-schema.ts`)

**Component-Defined Schemas:**
- DataHub panels define schemas inline for flexibility
- Allows dynamic schema generation based on resource state

---

## Form Component API Reference

The `<ChakraRJSForm>` component is a wrapper around RJSF's `<Form>` that provides Chakra UI styling, custom widgets, and application-specific templates. Understanding its props is essential for customization.

### Component Import

```typescript
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
```

### Core Props

#### schema (required)

**Type:** `JSONSchema7`
**Purpose:** Defines the data structure, validation rules, and field types.

```typescript
const schema: JSONSchema7 = {
  type: 'object',
  required: ['name', 'port'],
  properties: {
    name: {
      type: 'string',
      title: 'Connection Name',
      minLength: 1,
      maxLength: 100,
    },
    port: {
      type: 'integer',
      title: 'Port',
      minimum: 1,
      maximum: 65535,
      default: 1883,
    },
    enabled: {
      type: 'boolean',
      title: 'Enabled',
      default: true,
    },
  },
}
```

**Key Features:**
- Automatic validation from constraints (`minLength`, `minimum`, etc.)
- Type coercion (string → number for `type: 'number'`)
- Required field enforcement
- Default values

---

#### uiSchema (optional)

**Type:** `UiSchema`
**Purpose:** Controls UI presentation, widgets, ordering, and tabs without changing data structure.

```typescript
const uiSchema: UiSchema = {
  'ui:order': ['name', 'enabled', 'port'],
  name: {
    'ui:title': 'Custom Label',
    'ui:description': 'Helper text below the field',
    'ui:placeholder': 'Enter connection name',
  },
  port: {
    'ui:widget': 'updown',  // Use spinner widget
  },
  enabled: {
    'ui:widget': 'switch',  // Use toggle instead of checkbox
  },
  'ui:submitButtonOptions': {
    submitText: 'Create Connection',
  },
}
```

**Common Uses:**
- Override field labels and descriptions
- Select custom widgets
- Control field visibility (`ui:widget: 'hidden'`)
- Organize fields into tabs
- Set field ordering

---

#### formData (required)

**Type:** `T` (generic object matching schema shape)
**Purpose:** The current form values. Can be initial data or controlled state.

```typescript
interface ConnectionData {
  name: string
  port: number
  enabled: boolean
}

const formData: ConnectionData = {
  name: 'My MQTT Connection',
  port: 1883,
  enabled: true,
}
```

**Patterns:**
- **New entities:** Use empty object `{}` or defaults
- **Edit mode:** Load from API response
- **Controlled forms:** Track with `useState` and `onChange`

```typescript
const [formData, setFormData] = useState<ConnectionData>(initialData)

<ChakraRJSForm
  schema={schema}
  uiSchema={uiSchema}
  formData={formData}
  onChange={(e) => setFormData(e.formData)}
  onSubmit={handleSubmit}
/>
```

---

#### formContext (optional)

**Type:** `Record<string, any>` (often typed as specific interface)
**Purpose:** Share data between custom widgets/fields that isn't part of the form data itself.

```typescript
interface CombinerContext {
  queries: UseQueryResult[]  // React Query instances
  entities: DataSource[]      // Available data sources
}

const formContext: CombinerContext = {
  queries: [tagsQuery, topicFiltersQuery],
  entities: combiner.sources.items,
}

<ChakraRJSForm
  schema={schema}
  uiSchema={uiSchema}
  formData={formData}
  formContext={formContext}  // Accessible in custom widgets
  onSubmit={handleSubmit}
/>
```

**Common Uses:**
- Pass React Query instances to widgets that fetch data
- Share entity lists (adapters, bridges) for dropdown options
- Provide validation context (existing IDs to check uniqueness)
- Pass callbacks for complex interactions

**Accessing in Custom Widget:**

```typescript
const MyCustomWidget = (props: WidgetProps) => {
  const { queries, entities } = props.formContext as CombinerContext
  // Use queries and entities here
}
```

---

#### templates (internal)

**Type:** `Partial<TemplatesType>`
**Purpose:** Override RJSF's rendering of structural elements (fields, arrays, objects).

**Note:** The `ChakraRJSForm` component pre-configures templates. You typically don't need to override them unless extending `ChakraRJSForm` itself.

```typescript
// Internal configuration in ChakraRJSForm.tsx
templates={{
  ObjectFieldTemplate,     // Renders object properties
  FieldTemplate,           // Wraps each field with label/description
  BaseInputTemplate,       // Base input styling
  ArrayFieldTemplate,      // Renders array fields with add/remove
  ArrayFieldItemTemplate,  // Individual array item
  DescriptionFieldTemplate,// Field descriptions
  ErrorListTemplate,       // Validation error display
  TitleFieldTemplate,      // Field titles/labels
}}
```

**When to customize:**
- Building a new base form component
- Changing global field layout
- Custom array item rendering

---

#### widgets (internal)

**Type:** `RegistryWidgetsType`
**Purpose:** Map custom input components to `ui:widget` names.

**Note:** The `ChakraRJSForm` component pre-registers widgets. You typically don't need to override them.

```typescript
// Internal configuration in ChakraRJSForm.tsx
widgets={{
  ...adapterJSFWidgets,  // Protocol adapter widgets
  UpDownWidget,          // Number spinner
}}
```

**Pre-registered widgets:**
- `mqtt:subscription-select` - Subscription selector with validation
- `file-upload` - File upload with validation
- `password-input` - Password input with visibility toggle
- `updown` - Number input with increment/decrement buttons

**Usage in uiSchema:**

```typescript
{
  password: {
    'ui:widget': 'password-input'
  }
}
```

See [Custom Widgets](#custom-widgets) section for widget development.

---

#### fields (internal)

**Type:** `RegistryFieldsType`
**Purpose:** Map custom field components that replace entire field rendering (not just the input).

**Note:** The `ChakraRJSForm` component pre-registers fields. You typically don't need to override them.

```typescript
// Internal configuration in ChakraRJSForm.tsx
fields={{
  'mqtt:transform': MqttTransformationField,
}}
```

**Difference from Widgets:**
- **Widget:** Replaces just the input element (e.g., text input → dropdown)
- **Field:** Replaces the entire field including label, description, validation display

**Usage in uiSchema:**

```typescript
{
  transformation: {
    'ui:field': 'mqtt:transform'
  }
}
```

See [Custom Fields](#custom-fields) section for field development.

---

#### validator (internal)

**Type:** `ValidatorType`
**Purpose:** The JSON Schema validator instance. Handles schema compliance and custom format validation.

**Note:** The `ChakraRJSForm` component uses `customFormatsValidator` which extends the default AJV validator with custom formats.

```typescript
// Internal configuration in ChakraRJSForm.tsx
import { customFormatsValidator } from '@/components/rjsf/Form/validation.utils.ts'

validator={customFormatsValidator}
```

**Custom formats provided:**
- `mqtt-topic` - Validates MQTT topic syntax
- `identifier` - Validates identifier format (alphanumeric, hyphens, underscores)
- `file-extension` - Validates file extensions

**Schema usage:**

```typescript
{
  topic: {
    type: 'string',
    format: 'mqtt-topic',  // Uses custom validator
  }
}
```

See [Validation](#validation) section for custom format development.

---

#### customValidate (optional)

**Type:** `(formData: T, errors: FormValidation, uiSchema?: UiSchema) => FormValidation`
**Purpose:** Add cross-field validation or business logic validation beyond schema constraints.

```typescript
const customValidate = (formData, errors, uiSchema) => {
  // Check uniqueness against existing bridges
  if (existingBridges.includes(formData.id)) {
    errors.id?.addError('Bridge ID must be unique')
  }

  // Cross-field validation
  if (formData.enabled && !formData.host) {
    errors.host?.addError('Host is required when connection is enabled')
  }

  return errors
}

<ChakraRJSForm
  schema={schema}
  uiSchema={uiSchema}
  formData={formData}
  customValidate={customValidate}
  onSubmit={handleSubmit}
/>
```

**Common Uses:**
- Uniqueness validation (check against existing entities)
- Cross-field dependencies (field A requires field B)
- Business logic validation (port ranges for specific protocols)
- Async validation results (show errors from server)

**Pattern for uniqueness checking:**

```typescript
export const customUniqueBridgeValidate = (existingBridges: string[]) =>
  (formData: Bridge, errors: FormValidation) => {
    if (existingBridges.includes(formData.id)) {
      errors.id?.addError('Bridge ID already exists')
    }
    return errors
  }

// Usage
<ChakraRJSForm
  customValidate={customUniqueBridgeValidate(allBridges.map(b => b.id))}
/>
```

---

### Complete Example

```typescript
import { useState } from 'react'
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import type { JSONSchema7 } from 'json-schema'
import type { UiSchema } from '@rjsf/utils'

interface BridgeConfig {
  id: string
  host: string
  port: number
  enabled: boolean
}

const schema: JSONSchema7 = {
  type: 'object',
  required: ['id', 'host', 'port'],
  properties: {
    id: {
      type: 'string',
      title: 'Bridge ID',
      pattern: '^[a-zA-Z0-9-_]+$',
    },
    host: {
      type: 'string',
      title: 'MQTT Broker Host',
      format: 'hostname',
    },
    port: {
      type: 'integer',
      title: 'Port',
      minimum: 1,
      maximum: 65535,
      default: 1883,
    },
    enabled: {
      type: 'boolean',
      title: 'Enabled',
      default: true,
    },
  },
}

const uiSchema: UiSchema = {
  'ui:order': ['id', 'host', 'port', 'enabled'],
  id: {
    'ui:placeholder': 'my-bridge',
  },
  port: {
    'ui:widget': 'updown',
  },
  enabled: {
    'ui:widget': 'switch',
  },
}

export const BridgeForm = () => {
  const [formData, setFormData] = useState<BridgeConfig>({
    id: '',
    host: '',
    port: 1883,
    enabled: true,
  })

  const customValidate = (data: BridgeConfig, errors) => {
    if (data.enabled && data.port < 1024) {
      errors.port?.addError('Enabled bridges must use port >= 1024')
    }
    return errors
  }

  const handleSubmit = (e) => {
    console.log('Submitted:', e.formData)
  }

  return (
    <ChakraRJSForm
      id="bridge-form"
      schema={schema}
      uiSchema={uiSchema}
      formData={formData}
      onChange={(e) => setFormData(e.formData)}
      onSubmit={handleSubmit}
      customValidate={customValidate}
    />
  )
}
```

---

## JSON Schema Patterns

### Basic Structure

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
  },
}
```

### Data Types

| Type | Constraints | Example |
|------|-------------|---------|
| `string` | `minLength`, `maxLength`, `pattern`, `format` | Text inputs, email, URI |
| `number` | `minimum`, `maximum`, `multipleOf` | Numeric fields |
| `integer` | `minimum`, `maximum` | Whole numbers |
| `boolean` | N/A | Checkboxes, toggles |
| `array` | `minItems`, `maxItems`, `uniqueItems` | Lists of items |
| `object` | `properties`, `required` | Nested structures |
| `enum` | Fixed set of values | Dropdowns |

### Conditional Schemas (if/then/else)

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
      option1: { type: 'string' },
      option2: { type: 'number' }
    },
    required: ['option1']
  },
  else: {
    properties: {
      disabledMessage: { type: 'string' }
    }
  }
}
```

### Schema Definitions ($ref)

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

## UI Schema Reference

UI Schema controls how the form is rendered. All properties below can be used to customize form behavior and appearance.

### Standard Properties

| Property | Purpose | Example Values |
|----------|---------|----------------|
| `ui:widget` | Override widget implementation | `"updown"`, `"password"`, `"textarea"`, Component |
| `ui:field` | Override field implementation | Field component name or reference |
| `ui:order` | Reorder object properties | `['id', 'name', '*', 'submit']` |
| `ui:title` | Customize field title | `"Custom Title"` |
| `ui:description` | Replace schema description | `"Custom description"` |
| `ui:help` | Add guidance text (in addition to description) | `"Additional help"` |
| `ui:placeholder` | Add placeholder text | `"Enter value..."` |
| `ui:disabled` | Disable all child widgets | `true`, `false` |
| `ui:readonly` | Mark field as read-only | `true`, `false` |
| `ui:classNames` | Add CSS classes | `"custom-class another-class"` |
| `ui:style` | Apply inline styles | `{color: 'red'}` |
| `ui:enumNames` | Custom labels for enum values | `['Label 1', 'Label 2']` |
| `ui:enumDisabled` | Disable specific enum options | `['value1', 'value3']` |

### UI Options (ui:options)

Properties that go inside `ui:options`:

| Option | Purpose | Values |
|--------|---------|--------|
| `label` | Control label rendering | `true`, `false` |
| `hidden` | Conditional visibility | `"{{formData.field === false}}"` |
| `disabled` | Conditional disabled state | `"{{formData.flag}}"` |
| `addable` | Show "Add" button (arrays) | `true`, `false` |
| `removable` | Show "Remove" button (arrays) | `true`, `false` |
| `orderable` | Show up/down buttons (arrays) | `true`, `false` |

### Submit Button Configuration

```typescript
{
  'ui:submitButtonOptions': {
    norender: true,  // Hide default submit button
    submitText: 'Save',  // Custom button text
    props: {
      disabled: true,  // Disable button
      className: 'custom-class',
    }
  }
}
```

### Description vs ui:help vs ui:description

⚠️ **CRITICAL DISTINCTION:**

- **`description` (in JSON Schema)**: Displays as help text below the field
- **`ui:description` (in UI Schema)**: **REPLACES** the schema `description`
- **`ui:help` (in UI Schema)**: Displays **in addition to** the schema `description`

**Best Practice:**
- Put help text in the schema `description` by default
- Only use `ui:help` if you want **additional** text beyond the schema description
- Use `ui:description` to **override** the schema description for a specific UI context

### Tabs Configuration

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
  ]
}
```

### Array Field Configuration

```typescript
{
  arrayField: {
    'ui:options': {
      addable: true,
      removable: true,
      orderable: true,
    },
    items: {
      'ui:addButton': 'Add Item',
      'ui:collapsable': {
        titleKey: 'name',  // Use field as collapse title
      },
      name: {
        'ui:title': 'Item Name',
      },
    },
  }
}
```

---

## Custom Widgets

Widgets replace the default input component for individual fields. HiveMQ Edge Frontend has extensive custom widget support.

### Widget Inventory

#### Protocol Adapter Widgets

| Widget | File | Purpose | Registration |
|--------|------|---------|--------------|
| `UpDownWidget` | `src/components/rjsf/Widgets/UpDownWidget.tsx` | Number input with +/- buttons | ChakraRJSForm |
| `password` | Built-in RJSF/Chakra | Password input | Standard |
| `textarea` | Built-in RJSF/Chakra | Multi-line text | Standard |
| `select` | Built-in RJSF/Chakra | Dropdown | Standard |

#### DataHub Widgets

| Widget Key | Component | Purpose | File |
|------------|-----------|---------|------|
| `application/schema+json` | JSONSchemaEditor | JSON schema editor | `src/extensions/datahub/components/forms` |
| `text/javascript` | JavascriptEditor | JavaScript editor | `src/extensions/datahub/components/forms` |
| `application/octet-stream` | ProtoSchemaEditor | Protobuf schema editor | `src/extensions/datahub/components/forms` |
| `datahub:function-selector` | FunctionCreatableSelect | Function selector with create | `src/extensions/datahub/components/forms/FunctionCreatableSelect.tsx` |
| `datahub:metric-counter` | MetricCounterInput | Metric counter input | `src/extensions/datahub/components/forms/MetricCounterInput.tsx` |
| `datahub:version` | VersionManagerSelect | Version selector | `src/extensions/datahub/components/forms/VersionManagerSelect.tsx` |
| `datahub:message-interpolation` | MessageInterpolationTextArea | Message template editor | `src/extensions/datahub/components/forms/MessageInterpolationTextArea.tsx` |
| `datahub:message-type` | MessageTypeSelect | Protobuf message type selector | `src/extensions/datahub/components/forms/MessageTypeSelect.tsx` |
| `datahub:transition-selector` | TransitionSelect | State transition selector | `src/extensions/datahub/components/forms/TransitionSelect.tsx` |
| `datahub:behavior-model-selector` | BehaviorModelSelectDropdown | Behavior model dropdown | `src/extensions/datahub/components/forms/BehaviorModelSelectDropdown.tsx` |
| `datahub:behavior-model-selector-radio` | BehaviorModelSelect | Behavior model radio cards | `src/extensions/datahub/components/forms/BehaviorModelSelect.tsx` |
| `datahub:behavior-model-readonly` | BehaviorModelReadOnlyDisplay | Read-only behavior model | `src/extensions/datahub/components/forms/BehaviorModelReadOnlyDisplay.tsx` |
| `datahub:function-name` | ScriptNameCreatableSelect | Script name with create | `src/extensions/datahub/components/forms/ResourceNameCreatableSelect.tsx` |
| `datahub:schema-name` | SchemaNameCreatableSelect | Schema name with create | `src/extensions/datahub/components/forms/ResourceNameCreatableSelect.tsx` |
| `datahub:function-name-select` | ScriptNameSelect | Script name select-only | `src/extensions/datahub/components/forms/ResourceNameCreatableSelect.tsx` |
| `datahub:schema-name-select` | SchemaNameSelect | Schema name select-only | `src/extensions/datahub/components/forms/ResourceNameCreatableSelect.tsx` |
| `edge:adapter-selector` | AdapterSelect | Adapter selector | `src/extensions/datahub/components/forms` |

#### Deprecated/Unused Widgets

| Widget | Status | Notes |
|--------|--------|-------|
| `ToggleWidget` | ⚠️ Not in use | Implemented with tests but never registered or used |
| `AdapterTagSelect` | ⚠️ Not in use | Replaced by `discovery:tagBrowser` (which is also disabled - issue #24369) |
| `discovery:tagBrowser` | ⚠️ Disabled | Issue #24369 - Currently mapped to standard text input |

### Widget Registration

**Protocol Adapters:**
```typescript
// src/modules/ProtocolAdapters/utils/uiSchema.utils.ts
export const adapterJSFWidgets: RegistryWidgetsType = {
  'discovery:tagBrowser': 'text',  // Disabled
  'application/schema+json': JSONSchemaEditor,
}

// src/components/rjsf/Form/ChakraRJSForm.tsx
widgets={{
  ...(!showNativeWidgets && adapterJSFWidgets),
  UpDownWidget,
}}
```

**DataHub:**
```typescript
// src/extensions/datahub/designer/datahubRJSFWidgets.tsx
export const datahubRJSFWidgets: RegistryWidgetsType = {
  'application/schema+json': JSONSchemaEditor,
  'text/javascript': JavascriptEditor,
  'datahub:function-selector': FunctionCreatableSelect,
  // ... all DataHub widgets
}
```

### Widget Usage in UI Schema

```typescript
// Built-in widget (string)
{ 'ui:widget': 'updown' }

// Custom widget (component reference)
import ToggleWidget from '@/components/rjsf/Widgets/ToggleWidget.tsx'
{ 'ui:widget': ToggleWidget }

// DataHub widget (string key)
{ 'ui:widget': 'datahub:message-type' }
```

### Widget vs Field

**Use Widget When:**
- Replacing just the input component
- Don't need custom label/layout
- Simpler to implement

**Use Field When:**
- Need complete control over layout
- Custom label positioning
- Complex field groups
- Multiple inputs in one field

### FormContext for Cross-Field Dependencies

Widgets cannot access the full form data directly - they only receive their own field's value. To access other field values, use `formContext`:

```typescript
// Parent component
const [formData, setFormData] = useState(initialData)

return (
  <ChakraRJSForm
    formData={formData}
    formContext={{
      currentSchemaSource: formData.schemaSource,  // Pass live data
    }}
    onChange={(e) => setFormData(e.formData)}
  />
)
```

```typescript
// Custom widget
export const MyWidget: FC<WidgetProps> = (props) => {
  const { value, onChange, formContext } = props

  // Access shared data from context
  const context = formContext as MyContext
  const otherFieldValue = context?.currentSchemaSource

  return <Select value={value} onChange={onChange} />
}
```

**See:** [RJSF_WIDGET_DESIGN_AND_TESTING.md](../../.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md) for complete widget implementation patterns.

---

## Custom Fields

Fields replace entire field groups (label + input + error).

| Field | Component | Purpose | Registration | File |
|-------|-----------|---------|--------------|------|
| `compactTable` | CompactArrayField | Compact array display | `adapterJSFFields` | `src/components/rjsf/Fields/CompactArrayField.tsx` |
| `mqtt:transform` | MqttTransformationField | MQTT transformation editor | `adapterJSFFields` | `src/components/rjsf/Fields/MqttTransformationField.tsx` |
| N/A | InternalNotice | ⚠️ Not in use | Never imported | `src/components/rjsf/Fields/InternalNotice.tsx` |

### Field Registration

```typescript
// src/modules/ProtocolAdapters/utils/uiSchema.utils.ts
export const adapterJSFFields: RegistryFieldsType = {
  compactTable: CompactArrayField,
  'mqtt:transform': MqttTransformationField,
}
```

### Field Usage in UI Schema

```typescript
{
  myArrayField: {
    'ui:field': 'compactTable',  // Uses CompactArrayField
  }
}
```

---

## Custom Templates

Templates control the rendering of form structure elements.

| Template | File | Purpose |
|----------|------|---------|
| ArrayFieldTemplate | `src/components/rjsf/ArrayFieldTemplate.tsx` | Array field container |
| ArrayFieldItemTemplate | `src/components/rjsf/ArrayFieldItemTemplate.tsx` | Individual array items |
| ObjectFieldTemplate | `src/components/rjsf/ObjectFieldTemplate.tsx` | Object field container |
| FieldTemplate | `src/components/rjsf/FieldTemplate.tsx` | Standard field wrapper |
| BaseInputTemplate | `src/components/rjsf/BaseInputTemplate.tsx` | Input field wrapper |
| DescriptionFieldTemplate | `src/components/rjsf/Templates/DescriptionFieldTemplate.tsx` | Description rendering |
| TitleFieldTemplate | `src/components/rjsf/Templates/TitleFieldTemplate.tsx` | Title rendering |
| ErrorListTemplate | `src/components/rjsf/Templates/ErrorListTemplate.tsx` | Error list display |
| CompactArrayFieldTemplate | `src/components/rjsf/Templates/CompactArrayFieldTemplate.tsx` | Compact array container |
| CompactArrayFieldItemTemplate | `src/components/rjsf/Templates/CompactArrayFieldItemTemplate.tsx` | Compact array items |
| CompactObjectFieldTemplate | `src/components/rjsf/Templates/CompactObjectFieldTemplate.tsx` | Compact object container |

Templates are automatically registered in `ChakraRJSForm.tsx`.

---

## Validation

### Schema-Based Validation

Validation is automatic from JSON Schema constraints:

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
    },
    topic: {
      type: 'string',
      format: 'mqtt-topic', // Custom format validator
    },
  }
}
```

### Custom Format Validators

Located in `src/components/rjsf/Form/validation.utils.ts`:

| Format | Validator Function | Purpose | Status |
|--------|-------------------|---------|--------|
| `mqtt-topic` | `validationTopic()` | MQTT topic validation | ✅ Active |
| `mqtt-tag` | `validationTag()` | MQTT tag validation | ✅ Active |
| `mqtt-topic-filter` | `validationTopicFilter()` | MQTT topic filter validation | ✅ Active |
| `identifier` | Stub | Adapter ID validation | ✅ Active |
| `boolean` | Stub | Boolean hack | ✅ Active |
| `interpolation` | Stub | DataHub interpolation | ⚠️ Orphaned |
| `jwt` | `validationJWT()` | JWT token validation | ⚠️ Orphaned |
| `hostname` | **Missing** | Hostname/IP validation | ❌ Gap (F-M1) |

**Missing Validator - F-M1:**

`hostname` format is specified by backend in Modbus, EIP, and PLC4X adapters but frontend has no validator. See [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md) for details.

### Custom Validation Functions

```typescript
import type { RJSFValidationError } from '@rjsf/utils'

const customValidate = (formData: MyData, errors: RJSFValidationError) => {
  if (formData.field1 && formData.field2 === formData.field1) {
    errors.field2.addError('Must be different from field1')
  }
  return errors
}

<ChakraRJSForm
  schema={mySchema}
  customValidate={customValidate}
/>
```

---

## Component Integration

### Standard Pattern (Drawer with Form)

```typescript
import ChakraRJSForm from '@/components/rjsf/Form/ChakraRJSForm'
import { mySchema, myUISchema } from './schemas'

const MyDrawer: FC<Props> = ({ isOpen, onClose, initialData, onSubmit }) => {
  const handleSubmit = (event: IChangeEvent<MyData>) => {
    if (event.formData) {
      onSubmit(event.formData)
      onClose()
    }
  }

  return (
    <Drawer variant="hivemq" size="lg" isOpen={isOpen} onClose={onClose}>
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
            <Button onClick={onClose}>Cancel</Button>
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

1. **Form ID matching**: `<ChakraRJSForm id="my-form">` must match `<Button form="my-form">`
2. **Hide default submit**: `'ui:submitButtonOptions': { norender: true }` in UI schema
3. **Submit button location**: In DrawerFooter, not in form body
4. **Form wrapper**: Always wrap in `<Card><CardBody>` for consistent styling
5. **Drawer variant**: Use `variant="hivemq"` for consistent drawer styling
6. **Button variant**: Use `variant="primary"` for submit button

---

## Testing Patterns

### Component Test Pattern

```typescript
describe('MyComponent with RJSF', () => {
  it('should render form fields', () => {
    cy.mountWithProviders(<MyComponent />)

    // Access fields by label
    cy.get('label#root_fieldName-label').should('be.visible')
    cy.get('label#root_fieldName-label + div').type('value')
  })

  it('should submit form', () => {
    const onSubmitSpy = cy.spy().as('onSubmit')
    cy.mountWithProviders(<MyComponent onSubmit={onSubmitSpy} />)

    // Fill form
    cy.get('label#root_fieldName-label + div').type('value')

    // Submit
    cy.get('button[type="submit"]').click()

    // Verify
    cy.get('@onSubmit').should('have.been.calledOnce')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<MyComponent />)
    cy.checkAccessibility()
  })
})
```

### React-Select Testing Gotchas

When testing custom widgets using react-select:

```typescript
// ❌ Wrong - can't check input.value with react-select
cy.get('#root_type').should('have.value', 'JSON')

// ✅ Correct - check displayed text
cy.get('label#root_type-label + div').should('contain.text', 'JSON')

// ✅ Open dropdown and select option
cy.get('label#root_type-label + div').click()
cy.contains('[role="option"]', 'Option 1').click()
```

### Widget Implementation Checklist

When creating custom RJSF widgets with react-select:

- [ ] `FormControl` has `data-testid={id}`
- [ ] `FormLabel` has `id={`${id}-label`}`
- [ ] `FormLabel` has `htmlFor={id}`
- [ ] `CreatableSelect` has `id={`${id}-widget`}`
- [ ] `CreatableSelect` has `name={id}`
- [ ] `CreatableSelect` has `instanceId={id}`
- [ ] `CreatableSelect` has `inputId={id}`
- [ ] Handle `onChange` correctly
- [ ] Handle `disabled` and `readonly` props
- [ ] Handle `required` prop
- [ ] Handle `rawErrors` for validation display
- [ ] Widget registered in registry
- [ ] UISchema uses correct widget reference

**See:** [RJSF Widget Design & Testing Guidelines](../../.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md) for complete patterns.

---

## Common Issues & Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| **Submit button doesn't work** | Form ID doesn't match button's `form` attribute | Ensure `<ChakraRJSForm id="my-form">` matches `<Button form="my-form">` |
| **Two submit buttons appear** | Didn't hide default submit button | Add `'ui:submitButtonOptions': { norender: true }` to UI schema |
| **Conditional visibility doesn't work** | Wrong syntax | Use string expression: `'ui:options': { hidden: '{{formData.field === false}}' }` |
| **Widget doesn't apply** | Widget name vs component confusion | Built-in: `'ui:widget': 'updown'` (string). Custom: `'ui:widget': ToggleWidget` (component) |
| **Validation doesn't work** | Schema constraints wrong | Use correct properties: `minLength` (not `min`), `minimum` (for numbers) |
| **Enum shows values instead of labels** | Missing `ui:enumNames` | Add `'ui:enumNames': ['Label 1', 'Label 2']` to UI schema |
| **React-select value verification fails** | Can't check `input.value` | Check displayed text: `cy.get('label#root_field-label + div').should('contain.text', 'Value')` |
| **Field doesn't appear** | Conditional field not yet visible | Wait for field: `cy.get('label#root_field-label').should('be.visible')` |
| **Duplicate description text** | Both `description` and `ui:help` defined | Use only schema `description` OR `ui:description` to replace it |

---

## File Locations

| Component | File Path |
|-----------|-----------|
| **Main Form Component** | `src/components/rjsf/Form/ChakraRJSForm.tsx` |
| **Validation Utils** | `src/components/rjsf/Form/validation.utils.ts` |
| **Widget Directory** | `src/components/rjsf/Widgets/` |
| **Field Directory** | `src/components/rjsf/Fields/` |
| **Template Directory** | `src/components/rjsf/Templates/` |
| **API Schemas** | `src/api/schemas/` |
| **Workspace Schemas** | `src/modules/Workspace/schemas/` |
| **DataHub Widgets** | `src/extensions/datahub/designer/datahubRJSFWidgets.tsx` |
| **DataHub Forms** | `src/extensions/datahub/components/forms/` |
| **Adapter Widget Registry** | `src/modules/ProtocolAdapters/utils/uiSchema.utils.ts` |
| **Test Utilities** | `src/__test-utils__/rjsf/` |
| **Adapter Mocks** | `src/__test-utils__/adapters/` |

---

## Related Documentation

**Architecture:**
- [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md) - Backend-driven adapter configuration
- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md) - DataHub policy designer

**Guides:**
- [Testing Guide](./TESTING_GUIDE.md) - General testing patterns
- [Cypress Guide](./CYPRESS_GUIDE.md) - Cypress-specific patterns
- [Design Guide](./DESIGN_GUIDE.md) - UI component patterns

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md) - Dependencies: @rjsf/chakra-ui, validator-ajv8

**External:**
- [RJSF Official Docs](https://rjsf-team.github.io/react-jsonschema-form/)
- [JSON Schema Spec](https://json-schema.org/)
- [Chakra UI Docs](https://chakra-ui.com/)

---

**Last Updated:** 2026-02-16
