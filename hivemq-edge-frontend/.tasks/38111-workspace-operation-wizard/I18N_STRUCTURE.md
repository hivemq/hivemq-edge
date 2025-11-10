# Workspace Wizard - i18n Translation Structure

**Task:** 38111-workspace-operation-wizard  
**Created:** November 10, 2025

---

## Translation Keys to Add

These keys should be added to `src/locales/en/translation.json` under the `workspace` object.

### Complete JSON Structure

```json
{
  "workspace": {
    "wizard": {
      "trigger": {
        "buttonLabel": "Create New",
        "buttonAriaLabel": "Create new entity or integration point",
        "menuTitle": "What would you like to create?"
      },
      "category": {
        "entities": "Entities",
        "integrationPoints": "Integration Points"
      },
      "entityType": {
        "name_ADAPTER": "Adapter",
        "name_BRIDGE": "Bridge",
        "name_COMBINER": "Combiner",
        "name_ASSET_MAPPER": "Asset Mapper",
        "name_GROUP": "Group",
        "name_TAG": "Tags",
        "name_TOPIC_FILTER": "Topic Filters",
        "name_DATA_MAPPING_NORTH": "Data Mapping (Northbound)",
        "name_DATA_MAPPING_SOUTH": "Data Mapping (Southbound)",
        "name_DATA_COMBINING": "Data Combining",

        "description_ADAPTER": "Connect to devices using specific protocols",
        "description_BRIDGE": "Connect to another MQTT broker",
        "description_COMBINER": "Merge data from multiple sources",
        "description_ASSET_MAPPER": "Map data to HiveMQ Pulse assets",
        "description_GROUP": "Group nodes logically",
        "description_TAG": "Add tags to a device node",
        "description_TOPIC_FILTER": "Configure topic filters on Edge Broker",
        "description_DATA_MAPPING_NORTH": "Map device data to MQTT topics",
        "description_DATA_MAPPING_SOUTH": "Map MQTT topics to device commands",
        "description_DATA_COMBINING": "Configure data combining logic"
      },
      "progress": {
        "stepCounter": "Step {{current}} of {{total}}",
        "cancel": "Cancel Wizard",
        "ariaLabel": "Wizard progress",

        "step_ADAPTER_0": "Review adapter preview",
        "step_ADAPTER_1": "Select protocol type",
        "step_ADAPTER_2": "Configure adapter settings",

        "step_BRIDGE_0": "Review bridge preview",
        "step_BRIDGE_1": "Configure bridge settings",

        "step_COMBINER_0": "Select data sources",
        "step_COMBINER_1": "Review combiner preview",
        "step_COMBINER_2": "Configure combining logic",

        "step_ASSET_MAPPER_0": "Select data sources and Pulse Agent",
        "step_ASSET_MAPPER_1": "Review asset mapper preview",
        "step_ASSET_MAPPER_2": "Configure asset mappings",

        "step_GROUP_0": "Select nodes to group",
        "step_GROUP_1": "Review group preview",
        "step_GROUP_2": "Configure group settings",

        "step_TAG_0": "Select device node",
        "step_TAG_1": "Configure tags",

        "step_TOPIC_FILTER_0": "Select Edge Broker",
        "step_TOPIC_FILTER_1": "Configure topic filters",

        "step_DATA_MAPPING_NORTH_0": "Select adapter",
        "step_DATA_MAPPING_NORTH_1": "Configure northbound mappings",

        "step_DATA_MAPPING_SOUTH_0": "Select adapter",
        "step_DATA_MAPPING_SOUTH_1": "Configure southbound mappings",

        "step_DATA_COMBINING_0": "Select combiner",
        "step_DATA_COMBINING_1": "Configure combining logic"
      },
      "selection": {
        "instruction": "Click to select {{nodeType}}",
        "instructionMulti": "Select {{min}} to {{max}} nodes",
        "instructionMinOnly": "Select at least {{min}} node",
        "instructionMinOnly_plural": "Select at least {{min}} nodes",
        "selected": "{{count}} selected",
        "required": "{{nodeType}} is required",
        "cannotSelect": "This node cannot be selected",
        "cancel": "Cancel Selection",
        "proceed": "Continue",
        "clear": "Clear Selection"
      },
      "ghost": {
        "label": "Preview",
        "ariaLabel": "Preview of {{entityType}} being created",
        "tooltip": "This is a preview. Complete the wizard to create the actual entity."
      },
      "errors": {
        "apiError": "Failed to create {{entityType}}",
        "apiErrorWithReason": "Failed to create {{entityType}}: {{reason}}",
        "validationError": "Please fix the validation errors before proceeding",
        "selectionRequired": "Please select at least {{count}} node",
        "selectionRequired_plural": "Please select at least {{count}} nodes",
        "pulseAgentRequired": "Pulse Agent node must be selected for Asset Mapper",
        "noSelectableNodes": "No nodes available for selection",
        "configurationIncomplete": "Please complete the configuration",
        "unknownError": "An unknown error occurred"
      },
      "confirmation": {
        "cancelTitle": "Cancel Wizard?",
        "cancelMessage": "You have unsaved changes. Are you sure you want to cancel?",
        "cancelConfirm": "Yes, Cancel",
        "cancelAbort": "Continue Editing"
      },
      "success": {
        "created": "{{entityType}} created successfully",
        "updated": "{{entityType}} updated successfully"
      },
      "steps": {
        "preview": {
          "title": "Preview",
          "description": "Review the entities that will be created",
          "proceed": "Continue to Configuration"
        },
        "selection": {
          "title": "Selection",
          "helpText": "Click on nodes in the workspace to select them"
        },
        "configuration": {
          "title": "Configuration",
          "description": "Configure the {{entityType}} settings"
        }
      }
    }
  }
}
```

---

## Usage Examples

### Entity Type Names

```typescript
// In component
const { t } = useTranslation()

// ✅ CORRECT - Plain string key with context
const name = t('workspace.wizard.entityType.name', { context: EntityType.ADAPTER })
// Result: "Adapter"

const description = t('workspace.wizard.entityType.description', { context: EntityType.ADAPTER })
// Result: "Connect to devices using specific protocols"
```

### Progress Steps

```typescript
// In WizardProgressBar component
const { t } = useTranslation()

const stepDescription = t('workspace.wizard.progress.step', {
  context: `${entityType}_${currentStep}`,
})
// Example: context = "ADAPTER_1"
// Result: "Select protocol type"

const stepCounter = t('workspace.wizard.progress.stepCounter', {
  current: 2,
  total: 3,
})
// Result: "Step 2 of 3"
```

### Selection Instructions

```typescript
// In SelectionStep component
const { t } = useTranslation()

const instruction = t('workspace.wizard.selection.instruction', {
  nodeType: 'device',
})
// Result: "Click to select device"

const multiInstruction = t('workspace.wizard.selection.instructionMulti', {
  min: 2,
  max: 5,
})
// Result: "Select 2 to 5 nodes"

// With pluralization
const minInstruction = t('workspace.wizard.selection.instructionMinOnly', {
  count: 3,
})
// Result: "Select at least 3 nodes"
```

### Error Messages

```typescript
// In error handling
const { t } = useTranslation()

const errorMessage = t('workspace.wizard.errors.apiError', {
  entityType: 'Adapter',
})
// Result: "Failed to create Adapter"

const selectionError = t('workspace.wizard.errors.selectionRequired', {
  count: 2,
})
// Result: "Please select at least 2 nodes"
```

### Ghost Node Labels

```typescript
// In GhostNode component
const { t } = useTranslation()

const ariaLabel = t('workspace.wizard.ghost.ariaLabel', {
  entityType: 'Adapter',
})
// Result: "Preview of Adapter being created"
```

---

## Context Values Reference

### Entity Types

- `ADAPTER`
- `BRIDGE`
- `COMBINER`
- `ASSET_MAPPER`
- `GROUP`
- `TAG`
- `TOPIC_FILTER`
- `DATA_MAPPING_NORTH`
- `DATA_MAPPING_SOUTH`
- `DATA_COMBINING`

### Step Context Pattern

Format: `{ENTITY_TYPE}_{STEP_NUMBER}`

Examples:

- `ADAPTER_0` → "Review adapter preview"
- `ADAPTER_1` → "Select protocol type"
- `ADAPTER_2` → "Configure adapter settings"
- `COMBINER_0` → "Select data sources"
- `TAG_0` → "Select device node"

---

## Adding to Existing Translation File

The wizard translations should be added under the existing `workspace` object at approximately line 848.

**Before:**

```json
{
  "workspace": {
    "canvas": { ... },
    "controls": { ... },
    "autoLayout": { ... },
    "configuration": { ... }
  }
}
```

**After:**

```json
{
  "workspace": {
    "canvas": { ... },
    "controls": { ... },
    "autoLayout": { ... },
    "configuration": { ... },
    "wizard": {
      // Add all wizard translations here
    }
  }
}
```

---

## Validation Checklist

- [ ] All keys are plain strings (no template literals)
- [ ] Context values match enum values exactly
- [ ] Pluralization keys have `_plural` suffix where needed
- [ ] All entity types have name and description translations
- [ ] All wizard steps have progress descriptions
- [ ] Error messages are clear and actionable
- [ ] ARIA labels provided for accessibility
- [ ] No duplicate keys
- [ ] JSON is valid (no trailing commas)

---

## Testing Translation Keys

```typescript
// Test all entity type translations exist
Object.values(EntityType).forEach((type) => {
  const name = t('workspace.wizard.entityType.name', { context: type })
  const description = t('workspace.wizard.entityType.description', { context: type })

  expect(name).not.toContain('workspace.wizard')
  expect(description).not.toContain('workspace.wizard')
})

// Test step translations exist
const testSteps = [
  'ADAPTER_0',
  'ADAPTER_1',
  'ADAPTER_2',
  'BRIDGE_0',
  'BRIDGE_1',
  'COMBINER_0',
  'COMBINER_1',
  'COMBINER_2',
]

testSteps.forEach((stepKey) => {
  const description = t('workspace.wizard.progress.step', { context: stepKey })
  expect(description).not.toContain('workspace.wizard')
})
```

---

## Future Additions

When adding new entity types or wizard steps:

1. Add new context keys to translation file:

   ```json
   "name_NEW_ENTITY": "New Entity Name",
   "description_NEW_ENTITY": "New entity description",
   "step_NEW_ENTITY_0": "Step description"
   ```

2. Update enum values in code:

   ```typescript
   enum EntityType {
     // Existing types...
     NEW_ENTITY = 'NEW_ENTITY',
   }
   ```

3. Update metadata registry:

   ```typescript
   [EntityType.NEW_ENTITY]: {
     type: EntityType.NEW_ENTITY,
     icon: LuNewIcon,
     category: 'entity',
     // ...
   }
   ```

4. Usage remains the same:
   ```typescript
   t('workspace.wizard.entityType.name', { context: EntityType.NEW_ENTITY })
   ```

---

**Notes:**

- Keep all translation keys alphabetically organized within sections
- Maintain consistent naming patterns
- Document any non-obvious context values
- Test translations in multiple locales if/when adding i18n support
