# Combiner Walkthrough - Documentation Plan

**Date:** 2026-02-16
**Task:** EDG-40 - Combiner UX Walkthrough Documentation
**Target:** docs/walkthroughs/RJSF_COMBINER.md

---

## Document Purpose

Create a walkthrough document that explains the Combiner UX from two complementary perspectives:

1. **Interaction Design Perspective**: Why we broke down the flat form into interactive UX patterns (user-centered design rationale)
2. **Technical Perspective**: How data flows through RJSF components (props, schema, uiSchema, formData, context, validation)

**Audience:**

- Developers maintaining or extending the Combiner
- UX designers understanding the rationale
- AI agents implementing similar form patterns
- Technical leads evaluating form architecture

---

## Document Structure

### 1. Introduction (5 sections)

**1.1 Overview**

- What is the Combiner?
- Why does it need special documentation?
- Two-perspective approach explained

**1.2 The Challenge: From Flat Form to Interactive UX**

- Screenshot: Native RJSF form (flat, scrolling)
- Screenshot: Custom Combiner UX (tabs, table, drawer)
- User pain points with flat forms
- Interaction design goals

**1.3 The Technical Context**

- RJSF architecture primer (brief, link to RJSF_GUIDE.md)
- Custom widgets vs fields vs templates
- Schema isolation principle: "widgets only see their own level"
- FormContext for cross-widget data sharing

**1.4 Combiner Data Model**

- OpenAPI schema overview (brief code block)
- Key entities: Combiner, EntityReference, DataCombining, Instruction
- Link to: PROTOCOL_ADAPTER_ARCHITECTURE.md for schema details

**1.5 Component Hierarchy**

- Tree diagram (text-based) showing widget nesting
- Ownership boundaries highlighted
- FormContext flow indicated

```
CombinerMappingManager
└─ ChakraRJSForm (RJSF)
   └─ EntityReferenceTableWidget
   └─ DataCombiningTableField
      └─ DataCombiningEditorDrawer
         └─ ChakraRJSForm (nested)
            └─ DataCombiningEditorField
               ├─ CombinedEntitySelect
               ├─ PrimarySelect
               ├─ CombinedSchemaLoader
               ├─ DestinationSchemaLoader
               └─ ...
```

---

### 2. Breaking Down the Flat Form (Interaction Design Focus)

**2.1 The Flat Form Problem**

- Screenshot: Native RJSF with all fields visible
- UX issues:
  - Cognitive overload (60+ fields on single scroll)
  - No visual grouping of related fields
  - Poor scannability for array items
  - No context for relationships (which entity owns which tag?)
  - Validation errors hard to locate

**2.2 Progressive Disclosure Strategy**

- Tab pattern for top-level grouping (Configuration, Sources, Mappings)
- Table pattern for array items (mappings list)
- Drawer pattern for item editing (focus on single mapping)
- Screenshot: Tab navigation showing 3 sections

**2.3 Contextual Scaffolding**

- Definition: UI that constrains user actions to prevent invalid states
- Example: Primary selector only shows selected integration points
- Example: Entity selector shows entity metadata (type, description)
- Trade-off: More code complexity for better UX

**2.4 Visual Affordances**

- Icons indicating field types (key icon for primary)
- Color coding for entity types
- Empty states with clear CTAs
- Screenshot: Mapping table with visual indicators

---

### 3. Widget Deep Dives (4 Widgets × 2 Perspectives Each)

Each widget section follows this template:

**[Widget Name]**

**Interaction Design Perspective:**

- User goal: What is the user trying to accomplish?
- UX challenges: What makes this task difficult in a flat form?
- Design solution: How does this widget address the challenges?
- Visual design: Icons, colors, layout choices
- Screenshot: Widget in action

**Technical Perspective:**

- Schema binding: What part of the schema does this widget own?
- Props received: formData structure and type
- Secondary data: What other data does it consume? (FormContext, queries)
- Data flow: How does user interaction become schema update?
- onChange implementation: What triggers re-renders?
- Code location: File path for reference
- Validation: Schema-based vs custom rules

---

#### 3.1 PrimarySelect Widget

**Interaction Design Perspective:**

**User Goal:** Select which integration point (tag or topic filter) serves as the primary key for this mapping.

**UX Challenges in Flat Form:**

- Open text input allows invalid values (not in sources list)
- No visual indication of which sources are available
- Type (TAG vs TOPIC_FILTER) must be entered separately
- Easy to mistype tag/topic filter names

**Design Solution:**

- Dropdown selector with autocomplete
- Options auto-populated from selected sources
- Type automatically set on selection
- Key icon provides visual affordance
- Clearable to unset primary
- Screenshot placeholder: [Primary selector dropdown showing tag/topic filter options]

**Visual Design:**

- Key icon (🔑) prefixed to control
- Options labeled with raw identifier names
- Type hidden from UI (inferred from selection)

**Technical Perspective:**

**Schema Binding:**

- Widget bound to: `DataCombining` schema
- Property controlled: `sources.primary` (type: `DataIdentifierReference`)
  ```typescript
  sources: {
    primary: { id: string, type: 'TAG' | 'TOPIC_FILTER' }
  }
  ```

**Props Received:**

```typescript
interface PrimarySelectProps {
  id?: string
  formData?: DataCombining // Full DataCombining object
  onChange: (newValue: SingleValue<PrimaryOption>) => void
}
```

**Secondary Data Consumed:**

- `formData.sources.tags` (string[]) - Read-only
- `formData.sources.topicFilters` (string[]) - Read-only
- Combines both into unified option list

**Data Flow:**

1. Component receives `formData` containing current mapping state
2. `useMemo` computes options from `tags` + `topicFilters`:
   ```typescript
   const primaryOptions = useMemo(() => {
     const tags = formData?.sources?.tags || []
     const topicFilters = formData?.sources?.topicFilters || []
     return [
       ...tags.map((entity) => ({ label: entity, value: entity, type: 'TAG' })),
       ...topicFilters.map((entity) => ({ label: entity, value: entity, type: 'TOPIC_FILTER' })),
     ]
   }, [formData])
   ```
3. User selects option → `onChange` called with `{ label, value, type }`
4. Parent component transforms to `DataIdentifierReference` and updates `formData.sources.primary`
5. RJSF validates new value, triggers re-render
6. PrimarySelect receives updated `formData`, recomputes `primaryValue` via second `useMemo`

**onChange Implementation:**

- Widget calls `onChange` with `PrimaryOption` object
- Parent (DataCombiningEditorField) transforms to schema format
- RJSF propagates change to root form

**Code Location:**

- `src/modules/Mappings/combiner/PrimarySelect.tsx`

**Validation:**

_Schema-based:_

- `sources.primary` is NOT required by schema (can be null)
- Type is enum: must be 'TAG' or 'TOPIC_FILTER'

_Custom validation:_

- `validateDataPrimary` in CombinerMappingManager.tsx:
  ```typescript
  const validateDataPrimary = useCallback<CustomValidator<DataCombining>>((formData, errors) => {
    const allInt = [...(formData?.sources?.tags || []), ...(formData?.sources?.topicFilters || [])]
    if (formData?.sources?.primary?.id && !allInt.includes(formData?.sources?.primary?.id)) {
      errors.sources?.primary?.addError('The primary key is not one of the integration point')
    }
    return errors
  }, [])
  ```
- Rule: If primary is set, it MUST exist in tags or topicFilters arrays
- Error message displayed below selector when violated

**Scaffolding in Action:**

- Because options come ONLY from existing sources, user cannot type invalid value
- Implicit validation: UI prevents impossible states
- Custom validation serves as safety net for programmatic changes

---

#### 3.2 CombinedEntitySelect Widget

**Interaction Design Perspective:**

**User Goal:** Select which entities (adapters, bridges) provide the integration points for this mapping.

**UX Challenges in Flat Form:**

- Entity IDs are opaque (UUIDs or adapter names)
- No context about entity type, status, or available integration points
- Multi-select for both tags and topic filters requires understanding which entity provides which
- Easy to select wrong entity without metadata

**Design Solution:**

- Multi-select dropdown with rich option display
- Each option shows:
  - Entity name
  - Entity type badge (Adapter, Bridge)
  - Available integration points (tags or topic filters)
  - Entity description (if available)
  - Icon indicating entity type
- Grouped by type (Adapters, Bridges)
- Clear labels distinguishing "Select tags from..." vs "Select topic filters from..."
- Screenshot placeholder: [Entity selector dropdown showing adapter/bridge options with metadata]

**Visual Design:**

- Icons for entity types (adapter icon, bridge icon)
- Badges for entity type
- Nested list showing available integration points
- Color coding per entity type

**Technical Perspective:**

**Schema Binding:**

- Widget bound to: `DataCombining` schema
- Properties controlled:
  - `sources.tags` (string[])
  - `sources.topicFilters` (string[])

**Props Received:**

```typescript
interface CombinedEntitySelectProps {
  formData?: DataCombining
  onChange: (field: 'tags' | 'topicFilters', values: string[]) => void
}
```

**Secondary Data Consumed:**

- **FormContext:** `combinedEntities` from `useGetCombinedEntities` query
  - Contains full entity details (adapters, bridges) from parent component
  - Structure:
    ```typescript
    type CombinedEntity = {
      id: string
      name: string
      type: 'ADAPTER' | 'BRIDGE'
      tags?: Tag[] // Available for adapters
      topicFilters?: string[] // Available for bridges
    }
    ```
- Retrieved via `const context = useContext(FormContext)` in component

**Data Flow:**

1. Parent (CombinerMappingManager) fetches entities via `useGetCombinedEntities(combinerSources)`
   - `combinerSources` from Combiner.sources (EntityReference[])
   - Query enriches EntityReferences with full entity data
2. Parent passes entities to nested form via `formContext.combinedEntities`
3. CombinedEntitySelect accesses via React context:
   ```typescript
   const context = useContext(FormContext)
   const entities = context?.combinedEntities || []
   ```
4. Component splits entities by type:
   ```typescript
   const adapters = entities.filter((e) => e.type === 'ADAPTER')
   const bridges = entities.filter((e) => e.type === 'BRIDGE')
   ```
5. User selects adapter → `onChange('tags', selectedTags)` called
6. User selects bridge → `onChange('topicFilters', selectedFilters)` called
7. Parent updates `formData.sources.tags` or `formData.sources.topicFilters`
8. RJSF propagates, triggers re-render
9. PrimarySelect re-computes options (dependent on tags/topicFilters)

**onChange Implementation:**

- Two separate calls: one for tags, one for topicFilters
- Each returns array of integration point IDs (strings)
- Parent merges into formData at correct path

**Code Location:**

- `src/modules/Mappings/combiner/CombinedEntitySelect.tsx`

**Validation:**

_Schema-based:_

- `sources.tags` is string[] (not required, can be empty)
- `sources.topicFilters` is string[] (not required, can be empty)

_Custom validation:_

- No custom validation rules for these fields
- Validation comes from Primary selector (must be in sources list)

**React-Select Integration:**

- Uses `react-select` library for multi-select UI
- Custom `Option` component renders entity metadata:
  ```typescript
  const Option = (props: OptionProps<EntityOption>) => (
    <chakraComponents.Option {...props}>
      <HStack>
        <Icon as={getEntityIcon(props.data.type)} />
        <VStack align="start">
          <Text>{props.data.label}</Text>
          <Text fontSize="sm" color="gray.500">{props.data.description}</Text>
        </VStack>
      </HStack>
    </chakraComponents.Option>
  )
  ```
- Custom `MultiValue` component for selected chips

**Scaffolding in Action:**

- Options only show entities already selected in Combiner.sources
- Cannot add entities not in parent Combiner (must go back to Sources tab)
- Rich metadata helps user understand what they're selecting
- Prevents selecting adapters for topic filters (type mismatch impossible)

---

#### 3.3 DataCombiningEditorField

**Interaction Design Perspective:**

**User Goal:** Configure a complete mapping: select sources, choose primary, define destination, create transformation instructions.

**UX Challenges in Flat Form:**

- 10+ fields displayed linearly
- Relationships between fields not clear (primary depends on sources)
- No visual grouping of source vs destination
- Schema loading triggered on multiple dependencies
- Instruction list buried at bottom (most important part!)

**Design Solution:**

- Grid layout splitting left (sources) and right (destination)
- Visual flow: Sources → Primary → Schema Loading → Destination → Instructions
- Collapsible sections for sources/destination configuration
- Schema loaders show live feedback (loading, error, schema name)
- Instruction list prominently displayed with mapping table
- Screenshot placeholder: [Editor field showing split layout with sources on left, destination on right]

**Visual Design:**

- Grid: 2 columns on desktop, 1 column on mobile
- Source section: Blue border accent
- Destination section: Green border accent
- Schema loader: Card with status indicator
- Instruction list: Full-width table below grid

**Technical Perspective:**

**Schema Binding:**

- Custom field bound to: `DataCombining` schema (full object)
- Properties managed:
  - `sources.tags`
  - `sources.topicFilters`
  - `sources.primary`
  - `destination.assetId`
  - `destination.topic`
  - `destination.schema`
  - `instructions[]`

**Props Received:**

```typescript
interface DataCombiningEditorFieldProps {
  idSchema: IdSchema
  formData?: DataCombining
  onChange: (newValue: DataCombining) => void
  schema: RJSFSchema
  uiSchema: UiSchema
  registry: Registry
}
```

**Secondary Data Consumed:**

- **FormContext.combinedEntities:** Full entity details for CombinedEntitySelect
- **React Query:** Schema loading queries
  - `useGetDataPointSchemas` for combined source schemas
  - `useGetDataHubSchema` for destination schema
- **Local State:**
  - Expanded/collapsed sections
  - Schema loading status

**Data Flow:**

_Complex Multi-Step Flow:_

1. **Initial Render:**

   - Receives `formData` with current DataCombining state
   - Passes subsets to child widgets (PrimarySelect, CombinedEntitySelect)

2. **Source Selection (CombinedEntitySelect):**

   - User selects tags/topic filters
   - `onChange('tags', [...])` called
   - DataCombiningEditorField updates `formData`:
     ```typescript
     const handleSourceChange = (field: 'tags' | 'topicFilters', values: string[]) => {
       onChange({
         ...formData,
         sources: {
           ...formData.sources,
           [field]: values,
         },
       })
     }
     ```
   - RJSF propagates change
   - PrimarySelect re-renders with new options

3. **Primary Selection (PrimarySelect):**

   - User selects primary
   - `onChange(primaryOption)` called
   - DataCombiningEditorField transforms to DataIdentifierReference:
     ```typescript
     const handlePrimaryChange = (option: PrimaryOption | null) => {
       onChange({
         ...formData,
         sources: {
           ...formData.sources,
           primary: option ? { id: option.value, type: option.type } : undefined,
         },
       })
     }
     ```

4. **Schema Loading (Automatic):**

   - `useEffect` watches `formData.sources.tags` and `formData.sources.topicFilters`
   - When changed, triggers `useGetDataPointSchemas` query
   - Query fetches schemas for all selected sources
   - `CombinedSchemaLoader` displays loading state → schema name when complete
   - Schemas stored in component state (not in formData)

5. **Destination Configuration:**

   - User enters topic, assetId, selects schema
   - Each triggers separate onChange call
   - `DestinationSchemaLoader` fetches selected schema
   - Schema used to populate instruction builder

6. **Instruction Creation:**
   - User interacts with `MappingInstructionList`
   - Instructions added to `formData.instructions[]`
   - Each instruction references source fields (from combined schema) and destination fields (from destination schema)

**onChange Implementation:**

- Single `onChange` prop receives complete updated `DataCombining`
- Field is responsible for merging partial updates from children
- Pattern: spread existing formData, update specific path, call onChange
- RJSF handles debouncing and validation

**Code Location:**

- `src/modules/Mappings/combiner/DataCombiningEditorField.tsx`

**Validation:**

_Schema-based:_

- `DataCombining.sources.primary` (see PrimarySelect validation)
- `DataCombining.destination.topic` format: 'mqtt-topic'
- `DataCombining.instructions` is array (not required)

_Custom validation:_

- Validation handled at parent (CombinerMappingManager) level
- Field is responsible for data structure, not validation

**Component Composition:**

- **Not a simple widget** - orchestrates 5+ sub-components
- **Layout component** - manages grid, spacing, visual flow
- **Data router** - receives single onChange, dispatches to children
- **Query coordinator** - triggers schema fetches based on source selection

**Scaffolding in Action:**

- Schema loaders prevent instruction creation until schemas loaded
- Primary selector constrains to valid sources
- Destination schema selector only shows published DataHub schemas
- Instruction builder only shows fields from loaded schemas

---

#### 3.4 DataCombiningTableField

**Interaction Design Perspective:**

**User Goal:** Manage multiple mappings (DataCombining items) for a single Combiner.

**UX Challenges in Flat Form:**

- Array items displayed as vertically stacked forms
- 10+ fields × N items = 10N fields on screen
- Scrolling nightmare to find specific mapping
- No overview of all mappings
- Cannot see mapping summary without expanding details
- Editing one mapping requires scrolling past others

**Design Solution:**

- Table view showing mapping summaries
  - Columns: Primary key, Source count, Destination topic, Action buttons
- Row actions: Edit, Delete
- Add button above table
- Click Edit → drawer opens with single mapping editor
- Drawer contains full DataCombiningEditorField for one item
- Screenshot placeholder: [Mapping table with 3 rows showing summaries]
- Screenshot placeholder: [Drawer open showing DataCombiningEditorField]

**Visual Design:**

- Table: Alternating row colors, hover states
- Primary key column: Key icon + identifier
- Source count: Badge showing "2 sources"
- Action buttons: Icon buttons (Edit: pencil, Delete: trash)
- Drawer: Slides from right, full height, white background

**Technical Perspective:**

**Schema Binding:**

- Custom field bound to: `Combiner` schema
- Property controlled: `mappings.items` (DataCombining[])

**Props Received:**

```typescript
interface DataCombiningTableFieldProps {
  idSchema: IdSchema
  formData?: DataCombining[] // Array of mappings
  onChange: (newValue: DataCombining[]) => void
  schema: RJSFSchema
  uiSchema: UiSchema
  registry: Registry
}
```

**Secondary Data Consumed:**

- **FormContext.combinedEntities:** Passed through to drawer's nested form
- Drawer contains nested ChakraRJSForm, which needs same context

**Data Flow:**

_Array Manipulation Pattern:_

1. **Render Table:**

   - Receives `formData` as DataCombining[]
   - Maps array to table rows:
     ```typescript
     const rows = formData?.map((mapping, index) => ({
       id: mapping.id || `mapping-${index}`,
       primary: mapping.sources.primary?.id || '-',
       sourceCount: (mapping.sources.tags?.length || 0) + (mapping.sources.topicFilters?.length || 0),
       topic: mapping.destination?.topic || '-',
       index,
     }))
     ```

2. **Add New Mapping:**

   - User clicks "Add Mapping"
   - Creates empty DataCombining with UUID:
     ```typescript
     const handleAdd = () => {
       const newMapping: DataCombining = {
         id: uuidv4(),
         sources: { tags: [], topicFilters: [] },
         destination: {},
         instructions: [],
       }
       onChange([...(formData || []), newMapping])
       setEditingIndex(formData?.length || 0)
       onDrawerOpen()
     }
     ```
   - Opens drawer with new item

3. **Edit Existing Mapping:**

   - User clicks Edit button on row
   - Sets `editingIndex` state
   - Opens drawer
   - Drawer renders DataCombiningEditorField with `formData[editingIndex]`

4. **Update Mapping in Drawer:**

   - User edits fields in DataCombiningEditorField
   - Field calls its onChange with updated DataCombining
   - Drawer's onChange handler:
     ```typescript
     const handleDrawerChange = (updatedMapping: DataCombining) => {
       const newArray = [...(formData || [])]
       newArray[editingIndex] = updatedMapping
       onChange(newArray)
     }
     ```
   - Array item replaced, parent onChange called
   - RJSF validates entire array

5. **Delete Mapping:**
   - User clicks Delete button
   - Confirmation dialog shown
   - On confirm:
     ```typescript
     const handleDelete = (index: number) => {
       const newArray = (formData || []).filter((_, i) => i !== index)
       onChange(newArray)
     }
     ```

**onChange Implementation:**

- Table field manages array operations (add, update, delete)
- Each operation creates new array, calls onChange
- RJSF treats entire array as single change
- Individual item changes propagated up from drawer

**Code Location:**

- `src/modules/Mappings/combiner/DataCombiningTableField.tsx`
- `src/modules/Mappings/combiner/DataCombiningEditorDrawer.tsx` (drawer wrapper)

**Validation:**

_Schema-based:_

- `mappings.items` is array of DataCombining
- Each item validated against DataCombining schema
- Array itself not required (can be empty)

_Custom validation:_

- Applied to each array item individually
- Same validation rules as DataCombiningEditorField

**Nested Form Context:**

- Drawer contains **separate RJSF instance** for single item
- Must recreate FormContext with same combinedEntities:
  ```typescript
  <ChakraRJSForm
    schema={itemSchema}
    formData={formData[editingIndex]}
    onChange={handleDrawerChange}
    formContext={{ combinedEntities: context?.combinedEntities }}
  >
    <DataCombiningEditorField {...} />
  </ChakraRJSForm>
  ```
- Context propagation critical for CombinedEntitySelect to work

**Scaffolding in Action:**

- Table provides overview, prevents cognitive overload
- Drawer focuses user on single mapping (progressive disclosure)
- Cannot edit multiple mappings simultaneously (prevents confusion)
- Delete confirmation prevents accidental data loss
- Empty state with clear CTA when no mappings exist

---

### 4. Data Lifecycle Deep Dive

**4.1 RJSF Props Flow**

**The Core Props:**

```typescript
<ChakraRJSForm
  schema={combinerSchema}       // JSON Schema defining data structure
  uiSchema={combinerUiSchema}   // UI customization (widgets, ordering, etc.)
  formData={currentData}        // Current form state (the payload)
  formContext={contextData}     // Additional data passed to custom widgets
  onChange={handleChange}       // Called on any data change
  onSubmit={handleSubmit}       // Called when form submitted
  validator={customValidator}   // Custom validation rules
/>
```

**Data Flow Diagram:**

```
User Interaction
      ↓
Widget onChange
      ↓
Transform to schema format
      ↓
RJSF onChange handler
      ↓
Validate against schema + custom rules
      ↓ (if valid)
Update formData state
      ↓
Re-render all affected widgets
      ↓
Widget useMemo recomputes derived data
      ↓
UI updates
```

---

**4.2 FormContext Pattern**

**Problem:** Custom widgets are isolated by design (only see their schema level), but Combiner widgets need shared data:

- CombinedEntitySelect needs full entity details (name, type, integration points)
- Primary selector needs to know which sources are selected
- Schema loaders need entity data to fetch schemas

**Solution:** FormContext provides read-only shared data without breaking widget isolation.

**Implementation:**

_Parent (CombinerMappingManager):_

```typescript
const { data: combinedEntities } = useGetCombinedEntities(
  combiner?.sources?.items || []
)

<ChakraRJSForm
  formContext={{
    combinedEntities,
    // Other shared data...
  }}
/>
```

_Child Widget (CombinedEntitySelect):_

```typescript
const context = useContext(FormContext)
const entities = context?.combinedEntities || []
```

**Rules:**

- FormContext is READ-ONLY (cannot call onChange on it)
- Should contain query results, not form data
- Avoids prop drilling through RJSF internals
- Standard React Context API

---

**4.3 Validation Separation**

**Two Validation Layers:**

**Layer 1: Schema-Based Validation (Automatic)**

- Defined in JSON Schema via metadata
- Examples:
  ```typescript
  { type: 'string', format: 'mqtt-topic' }  // Format validation
  { type: 'number', minimum: 1, maximum: 100 }  // Range validation
  { required: ['id', 'name'] }  // Required fields
  ```
- Errors displayed inline below fields
- Blocking: Form cannot submit if schema validation fails

**Layer 2: Custom Validation (Manual)**

- Defined in validator functions
- Applied to full formData or specific paths
- Examples:
  - Primary must be in sources list
  - Destination topic must not conflict with source topics
  - Instruction source fields must exist in combined schema
- Implemented in `CombinerMappingManager.tsx`:
  ```typescript
  const validateDataPrimary = useCallback<CustomValidator<DataCombining>>(
    (formData, errors) => {
      // Custom logic here
      if (condition) {
        errors.sources?.primary?.addError('Error message')
      }
      return errors
    },
    [dependencies]
  )
  ```

**Why Separate?**

- Schema validation = data structure correctness
- Custom validation = business logic correctness
- Widgets don't know about validation (separation of concerns)
- Validation can be tested independently

---

**4.4 Scaffolding vs Validation**

**Scaffolding Definition:** UI components that implicitly validate by constraining user actions.

**Example: Primary Selector**

_Without Scaffolding (text input):_

- User can type any string
- Validation runs on blur/submit
- Error shown: "Primary must be one of the selected sources"
- User must manually fix typo

_With Scaffolding (dropdown):_

- User can only select from existing sources
- Invalid value impossible to enter
- No validation error (UI prevents invalid state)
- Better UX: no error messages, clear options

**Trade-offs:**

- **Pro:** Better UX, fewer errors, clearer choices
- **Pro:** Validation as safety net (not primary defense)
- **Con:** More complex component code
- **Con:** Harder to test (must mock options)
- **Con:** Less flexible (cannot type free-form)

**When to Use:**

- Fields with constrained value sets (enums, references)
- Fields with complex formats (topics, tags)
- Multi-step dependencies (primary depends on sources)

**When Not to Use:**

- Free-form text fields (descriptions, names)
- Fields where flexibility matters more than correctness
- Fields where validation is primary requirement

---

**4.5 React Memoization for Performance**

**Problem:** On every formData change, all widgets re-render. Expensive computations (option lists, schema merges) run on every render.

**Solution:** `useMemo` and `useCallback` hooks to cache derived data.

**Example: PrimarySelect Options**

_Without Memoization:_

```typescript
const primaryOptions = [
  ...tags.map((t) => ({ label: t, value: t, type: 'TAG' })),
  ...topicFilters.map((t) => ({ label: t, value: t, type: 'TOPIC_FILTER' })),
]
// Recomputed on EVERY render (even if tags/topicFilters unchanged)
```

_With Memoization:_

```typescript
const primaryOptions = useMemo(
  () => [
    ...tags.map((t) => ({ label: t, value: t, type: 'TAG' })),
    ...topicFilters.map((t) => ({ label: t, value: t, type: 'TOPIC_FILTER' })),
  ],
  [formData]
) // Only recompute when formData changes
```

**Guidelines:**

- Use `useMemo` for expensive computations (array transformations, object merges)
- Use `useCallback` for event handlers passed to children
- Don't over-memoize (premature optimization)
- Profile before optimizing (React DevTools)

---

### 5. Cross-Cutting Concerns

**5.1 Error Handling**

**Schema Loading Errors:**

- `CombinedSchemaLoader` shows error state if query fails
- User cannot proceed to instruction creation
- Error message suggests checking entity configuration

**Validation Errors:**

- Inline below fields (red text, icon)
- Summary at top of form (if multiple errors)
- Prevents form submission

**API Errors:**

- Toast notifications for save failures
- Form data preserved (not lost)
- Retry mechanism for transient errors

---

**5.2 Empty States**

**No Mappings:**

- Table shows empty state card
- CTA: "Add your first mapping"
- Icon: Empty box illustration

**No Sources Selected:**

- Entity selector shows empty state
- Message: "Configure sources in the Sources tab"
- Link to Sources tab

**No Schemas Available:**

- Schema loader shows warning
- Message: "No schemas available for selected sources"
- Link to entity configuration

---

**5.3 Loading States**

**Schema Loading:**

- Skeleton loader in schema card
- Spinner icon + "Loading schema..."
- Disables instruction editor until complete

**Entity Loading:**

- Entity selector shows loading skeleton
- Options disabled during load
- Error state if query fails

---

**5.4 Accessibility**

**Keyboard Navigation:**

- All interactive elements focusable
- Tab order: top-to-bottom, left-to-right
- Enter to open dropdowns, Space to select
- Escape to close drawer

**Screen Reader Support:**

- ARIA labels on all selectors
- Role annotations (combobox, listbox, option)
- Live regions for dynamic content (validation errors, loading states)
- Semantic HTML (table for mappings, form for editor)

**Focus Management:**

- Focus trapped in drawer when open
- Focus returns to Edit button on drawer close
- Focus moves to error fields on validation failure

---

### 6. Testing Strategy

**6.1 Component Tests (Cypress)**

**PrimarySelect.spec.cy.tsx:**

- Renders with empty sources → shows empty dropdown
- Renders with tags → shows tag options
- Renders with topic filters → shows topic filter options
- Renders with both → shows combined options
- Selection triggers onChange with correct type
- Clear button resets value
- Accessibility: focusable, labeled, keyboard navigable

**CombinedEntitySelect.spec.cy.tsx:**

- Renders with combinedEntities in FormContext
- Shows adapter options with tag counts
- Shows bridge options with topic filter counts
- Multi-select works for tags
- Multi-select works for topic filters
- Cannot select adapters for topic filters (constraint)
- Accessibility: multi-select labeled, keyboard navigable

**DataCombiningEditorField.spec.cy.tsx:**

- Renders all sub-components
- Source selection triggers primary re-render
- Primary selection updates formData
- Schema loaders trigger on source change
- Destination configuration updates formData
- All changes propagate to onChange
- Accessibility: grid layout navigable, all fields labeled

**DataCombiningTableField.spec.cy.tsx:**

- Renders empty state with CTA
- Renders table with mappings
- Add button opens drawer
- Edit button opens drawer with correct mapping
- Delete button shows confirmation → removes mapping
- Drawer changes update array item
- Accessibility: table navigable, drawer focusable

---

**6.2 Integration Tests (E2E)**

**combiner-mapping-flow.spec.cy.ts:**

1. Create new Combiner
2. Add sources (2 adapters, 1 bridge)
3. Navigate to Mappings tab
4. Add first mapping
   - Select sources in entity selector
   - Choose primary
   - Configure destination
   - Create 2 instructions
   - Save drawer
5. Verify mapping appears in table
6. Edit mapping
   - Change primary
   - Add instruction
   - Save drawer
7. Verify changes reflected in table
8. Delete mapping
9. Verify mapping removed
10. Submit Combiner

---

**6.3 Mock Data Strategy**

**MSW Handlers:**

- `src/api/hooks/useGetCombinedEntities/__handlers__` - Entity query mocks
- `src/api/hooks/useGetDataPointSchemas/__handlers__` - Schema query mocks
- Return realistic data matching OpenAPI schema

**Test Fixtures:**

- `src/__test-utils__/fixtures/combiner.ts` - Sample Combiner objects
- `src/__test-utils__/fixtures/entities.ts` - Sample entities with tags/filters
- `src/__test-utils__/fixtures/schemas.ts` - Sample JSON schemas

**FormContext Mocking:**

```typescript
cy.mountWithProviders(
  <FormContext.Provider value={{ combinedEntities: mockEntities }}>
    <CombinedEntitySelect {...props} />
  </FormContext.Provider>
)
```

---

### 7. Common Pitfalls and Solutions

**7.1 Widget Receiving Wrong Data**

**Symptom:** Widget errors with "Cannot read property X of undefined"

**Cause:** Widget bound to wrong schema level via uiSchema

**Solution:**

- Check uiSchema path matches widget expectations
- Example: If widget expects `DataCombining`, bind at `mappings.items.items`
- Use console.log in widget to inspect `formData` structure

---

**7.2 FormContext Data Not Available**

**Symptom:** Widget receives `context = undefined` or `context.combinedEntities = undefined`

**Cause:** FormContext not passed to nested RJSF instance (in drawer)

**Solution:**

- Ensure nested ChakraRJSForm has `formContext` prop
- Pass same context object from parent form
- Check React DevTools to verify context value

---

**7.3 onChange Not Triggering Re-Render**

**Symptom:** User makes change, UI doesn't update

**Cause:** onChange called with mutated object (same reference)

**Solution:**

- Always create NEW object when calling onChange:

  ```typescript
  // Wrong (mutates)
  formData.sources.tags.push(newTag)
  onChange(formData)

  // Correct (new object)
  onChange({
    ...formData,
    sources: {
      ...formData.sources,
      tags: [...formData.sources.tags, newTag],
    },
  })
  ```

---

**7.4 Validation Errors Not Showing**

**Symptom:** Custom validation fails but no error message displayed

**Cause:** Error added to wrong path in errors object

**Solution:**

- Match error path to schema structure exactly:

  ```typescript
  // If validating formData.sources.primary:
  errors.sources?.primary?.addError('message')

  // NOT:
  errors.primary?.addError('message') // Wrong level
  ```

---

**7.5 Memoization Stale Data**

**Symptom:** Widget shows old data after formData changes

**Cause:** useMemo dependency array missing formData fields

**Solution:**

- Include all formData fields used in computation:
  ```typescript
  useMemo(() => computeOptions(formData.sources), [formData.sources])
  // NOT: [formData] (too coarse)
  // NOT: [] (never updates)
  ```

---

### 8. Future Improvements

**8.1 Known Limitations**

- **No undo/redo:** Mapping changes cannot be undone (except Cancel drawer)
- **No bulk operations:** Cannot delete multiple mappings at once
- **No mapping templates:** Cannot save/reuse common mapping patterns
- **No drag-to-reorder:** Mappings table order fixed (chronological)

**8.2 Potential Enhancements**

- **Mapping preview:** Show sample output before saving
- **Schema diff viewer:** Compare combined vs destination schemas side-by-side
- **Instruction templates:** Auto-generate instructions for common patterns (passthrough, rename, etc.)
- **Validation improvements:** Real-time validation as user types (debounced)
- **Mapping search/filter:** Filter mappings by primary, source, destination

**8.3 Performance Optimizations**

- **Virtualized table:** For Combiners with 100+ mappings
- **Lazy schema loading:** Only load schemas when drawer opened
- **Optimistic updates:** Update UI before API confirms save

---

### 9. Related Documentation

**Must Read Before Working on Combiner:**

- [RJSF Guide](../guides/RJSF_GUIDE.md) - Complete RJSF reference
- [Protocol Adapter Architecture](../architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md) - Backend-driven schemas
- [Testing Guide](../guides/TESTING_GUIDE.md) - Component testing patterns
- [Cypress Guide](../guides/CYPRESS_GUIDE.md) - E2E testing reference

**Related Architecture:**

- [DataHub Architecture](../architecture/DATAHUB_ARCHITECTURE.md) - Schema management in DataHub
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md) - React Flow patterns

**API Documentation:**

- OpenAPI spec: `/api/openapi.yaml`
- TypeScript types: `src/api/__generated__/types.ts`

---

### 10. Screenshot Placeholders

**To be captured and inserted:**

1. **Introduction:**

   - [ ] Native RJSF form (flat, all fields visible)
   - [ ] Custom Combiner UX (tabs, table, drawer)
   - [ ] Tab navigation showing 3 sections

2. **PrimarySelect:**

   - [ ] Dropdown open showing tag/topic filter options
   - [ ] Key icon prefix visible
   - [ ] Validation error displayed

3. **CombinedEntitySelect:**

   - [ ] Dropdown open with adapter options
   - [ ] Rich option display (icon, name, description, integration points)
   - [ ] Multi-select chips showing selected entities

4. **DataCombiningEditorField:**

   - [ ] Full editor showing split layout
   - [ ] Sources panel on left with entity selector, primary selector
   - [ ] Destination panel on right with topic, schema
   - [ ] Schema loaders showing loaded state
   - [ ] Instruction list below grid

5. **DataCombiningTableField:**

   - [ ] Table with 3+ mappings showing summaries
   - [ ] Row hover state
   - [ ] Action buttons visible
   - [ ] Drawer open with editor field visible
   - [ ] Empty state with CTA

6. **Error States:**

   - [ ] Schema loading error
   - [ ] Validation error summary at top
   - [ ] Inline validation error below field

7. **Loading States:**
   - [ ] Schema loader skeleton
   - [ ] Entity selector loading
   - [ ] Table loading state

**Screenshot Format:**

- PNG format, 1920x1080 max resolution
- Stored in: `docs/assets/screenshots/combiner/`
- Named: `combiner-{component}-{state}.png`
- Alt text in markdown for accessibility

---

## Document Metadata

**Target Location:** `docs/walkthroughs/RJSF_COMBINER.md`

**Estimated Length:** 1200-1500 lines

**Frontmatter:**

```yaml
---
title: 'Combiner UX Walkthrough'
description: 'How we transformed flat RJSF forms into interactive UX for data mapping configuration'
tags: ['rjsf', 'combiner', 'ux', 'forms', 'walkthrough']
audience: ['developers', 'ux-designers', 'technical-leads']
related:
  - docs/guides/RJSF_GUIDE.md
  - docs/architecture/PROTOCOL_ADAPTER_ARCHITECTURE.md
  - docs/guides/TESTING_GUIDE.md
  - docs/guides/CYPRESS_GUIDE.md
last_updated: 2026-02-16
---
```

**Quality Criteria:**

- ✅ Two perspectives maintained (UX + Technical) throughout
- ✅ All 4 widgets documented with both perspectives
- ✅ Data lifecycle explained with code examples
- ✅ Cross-references to related docs
- ✅ Screenshot placeholders with descriptions
- ✅ Testing strategy included
- ✅ Common pitfalls documented
- ✅ Code locations provided (no code duplication)
- ✅ Accessible to both designers and developers

---

**Status:** ✅ Plan Complete - Ready for Document Creation
**Next Step:** Create `docs/walkthroughs/RJSF_COMBINER.md` based on this plan
