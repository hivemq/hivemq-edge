# RJSF (React JSON Schema Form) Migration Analysis

## Current State

a

### Package Versions

```json
{
  "@rjsf/chakra-ui": "5.24.13",
  "@rjsf/core": "5.24.13",
  "@rjsf/utils": "5.24.13",
  "@rjsf/validator-ajv8": "5.24.13"
}
```

### RJSF Usage Locations

The application uses RJSF for dynamic form generation in multiple critical areas:

| Feature Area      | Form Usage                  |
| :---------------- | :-------------------------- |
| Protocol Adapters | Adapter configuration forms |
| Bridges           | Bridge connection settings  |
| DataHub Policies  | Policy property panels      |
| Mappings          | Mapping configuration       |
| Combiners         | Combiner settings           |
| Topic Filters     | Topic filter forms          |
| Pulse Assets      | Asset management forms      |
| Workspace Wizard  | Entity creation forms       |

---

## Custom Components Inventory

### Location: `src/components/rjsf/`

**Total Files**: \~100+  
**Custom Templates**: 13  
**Custom Widgets**: 8+  
**Custom Fields**: 5+  
**Supporting Utilities**: 15+  
**Tests**: 30+

---

## Templates

### Standard Templates (Root Level)

| File                         | Lines | Purpose                  | Chakra Components                        |
| :--------------------------- | :---- | :----------------------- | :--------------------------------------- |
| `ArrayFieldTemplate.tsx`     | \~150 | Array field container    | Box, Stack, IconButton                   |
| `ArrayFieldItemTemplate.tsx` | \~100 | Array item with actions  | Box, Flex, IconButton                    |
| `BaseInputTemplate.tsx`      | \~80  | Input wrapper            | FormControl, Input                       |
| `FieldTemplate.tsx`          | \~120 | Field wrapper with label | FormControl, FormLabel, FormErrorMessage |
| `ObjectFieldTemplate.tsx`    | \~100 | Object field layout      | Box, Grid, Stack                         |

### Compact Templates (`Templates/`)

| File                                | Lines | Purpose            | Chakra Components |
| :---------------------------------- | :---- | :----------------- | :---------------- |
| `CompactArrayFieldTemplate.tsx`     | \~100 | Compact array      | Box, Stack        |
| `CompactArrayFieldItemTemplate.tsx` | \~80  | Compact array item | Flex, IconButton  |
| `CompactBaseInputTemplate.tsx`      | \~60  | Compact input      | Input             |
| `CompactFieldTemplate.tsx`          | \~80  | Compact field      | FormControl       |
| `CompactObjectFieldTemplate.tsx`    | \~90  | Compact object     | Box, Stack        |
| `DescriptionFieldTemplate.tsx`      | \~40  | Description text   | Text              |
| `ErrorListTemplate.tsx`             | \~50  | Error list display | Alert, List       |
| `TitleFieldTemplate.tsx`            | \~35  | Title display      | Heading           |

### Template Index

```ts
// Templates/index.ts exports all templates for easy import
export * from './CompactArrayFieldTemplate'
export * from './CompactArrayFieldItemTemplate'
// ... etc
```

---

## Custom Widgets

### Location: `src/components/rjsf/Widgets/`

| Widget                   | Purpose                   | Chakra Components        | Test File         |
| :----------------------- | :------------------------ | :----------------------- | :---------------- |
| `AdapterTagSelect.tsx`   | Tree-based tag selection  | Tree view, Modal, Select | ✅ `.spec.cy.tsx` |
| `EntitySelectWidget.tsx` | Entity selection dropdown | Select, Modal            | \-                |
| `SchemaWidget.tsx`       | Schema selection          | Select, Button, Modal    | ✅ `.spec.cy.tsx` |
| `ToggleWidget.tsx`       | Boolean toggle            | Switch                   | ✅ `.spec.cy.tsx` |
| `UpDownWidget.tsx`       | Number input with arrows  | NumberInput              | \-                |

### Widget Utilities

```
Widgets/utils/
├── treeview.utils.ts       # Tree view helpers
└── treeview.utils.spec.ts  # Unit tests
```

---

## Custom Fields

### Location: `src/components/rjsf/Fields/`

| Field                         | Purpose                    | Complexity |
| :---------------------------- | :------------------------- | :--------- |
| `CompactArrayField.tsx`       | Compact array handling     | Medium     |
| `InternalNotice.tsx`          | Display internal notices   | Low        |
| `MqttTransformationField.tsx` | MQTT transformation editor | High       |

### Field Index

```ts
// Fields/index.ts
export * from './CompactArrayField'
export * from './InternalNotice'
export * from './MqttTransformationField'
```

---

## Internal Components

### Location: `src/components/rjsf/__internals/`

| Component                 | Purpose                     | Chakra Components  |
| :------------------------ | :-------------------------- | :----------------- |
| `AddButton.tsx`           | Add array item button       | IconButton         |
| `ChakraIconButton.tsx`    | Generic icon button         | IconButton         |
| `IconButton.tsx`          | Icon button abstraction     | IconButton         |
| `RenderFieldTemplate.tsx` | Field template renderer     | \-                 |
| `TopicInputTemplate.tsx`  | Topic input with validation | Input, FormControl |

---

## Form Infrastructure

### Location: `src/components/rjsf/Form/`

| File                     | Purpose                      |
| :----------------------- | :--------------------------- |
| `ChakraRJSForm.tsx`      | Main form component wrapper  |
| `error-focus.utils.ts`   | Error focus handling         |
| `types.ts`               | Type definitions             |
| `useFormControlStore.ts` | Zustand store for form state |
| `validation.utils.ts`    | Validation helpers           |

### Main Form Usage

```ts
// src/components/rjsf/Form/ChakraRJSForm.tsx
import Form from '@rjsf/chakra-ui'

export const ChakraRJSForm = ({
  schema,
  uiSchema,
  formData,
  ...props
}) => {
  return (
    <Form
      schema={schema}
      uiSchema={uiSchema}
      formData={formData}
      templates={customTemplates}
      widgets={customWidgets}
      fields={customFields}
      {...props}
    />
  )
}
```

---

## Complex Feature Components

### Batch Mode Mappings (`BatchModeMappings/`)

**Purpose**: Bulk upload and mapping of adapter configurations via CSV/Excel

| File                                    | Purpose                 |
| :-------------------------------------- | :---------------------- |
| `BatchUploadButton.tsx`                 | Upload trigger          |
| `components/ColumnMatcherStep.tsx`      | Column mapping wizard   |
| `components/ConfirmStep.tsx`            | Confirmation step       |
| `components/DataSourceStep.tsx`         | Data source selection   |
| `components/MappingsValidationStep.tsx` | Validation display      |
| `components/UploadStepper.tsx`          | Multi-step wizard       |
| `hooks/useBatchModeSteps.ts`            | Step state management   |
| `utils/config.utils.ts`                 | Configuration utilities |
| `utils/dropzone.utils.ts`               | File upload handling    |
| `utils/levenshtein.utils.ts`            | String matching         |
| `utils/rolling-hash.utils.ts`           | Hash utilities          |

**Chakra Components Used**: Modal, Stepper, Table, Button, Select, Alert

### MQTT Transformation (`MqttTransformation/`)

**Purpose**: Visual mapping between MQTT payload schemas

| File                                        | Purpose                |
| :------------------------------------------ | :--------------------- |
| `JsonSchemaBrowser.tsx`                     | Schema tree browser    |
| `components/DataModelDestination.tsx`       | Destination schema     |
| `components/DataModelSources.tsx`           | Source schema          |
| `components/EntitySelector.tsx`             | Entity picker          |
| `components/ListMappings.tsx`               | Mapping list           |
| `components/MappingContainer.tsx`           | Main container         |
| `components/MappingDrawer.tsx`              | Mapping details drawer |
| `components/MappingEditor.tsx`              | Mapping editor         |
| `components/MappingInstructionList.tsx`     | Instructions list      |
| `components/mapping/MappingInstruction.tsx` | Single instruction     |
| `components/mapping/ValidationStatus.tsx`   | Validation indicator   |
| `components/schema/PropertyItem.tsx`        | Schema property item   |

**Chakra Components Used**: Drawer, Tree, Box, Flex, Button, Badge, Tooltip

### Split Array Editor (`SplitArrayEditor/`)

| File                             | Purpose                        |
| :------------------------------- | :----------------------------- |
| `components/ArrayItemDrawer.tsx` | Drawer for editing array items |

---

## DataHub-Specific RJSF Components

### Location: `src/extensions/datahub/`

| File                                                | Purpose                    | Complexity |
| :-------------------------------------------------- | :------------------------- | :--------- |
| `designer/datahubRJSFWidgets.tsx`                   | DataHub widget overrides   | Medium     |
| `components/forms/ReactFlowSchemaForm.tsx`          | React Flow integrated form | High       |
| `components/forms/CodeEditor.tsx`                   | Monaco code editor widget  | High       |
| `components/forms/AdapterSelect.tsx`                | Adapter dropdown           | Low        |
| `components/forms/FunctionCreatableSelect.tsx`      | Function picker            | Medium     |
| `components/forms/MessageInterpolationTextArea.tsx` | Interpolation editor       | High       |
| `components/forms/MessageTypeSelect.tsx`            | Message type picker        | Low        |
| `components/forms/MetricCounterInput.tsx`           | Metric counter             | Low        |
| `components/forms/ResourceNameCreatableSelect.tsx`  | Resource picker            | Medium     |
| `components/forms/TransitionSelect.tsx`             | FSM transition picker      | Medium     |
| `components/forms/VersionManagerSelect.tsx`         | Version selector           | Low        |

---

## Test Coverage

### Component Tests (Cypress)

| Location                                                   | Test Files |
| :--------------------------------------------------------- | :--------- |
| `src/components/rjsf/**/*.spec.cy.tsx`                     | \~30       |
| `src/extensions/datahub/components/forms/**/*.spec.cy.tsx` | \~15       |

### Unit Tests (Vitest)

| Location                                              | Test Files |
| :---------------------------------------------------- | :--------- |
| `src/components/rjsf/Form/*.spec.ts`                  | 4          |
| `src/components/rjsf/utils/*.spec.ts`                 | 2          |
| `src/components/rjsf/BatchModeMappings/**/*.spec.ts`  | 4          |
| `src/components/rjsf/MqttTransformation/**/*.spec.ts` | 2          |

---

## Chakra UI v3 Impact Analysis

### Breaking Changes Expected

1. **Component API Changes**:

- Props renaming/restructuring
- Styling approach changes (CSS-in-JS to CSS variables)
- Color mode handling

2. **Theme Token Changes**:

- Color tokens
- Spacing tokens
- Component variants

3. **Form Component Changes**:

- FormControl patterns
- Input styling
- Error message display

### High-Risk Components

| Component           | Risk Level | Reason                        |
| :------------------ | :--------- | :---------------------------- |
| All templates       | High       | Direct Chakra component usage |
| AdapterTagSelect    | High       | Complex tree \+ modal         |
| BatchModeMappings   | High       | Complex multi-step wizard     |
| MqttTransformation  | High       | Complex nested components     |
| CodeEditor (Monaco) | Medium     | Monaco \+ Chakra integration  |

### Dependency on @rjsf/chakra-ui

The `@rjsf/chakra-ui` package must release a Chakra v3 compatible version before migration can proceed. Current status:

- **RJSF GitHub Issues**: Monitor for v3 support discussions
- **Expected Timeline**: Unknown \- depends on RJSF team

---

## Migration Strategy

### Pre-Migration Requirements

- [ ] Wait for `@rjsf/chakra-ui` v3 release
- [ ] Or fork and update `@rjsf/chakra-ui` for v3

### Phase 1: Core Templates (Week 1-2)

1. Update `FieldTemplate.tsx`
2. Update `BaseInputTemplate.tsx`
3. Update `ArrayFieldTemplate.tsx`
4. Update `ObjectFieldTemplate.tsx`
5. Update internal components

### Phase 2: Compact Templates (Week 2-3)

1. Update all `Compact*` templates
2. Update `DescriptionFieldTemplate`
3. Update `ErrorListTemplate`
4. Update `TitleFieldTemplate`

### Phase 3: Widgets (Week 3-4)

1. Update `ToggleWidget`
2. Update `UpDownWidget`
3. Update `SchemaWidget`
4. Update `EntitySelectWidget`
5. Update `AdapterTagSelect` (complex)

### Phase 4: Complex Features (Week 4-6)

1. Update BatchModeMappings
2. Update MqttTransformation
3. Update SplitArrayEditor

### Phase 5: DataHub Components (Week 6-7)

1. Update DataHub form components
2. Update Monaco integration
3. Update policy forms

### Phase 6: Testing & Validation (Week 7-8)

1. Run all component tests
2. Run all E2E tests
3. Manual testing of forms
4. Fix regressions

---

## Recommendations

1. **Start with Theme Audit**: Document all Chakra tokens used in RJSF components
2. **Create Component Mapping**: Map v2 components/props to v3 equivalents
3. **Test in Isolation**: Create test harness for RJSF components
4. **Gradual Migration**: Use feature flags if needed
5. **Maintain Parallel Versions**: Keep v2 components until v3 stable

---

## References

- [RJSF Documentation](https://rjsf-team.github.io/react-jsonschema-form/)
- [RJSF Chakra UI Theme](https://rjsf-team.github.io/react-jsonschema-form/docs/usage/themes/chakra-ui)
- [Chakra UI v3 Migration Guide](https://www.chakra-ui.com/docs/get-started/migration) (when available)
- [Project RJSF Guidelines](http://./.tasks/RJSF_GUIDELINES.md)
- [RJSF Widget Design](http://./.tasks/RJSF_WIDGET_DESIGN_AND_TESTING.md)
