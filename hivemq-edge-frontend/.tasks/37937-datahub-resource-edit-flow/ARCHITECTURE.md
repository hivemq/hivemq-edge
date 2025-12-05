# Task 37937: Architecture & Design Decisions

**Created:** November 26, 2025  
**Last Updated:** November 26, 2025  
**Version:** 1.1  
**Status:** Under Review - API Verification Needed

---

## Document Management

### Update Strategy

**This document will be updated in-place as understanding evolves. No new documents will be created for corrections.**

**Version History:**

- v1.0 (Nov 26, 2025): Initial architecture draft
- v1.1 (Nov 26, 2025): Corrected API assumptions, aligned testing strategy

**When to Update:**

- API contract verification reveals different structure
- Implementation discovers unforeseen constraints
- Testing uncovers architectural issues
- Design decisions need revision

**How to Update:**

1. Update the relevant section(s)
2. Increment version number
3. Add entry to version history
4. Update "Last Updated" date
5. Note changes in TASK_SUMMARY.md session logs

**No document sprawl** - This single ARCHITECTURE.md is the source of truth.

---

## Overview

This document captures the architectural decisions, design patterns, and technical approach for refactoring the DataHub resource management system.

**⚠️ IMPORTANT**: Some assumptions in this document require verification during implementation (see API Contracts section).

---

## Current Architecture Problems

### Problem 1: Mixed Concerns

**SchemaPanel** and **FunctionPanel** currently handle:

1. Resource lifecycle (create, edit, version)
2. Node configuration (select which resource to use)
3. State synchronization (complex programmatic update flags)
4. Type/version switching (cascading onChange events)

**Result**: 200+ LOC components with complex conditional logic

```typescript
// Current SchemaPanel - Complex state management
const isProgrammaticUpdateRef = useRef(false)

const onReactFlowSchemaFormChange = (changeEvent, id) => {
  // Ignore programmatic updates to prevent cascade
  if (isProgrammaticUpdateRef.current) {
    isProgrammaticUpdateRef.current = false
    return
  }

  // Handle name change - load or create
  if (id?.includes('name')) {
    const schema = allSchemas?.items?.findLast(...)
    if (schema) {
      isProgrammaticUpdateRef.current = true
      setFormData({ /* complex state */ })
      queueMicrotask(() => {
        isProgrammaticUpdateRef.current = false
      })
    } else {
      // Create new draft...
    }
  }

  // Handle type change - update schema source
  if (id?.includes('type')) {
    // More complex logic...
  }

  // Handle version change...
}
```

**Issues**:

- Hard to understand and maintain
- Error-prone (race conditions in state updates)
- Violates single responsibility principle
- Difficult to test in isolation

---

### Problem 2: Unclear User Mental Model

**Current Flow** (confusing):

```
1. Open policy designer
2. Add schema node
3. Configure node → suddenly creating/editing schemas
4. Switch between creating new vs. selecting existing
5. Manage versions within policy context
```

Users don't know if they're:

- Designing a policy?
- Managing schemas?
- Both at the same time?

**Desired Flow** (clear):

```
1. Go to DataHub → Schemas tab
2. Create/edit schemas (clear resource management context)
3. Go to policy designer
4. Select existing schema by name + version (clear policy context)
```

---

### Problem 3: Coupled Publishing

**Current**: Policy publish also publishes resources

```typescript
// ToolbarPublish.tsx
const onPublish = async () => {
  // 1. Extract resources from dry-run report
  const resources = extractResources(report)

  // 2. Publish resources first
  for (const schema of resources.schemas) {
    await createSchema(schema)
  }
  for (const script of resources.scripts) {
    await createScript(script)
  }

  // 3. Finally publish policy
  await createPolicy(policy)
}
```

**Issues**:

- Complex error handling (partial failures)
- Resources created even if policy publish fails
- Hard to reason about transaction boundaries
- Unnecessary coupling

---

## Target Architecture

### Separation of Concerns

```
┌─────────────────────────────────────────────────────────┐
│                   DataHub Main Page                     │
│                                                         │
│  Schemas Tab                    Scripts Tab            │
│  ├─ [+ Create New Schema]       ├─ [+ Create New Script]│
│  ├─ Schema Table                ├─ Script Table        │
│  │  └─ [⋮ Edit] per row         │  └─ [⋮ Edit] per row │
│  └─ SchemaEditor (drawer)       └─ ScriptEditor (drawer)│
│                                                         │
│  Responsibility: Resource CRUD (Create, Read, Update, Delete)
└─────────────────────────────────────────────────────────┘

                         ▼

┌─────────────────────────────────────────────────────────┐
│                  Policy Designer                        │
│                                                         │
│  SchemaNode                     FunctionNode           │
│  ├─ SchemaPanelSimplified       ├─ FunctionPanelSimplified
│  │  └─ ResourceSelector         │  └─ ResourceSelector │
│  │     ├─ Name: [dropdown]      │     ├─ Name: [dropdown]
│  │     └─ Version: [dropdown]   │     └─ Version: [dropdown]
│                                                         │
│  Responsibility: Resource Selection (reference by name + version)
└─────────────────────────────────────────────────────────┘

                         ▼

┌─────────────────────────────────────────────────────────┐
│                  Policy Publishing                      │
│                                                         │
│  ToolbarPublish                                         │
│  ├─ Validate resource references exist                 │
│  ├─ Publish policy only (not resources)                │
│  └─ Policy references resources by name + version      │
│                                                         │
│  Responsibility: Policy lifecycle (validate, publish)   │
└─────────────────────────────────────────────────────────┘
```

---

## Component Design

### 1. ResourceEditorDrawer (Base Component)

**Purpose**: Reusable drawer for editing any resource type

**Props**:

```typescript
interface ResourceEditorDrawerProps<T> {
  isOpen: boolean
  onClose: () => void
  resourceType: 'schema' | 'script'
  initialData?: T
  onSave: (data: T) => Promise<void>
  jsonSchema: JSONSchema7
  uiSchema?: UiSchema
}
```

**Responsibilities**:

- Render RJSF form with provided schema
- Handle save/cancel actions
- Display loading and error states
- Accessibility (keyboard nav, ARIA)

**NOT Responsible For**:

- Business logic (handled by specific editors)
- API calls (handled by hooks)
- Validation logic (handled by JSON Schema)

**Usage**:

```typescript
<ResourceEditorDrawer
  isOpen={isOpen}
  onClose={onClose}
  resourceType="schema"
  jsonSchema={MOCK_SCHEMA_SCHEMA}
  onSave={handleSave}
/>
```

---

### 2. SchemaEditor & ScriptEditor

**Purpose**: Schema/Script-specific editors extending base drawer

**Responsibilities**:

- Provide resource-specific JSON Schema
- Handle resource-specific validation (JSON parse, Protobuf parse, JS syntax)
- Manage resource-specific state (type switching for schemas)
- Call appropriate API mutations

**Example** (SchemaEditor):

```typescript
export const SchemaEditor: FC<SchemaEditorProps> = ({
  isOpen,
  onClose,
  schemaId // undefined = create new, defined = edit existing
}) => {
  const { data: schema } = useGetSchema(schemaId)
  const createMutation = useCreateSchema()
  const updateMutation = useUpdateSchema()

  const handleSave = async (formData: SchemaData) => {
    // Validate schema definition
    if (formData.type === 'JSON') {
      JSON.parse(formData.schemaSource) // throws if invalid
    } else {
      parse(formData.schemaSource) // Protobuf validation
    }

    // Save
    if (schemaId) {
      await updateMutation.mutateAsync({ id: schemaId, data: formData })
    } else {
      await createMutation.mutateAsync(formData)
    }
  }

  return (
    <ResourceEditorDrawer
      isOpen={isOpen}
      onClose={onClose}
      resourceType="schema"
      initialData={schema}
      jsonSchema={MOCK_SCHEMA_SCHEMA}
      uiSchema={getSchemaUISchema(schema)}
      onSave={handleSave}
    />
  )
}
```

---

### 3. ResourceSelector

**Purpose**: Reusable two-stage selector (name → version)

**Props**:

```typescript
interface ResourceSelectorProps {
  resourceType: 'schema' | 'script'
  value: { name: string; version: number } | undefined
  onChange: (value: { name: string; version: number }) => void
  resources: Array<{ id: string; version: number }>
}
```

**UI Flow**:

```
Step 1: Select Name
┌──────────────────────────┐
│ Schema Name: [dropdown]  │
│   - temperature_schema   │
│   - humidity_schema      │
│   - pressure_schema      │
└──────────────────────────┘

Step 2: Select Version (after name selected)
┌──────────────────────────┐
│ Version: [dropdown]      │
│   - 3 (latest)           │
│   - 2                    │
│   - 1                    │
└──────────────────────────┘

Current Selection
┌──────────────────────────┐
│ temperature_schema (v3)  │
│ [✕ Clear Selection]      │
└──────────────────────────┘
```

**State Management**:

```typescript
const [selectedName, setSelectedName] = useState<string>()
const [selectedVersion, setSelectedVersion] = useState<number>()

// When name changes, load versions for that name
useEffect(() => {
  if (selectedName) {
    const versions = resources.filter((r) => r.id === selectedName).map((r) => r.version)
    setAvailableVersions(versions)
  }
}, [selectedName, resources])

// When both selected, call onChange
useEffect(() => {
  if (selectedName && selectedVersion) {
    onChange({ name: selectedName, version: selectedVersion })
  }
}, [selectedName, selectedVersion])
```

---

### 4. Simplified Panels

**Purpose**: Minimal panels for resource selection only

**Comparison**:

| Aspect           | Old Panel                        | New Panel              |
| ---------------- | -------------------------------- | ---------------------- |
| LOC              | ~200                             | <100                   |
| Responsibilities | Resource CRUD + Selection        | Selection only         |
| State complexity | High (programmatic refs)         | Low (simple selection) |
| Form logic       | Complex (type/version switching) | Simple (dropdowns)     |
| Error handling   | Multiple paths                   | Single path            |

**Example** (SchemaPanelSimplified):

```typescript
export const SchemaPanelSimplified: FC<PanelProps> = ({
  selectedNode,
  onFormSubmit,
  onFormError
}) => {
  const { data: allSchemas } = useGetAllSchemas()
  const { guardAlert, isNodeEditable } = usePolicyGuards(selectedNode)
  const [selectedResource, setSelectedResource] = useState<ResourceReference>()

  const handleResourceChange = (resource: ResourceReference) => {
    setSelectedResource(resource)

    // Update node data
    onFormSubmit?.({
      ...selectedNode.data,
      name: resource.name,
      version: resource.version
    })
  }

  if (!isNodeEditable) {
    return guardAlert
  }

  return (
    <Card>
      <CardBody>
        <ResourceSelector
          resourceType="schema"
          value={selectedResource}
          onChange={handleResourceChange}
          resources={allSchemas?.items || []}
        />
      </CardBody>
    </Card>
  )
}
```

**Key Simplifications**:

- ✅ No programmatic update refs
- ✅ No complex onChange cascades
- ✅ No type/version switching logic
- ✅ No embedded resource creation
- ✅ Single responsibility: selection

---

## Data Flow

### Resource Creation Flow

```
┌─────────────────────────────────────────────────────────┐
│ User: Click "Create New Schema" on Schema Table         │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SchemaTable: Open SchemaEditor drawer                   │
│   - isOpen = true                                       │
│   - schemaId = undefined (create mode)                  │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SchemaEditor: Render empty form                         │
│   - Load JSON schema definition (MOCK_SCHEMA_SCHEMA)    │
│   - Render RJSF form with empty initial data            │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ User: Fill form (name, type, definition)               │
│   - Name: "temperature_schema"                          │
│   - Type: "JSON"                                        │
│   - Definition: { "type": "object", ... }               │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ User: Click "Save"                                      │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SchemaEditor: Validate and save                         │
│   - Validate JSON definition (JSON.parse)               │
│   - Call createSchema mutation                          │
│   - Wait for success/error                              │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Success: Close drawer, refresh table, show toast       │
│   - SchemaTable: Refetch schemas                        │
│   - Toast: "Schema created successfully"                │
│   - New row appears in table                            │
└─────────────────────────────────────────────────────────┘
```

---

### Resource Selection in Policy Flow

```
┌─────────────────────────────────────────────────────────┐
│ User: Open policy designer, add schema node            │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ User: Click on schema node to configure                │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SchemaPanelSimplified: Render resource selector        │
│   - Load all available schemas via useGetAllSchemas    │
│   - Render ResourceSelector component                  │
│   - No creation UI, selection only                     │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ User: Select schema name from dropdown                 │
│   - "temperature_schema" selected                       │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ ResourceSelector: Load versions for selected name      │
│   - Filter schemas where id === "temperature_schema"    │
│   - Extract versions: [1, 2, 3]                         │
│   - Populate version dropdown                           │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ User: Select version from dropdown                      │
│   - Version 3 selected                                  │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ ResourceSelector: Call onChange callback                │
│   - onChange({ name: "temperature_schema", version: 3 })│
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SchemaPanelSimplified: Update node data                │
│   - onFormSubmit({ name: "...", version: 3, ... })     │
│   - Node data persisted in draft store                 │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ SchemaNode: Display selected resource                  │
│   - Display: "temperature_schema (v3) ✓"               │
│   - Tooltip: Schema details (type, description)        │
└─────────────────────────────────────────────────────────┘
```

---

### Policy Publishing Flow (New)

```
┌─────────────────────────────────────────────────────────┐
│ User: Click "Check Policy" button                      │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ usePolicyDryRun: Validate policy                        │
│   1. Extract all nodes and edges                        │
│   2. For each schema node:                              │
│      - Extract resource reference (name + version)      │
│      - Query: Does this schema exist?                   │
│      - Add error if not found                           │
│   3. For each function node:                            │
│      - Extract resource reference (name + version)      │
│      - Query: Does this script exist?                   │
│      - Add error if not found                           │
│   4. Build dry-run report (DryRunResults[])             │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Result: SUCCESS or FAILURE                              │
│   SUCCESS: All resources exist, policy valid            │
│   FAILURE: Missing resources or validation errors       │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ If FAILURE: Show errors in dry-run panel                │
│   - "Schema 'temperature_schema' v3 not found"          │
│   - "Create this schema from the Schema table"          │
│   - Publish button disabled                             │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ If SUCCESS: Enable publish button                       │
│   - Show success summary                                │
│   - List resource references used                       │
│   - "All resources are published and available"         │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ User: Click "Publish Policy"                            │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ ToolbarPublish: Publish policy ONLY                     │
│   - No resource publishing (already published)          │
│   - Policy payload includes resource references:        │
│     {                                                   │
│       id: "temp_policy",                                │
│       validators: [                                     │
│         {                                               │
│           type: "SCHEMA",                               │
│           schemaRef: {                                  │
│             name: "temperature_schema",                 │
│             version: 3                                  │
│           }                                             │
│         }                                               │
│       ]                                                 │
│     }                                                   │
│   - Call createPolicy or updatePolicy mutation          │
└─────────────────────────────────────────────────────────┘
                         ▼
┌─────────────────────────────────────────────────────────┐
│ Success: Show toast, navigate to published policy      │
│   - Toast: "Policy 'temp_policy' published successfully"│
│   - Navigate: /datahub/DATA_POLICY/temp_policy          │
└─────────────────────────────────────────────────────────┘
```

**⚠️ IMPORTANT: Current Publishing Flow Analysis Needed**

The current `ToolbarPublish.tsx` already:

1. Extracts resources from dry-run report
2. Publishes resources separately via `publishResources()`
3. Then publishes policy via `publishMainPolicy()`

**This means the backend already supports separate resource publishing!**

**Key Questions to Answer:**

- Are resources currently embedded in policy JSON or referenced?
- Is the current dry-run report structure correct for our needs?
- Do we need to change publishing flow at all, or just the UI?
- How are schemas/scripts referenced in the `arguments` field?

**Potential Changes** (to be verified):

- May not need to change publishing flow at all
- Focus on UI changes (move resource creation to tables)
- Validation might need to check if resources exist before publish
- Policy payload structure needs investigation

---

## State Management

### No New Stores Needed

Reuse existing Zustand stores:

**useDataHubDraftStore**: Already handles policy designer state

- No changes needed
- Continues to track nodes, edges, status

**usePolicyChecksStore**: Already handles validation state

- Minor changes: Include resource validation errors
- No structural changes

**React Query Caches**: Already handle resource data

- `useGetAllSchemas` - Fetch all schemas
- `useGetAllScripts` - Fetch all scripts
- `useCreateSchema`, `useUpdateSchema` - Schema mutations
- `useCreateScript`, `useUpdateScript` - Script mutations

### Component-Local State

New components use local state only:

```typescript
// SchemaEditor - local state for form
const [formData, setFormData] = useState<SchemaData>()
const [isLoading, setIsLoading] = useState(false)
const [error, setError] = useState<Error>()

// ResourceSelector - local state for selection
const [selectedName, setSelectedName] = useState<string>()
const [selectedVersion, setSelectedVersion] = useState<number>()
const [availableVersions, setAvailableVersions] = useState<number[]>([])
```

**Benefits**:

- No global state pollution
- Easier to test
- Clear component boundaries
- No store coupling

---

## API Contracts

### ⚠️ IMPORTANT: API Investigation Required

**CRITICAL**: The actual API contract for policies needs to be verified before implementation. Based on generated types:

```typescript
// From DataPolicy.ts
export type DataPolicy = {
  id: string
  matching: DataPolicyMatching
  onFailure?: DataPolicyAction
  onSuccess?: DataPolicyAction
  validation?: DataPolicyValidation
}

// From DataPolicyValidation.ts
export type DataPolicyValidation = {
  validators?: Array<DataPolicyValidator>
}

// From DataPolicyValidator.ts
export type DataPolicyValidator = {
  arguments: Record<string, any> // ⚠️ How resources are referenced!
  type: DataPolicyValidator.type // e.g., 'SCHEMA'
}
```

### Questions to Answer During Implementation

1. **How are schemas referenced in `arguments`?**
   - Is it `{ schemaId: "name", version: 3 }`?
   - Is it `{ schema: "name:3" }`?
   - Is it embedded PolicySchema object?
2. **Current publishing flow (ToolbarPublish.tsx) already publishes resources separately**

   - Resources extracted via `resourceReducer` from dry-run report
   - `publishResources()` creates schemas/scripts first
   - Then `publishMainPolicy()` publishes policy
   - **This means the separation already exists in the backend!**

3. **What changes are actually needed?**
   - The refactoring is about **UI/UX**, not API contracts
   - Move resource creation UI from panels to tables
   - Simplify panels to just select existing resources
   - Publishing flow may not need changes at all

### Action Items Before Subtask 1.1

- [ ] Inspect actual policy payloads in network tab
- [ ] Review backend API documentation
- [ ] Verify `arguments` structure for SCHEMA validators
- [ ] Confirm if current publishing flow is correct
- [ ] Update this document with findings

---

## Error Handling

### Resource Not Found During Validation

```typescript
// In usePolicyDryRun.ts
const validateResourceReferences = (nodes: Node[]): ValidationError[] => {
  const errors: ValidationError[] = []

  for (const node of nodes) {
    if (node.type === DataHubNodeType.SCHEMA) {
      const { name, version } = node.data
      const schemaExists = allSchemas?.items?.some((s) => s.id === name && s.version === version)

      if (!schemaExists) {
        errors.push({
          node: node,
          error: {
            title: 'Schema Reference Error',
            detail: `Schema "${name}" version ${version} not found. This schema is used by the validator node but doesn't exist.`,
            suggestion: 'Create this schema from the Schema table',
            actionLink: '/datahub?tab=schemas',
          },
        })
      }
    }

    // Similar for function nodes...
  }

  return errors
}
```

### Clear Error Messages

```
❌ Schema Reference Error

Schema "temperature_schema" version 3 not found.

This schema is used by the validator node but doesn't exist.

→ Create this schema from the Schema table

[Go to Schemas] [Dismiss]
```

---

## Testing Strategy

### 🚨 CRITICAL: Always Run Tests Before Declaring Complete

**Per project TESTING_GUIDELINES.md:**

- ❌ NEVER claim tests pass without running them
- ❌ NEVER say "tests should work"
- ✅ ALWAYS run actual test commands with --spec option
- ✅ ALWAYS include real test output in completion docs
- ✅ ALWAYS fix failures before proceeding

### Test Commands

**Component Tests** (run individual files):

```bash
# Single component
pnpm cypress:run:component --spec "src/extensions/datahub/components/editors/SchemaEditor.spec.cy.tsx"

# Related components
pnpm cypress:run:component --spec "src/extensions/datahub/components/editors/**/*.spec.cy.tsx"
```

**E2E Tests** (run specific scenarios):

```bash
# Single E2E test
pnpm cypress:run:e2e --spec "cypress/e2e/datahub/resource-management.spec.cy.ts"
```

**⚠️ DO NOT run all Cypress tests** unless specifically instructed.

### Component Test Requirements

**All new components MUST test:**

1. Rendering and state changes
2. User interactions
3. Error/loading states
4. **Accessibility (MANDATORY)**: Keyboard nav, ARIA labels, focus management

**Example Structure:**

```typescript
describe('SchemaEditor', () => {
  it('renders drawer when open')
  it('loads existing schema in edit mode')
  it('validates JSON schema syntax')
  it('calls onSave with correct data')

  // MANDATORY ACCESSIBILITY
  it('is keyboard accessible')
  it('closes on Escape key')
  it('has proper ARIA labels')
})
```

### E2E Test Requirements

**Test complete flows using Page Objects:**

```typescript
// cypress/pages/DataHub/SchemaEditorPage.ts
export class SchemaEditorPage {
  openCreateNew() {
    return cy.getByTestId('create-schema-button').click()
  }

  fillForm(data) {
    cy.get('input[name="name"]').type(data.name)
    // ...
  }
}
```

### Completion Documentation Format

**Required when completing subtasks with tests:**

````markdown
## Test Verification

Command: `pnpm cypress:run:component --spec "src/.../SchemaEditor.spec.cy.tsx"`

Results:

```
SchemaEditor
  ✓ renders drawer (120ms)
  ✓ validates syntax (150ms)
  ✓ is keyboard accessible (200ms)

  3 passing (2s)
```

✅ All tests verified passing.
````

---

## Migration Strategy

### Feature Flag

```typescript
// config/features.ts
export const FEATURES = {
  DATAHUB_SIMPLIFIED_RESOURCES: import.meta.env.VITE_FEATURE_SIMPLIFIED_RESOURCES === 'true'
}

// In code
{FEATURES.DATAHUB_SIMPLIFIED_RESOURCES ? (
  <SchemaPanelSimplified {...props} />
) : (
  <SchemaPanel {...props} />
)}
```

### Backward Compatibility

**Existing Policies**:

- Backend handles both formats (embedded + references)
- Frontend reads existing node data
- No data migration needed for display
- Save operation uses new format

**Gradual Rollout**:

1. Week 1-2: Development (flag OFF)
2. Week 3: Internal testing (flag ON dev)
3. Week 4: Beta (flag ON staging)
4. Week 5: Production (flag ON prod)
5. Week 6+: Remove old code

---

## Performance Considerations

### Resource Loading

**Optimization**: Cache schemas/scripts at app level

```typescript
// useGetAllSchemas.ts
export const useGetAllSchemas = () => {
  return useQuery({
    queryKey: ['datahub', 'schemas'],
    queryFn: fetchAllSchemas,
    staleTime: 5 * 60 * 1000, // 5 minutes
    cacheTime: 10 * 60 * 1000, // 10 minutes
  })
}
```

### Form Rendering

**Optimization**: Lazy load code editor

```typescript
// SchemaEditor.tsx
const CodeEditor = lazy(() => import('./CodeEditor'))

// In render
<Suspense fallback={<Skeleton height="200px" />}>
  <CodeEditor value={schemaSource} onChange={handleChange} />
</Suspense>
```

### Table Rendering

**Optimization**: Virtualization for large resource lists

```typescript
// SchemaTable.tsx - if needed
import { useVirtualizer } from '@tanstack/react-virtual'

// Only if >1000 resources, not typical
```

---

## Accessibility

### Keyboard Navigation

**All Interactions**:

- Tab through form fields
- Arrow keys in dropdowns
- Enter to submit
- Escape to cancel

**Focus Management**:

```typescript
// SchemaEditor.tsx
const firstFieldRef = useRef<HTMLInputElement>(null)

useEffect(() => {
  if (isOpen) {
    // Focus first field when drawer opens
    firstFieldRef.current?.focus()
  }
}, [isOpen])
```

### Screen Reader Support

**ARIA Labels**:

```tsx
<Select
  aria-label="Select schema name"
  aria-describedby="schema-name-help"
>
  {/* options */}
</Select>
<Text id="schema-name-help" fontSize="sm">
  Choose an existing schema to use in this policy
</Text>
```

**Live Regions**:

```tsx
<div role="status" aria-live="polite">
  {isSaving && 'Saving schema...'}
  {saveSuccess && 'Schema saved successfully'}
</div>
```

---

## Security Considerations

### Input Validation

**Schema Definition**:

- Validate JSON syntax before save
- Validate Protobuf syntax before save
- Sanitize user input (XSS prevention)

**Script Source**:

- Validate JavaScript syntax (linting)
- Warning for potentially dangerous patterns
- Sandbox execution in preview (if added)

### Authorization

**Resource CRUD**:

- Backend enforces permissions
- Frontend hides UI if no permission
- Clear error messages if action denied

---

## Future Enhancements (Out of Scope)

### Phase 2 Features

1. **Inline Resource Preview**

   - Preview schema/script in selector
   - Quick view without opening editor

2. **Resource Usage Analytics**

   - Show which policies use a resource
   - Warning before deleting used resource

3. **Bulk Operations**

   - Import multiple schemas
   - Export resource library

4. **Resource Templates**

   - Common schema templates
   - Script templates

5. **Version Diff Viewer**
   - Compare two versions
   - Highlight changes

---

## Summary

### Key Architectural Decisions

1. ✅ **Separation of Concerns**: Resource CRUD separate from node configuration
2. ✅ **Reusable Components**: Base drawer, resource selector
3. ✅ **Simplified Panels**: <100 LOC, single responsibility
4. ✅ **Reference-Based**: Policies reference resources, don't embed them
5. ✅ **Feature Flag**: Gradual rollout, backward compatibility
6. ✅ **No New Stores**: Reuse existing state management
7. ✅ **Clear Error Messages**: Actionable feedback for missing resources
8. ✅ **Accessibility First**: WCAG 2.1 AA compliance

### Success Metrics

- > 50% LOC reduction in panels ✅
- > 80% test coverage ✅
- No regression in existing functionality ✅
- Clear user mental model ✅
- Easier maintenance ✅

---

**Document Version**: 1.0  
**Last Updated**: November 26, 2025  
**Status**: Design Complete, Ready for Implementation
