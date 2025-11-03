# Data Hub Designer Architecture

**Document Purpose:** Deep understanding of the DataHub designer architecture, state management, and data flow for implementing the policy success summary feature.

---

## Core Concepts

### Policy Types

The DataHub supports two policy types:

1. **Data Policies** (`DataHubNodeType.DATA_POLICY`)

   - Apply data transformation on MQTT messages
   - Defined by `DataPolicy` type
   - Can include validators, schemas, operations, functions

2. **Behavior Policies** (`DataHubNodeType.BEHAVIOR_POLICY`)
   - Define behavior of MQTT clients
   - Defined by `BehaviorPolicy` type
   - Include transitions, events, client filters

---

## State Management Architecture

### 1. Draft Store (`useDataHubDraftStore`)

**Location:** `src/extensions/datahub/hooks/useDataHubDraftStore.ts`

**Responsibilities:**

- Manages the canvas state (nodes and edges)
- Tracks designer status: `DRAFT`, `LOADED`, `MODIFIED`
- Handles node and edge changes (React Flow events)
- Stores policy metadata (name, type)

**Key State:**

```typescript
{
  nodes: Node[]              // React Flow nodes
  edges: Edge[]              // React Flow connections
  status: DesignerStatus     // DRAFT | LOADED | MODIFIED
  name: string               // Policy name
  type: DataHubNodeType      // DATA_POLICY | BEHAVIOR_POLICY
}
```

**Key Actions:**

- `onNodesChange()` - React Flow node updates
- `onEdgesChange()` - React Flow edge updates
- `onConnect()` - New edge connections
- `onAddNodes()` - Add nodes to canvas
- `onUpdateNodes()` - Update node data

### 2. Policy Checks Store (`usePolicyChecksStore`)

**Location:** `src/extensions/datahub/hooks/usePolicyChecksStore.ts`

**Responsibilities:**

- Manages validation state and results
- Stores dry-run report
- Tracks validation status
- Provides error extraction

**Key State:**

```typescript
{
  node: Node | undefined           // Currently selected node
  report: DryRunResults[]          // Validation results
  status: PolicyDryRunStatus       // IDLE | RUNNING | SUCCESS | FAILURE
}
```

**Key Actions:**

- `initReport()` - Start validation (set status to RUNNING)
- `setReport(report)` - Store validation results and determine SUCCESS/FAILURE
- `getErrors()` - Extract failed results as ProblemDetailsExtended[]
- `setNode(node)` - Track selected node
- `reset()` - Clear validation state

**Status Determination Logic:**

```typescript
setReport: (report: DryRunResults<unknown, never>[]) => {
  const failedResults = report.filter((result) => !!result.error)
  set({
    report: report,
    status: failedResults.length ? PolicyDryRunStatus.FAILURE : PolicyDryRunStatus.SUCCESS,
  })
}
```

---

## Validation & Dry Run Flow

### DryRunResults Structure

```typescript
interface DryRunResults<T, R = never> {
  node: Node // The node being validated
  data?: T // Serialized policy/resource data
  error?: ProblemDetailsExtended // Validation error (if any)
  resources?: DryRunResults<R>[] // Nested resources (schemas, scripts)
}
```

**CRITICAL INSIGHT - Report Array Structure:**

The `DryRunResults[]` array is structured as:

1. **One item per node** in the designer (validators, schemas, scripts, operations, filters, etc.)
   - Each has `node` reference to the designer node
   - Each has `error` if that specific node's validation failed
   - Each has `data` with the node's configuration/payload
2. **PLUS a final summary item** - The complete policy validation (MOST IMPORTANT!)
   - This is NOT just validating the policy node
   - This validates the ENTIRE policy as a whole
   - Contains the **complete JSON payload** ready for publishing
   - Contains **ALL resources** (schemas, scripts) in `resources[]` array
   - Still has `node` reference (to the policy node)
   - **Does NOT represent individual nodes** - it's a synthetic aggregate

**Example Report Structure:**

```typescript
report: DryRunResults[] = [
  // Per-node validation items
  { node: topicFilterNode, data: {...}, error?: ProblemDetails },    // Node 1
  { node: validatorNode, data: {...}, error?: ProblemDetails },      // Node 2
  { node: schemaNode1, data: PolicySchema, error?: ProblemDetails }, // Node 3
  { node: operationNode, data: {...}, error?: ProblemDetails },      // Node 4
  { node: scriptNode1, data: Script, error?: ProblemDetails },       // Node 5
  { node: policyNode, data: {...}, error?: ProblemDetails },         // Node 6
  // ... one validation result per node in designer ...

  // FINAL SUMMARY ITEM (array length - 1) - Complete policy validation
  {
    node: policyNode,                    // Reference to the policy node
    data: DataPolicy | BehaviorPolicy,   // COMPLETE policy JSON ready to publish
    error: undefined,                    // No error on success
    resources: [                         // ALL resources needed by this policy
      {
        node: schemaNode1,
        data: PolicySchema,              // Complete schema definition
        error: undefined
      },
      {
        node: scriptNode1,
        data: Script,                    // Complete script definition
        error: undefined
      },
      // ... all schemas and scripts used by the policy
    ]
  }
]
```

**Key Distinctions:**

| Per-Node Items            | Final Summary Item              |
| ------------------------- | ------------------------------- |
| One per designer node     | One per policy (last in array)  |
| Validates individual node | Validates complete policy       |
| May have errors           | Only present if policy is valid |
| Partial data              | Complete JSON payload           |
| No resources array        | Contains ALL resources          |
| Used for error reporting  | Used for publishing             |

**Why This Matters for Implementation:**

1. **For Error Display:** Use per-node items to show which specific nodes have issues
2. **For Success Summary:** Use the FINAL item exclusively - it has everything:

   - Complete policy data (`data` field)
   - All resources with their complete definitions (`resources[]` array)
   - This is what gets published

3. **Accessing the Final Item:**

```typescript
const finalSummary = [...report].pop() // Last item in array
const policyPayload = finalSummary.data // Complete policy JSON
const allResources = finalSummary.resources // All schemas + scripts
```

### Validation Process

1. User clicks "Check" button
2. `usePolicyDryRun` hook processes the canvas
3. Validates each node and connection
4. Builds `DryRunResults[]` array
5. Sets report in `usePolicyChecksStore`
6. Panel shows results based on status

---

## Publishing Flow

### ToolbarPublish Component

**Location:** `src/extensions/datahub/components/toolbar/ToolbarPublish.tsx`

**Publishing Strategy:**

1. **Extract Resources from Report**

   - Filters report for `SCHEMA` and `FUNCTION` nodes
   - Only includes DRAFT or MODIFIED versions
   - Deduplicates by ID

2. **Publish Resources First**

   - Creates schemas via `createSchema.mutateAsync()`
   - Creates scripts via `createScript.mutateAsync()`
   - Updates draft nodes to published versions

3. **Publish Main Policy**

   - Determines if CREATE (DRAFT) or UPDATE (MODIFIED)
   - Calls appropriate mutation:
     - Data Policy: `createDataPolicy` or `updateDataPolicy`
     - Behavior Policy: `createBehaviorPolicy` or `updateBehaviorPolicy`

4. **Navigate to Published Policy**
   - Resets validation state
   - Navigates to `/datahub/{type}/{id}`

**Key Code Pattern:**

```typescript
const resourceReducer =
  <T extends PolicySchema | Script>(type: DataHubNodeType) =>
  (accumulator: T[], result: DryRunResults<T, never>) => {
    // Filter by type, check for data, validate version status
    // Deduplicate by ID
    // Return accumulated resources
  }

const publishResources = (resources?: DryRunResults<never>[]) => {
  const allSchemas = resources?.reduce(resourceReducer<PolicySchema>(SCHEMA), [])
  const allScripts = resources?.reduce(resourceReducer<Script>(FUNCTION), [])
  // Map to mutations and execute
}
```

---

## Current UI Components

### DryRunPanelController

**Location:** `src/extensions/datahub/components/controls/DryRunPanelController.tsx`

**Structure:**

```
Drawer (right side panel)
├─ DrawerHeader: "Report on policy validity"
├─ DrawerBody
│  └─ Card
│     ├─ CardHeader: <PolicySummaryReport status={status} />
│     └─ CardBody: <PolicyErrorReport errors={getErrors()} />
└─ DrawerFooter
   ├─ <ToolbarPublish /> (only if SUCCESS)
   └─ Close button
```

### PolicySummaryReport (Current Implementation)

**Location:** `src/extensions/datahub/components/helpers/PolicySummaryReport.tsx`

**Current Behavior:**

- Simple Chakra UI `Alert` component
- Shows title and description based on status
- No detailed information
- Status-based alert style: success/warning/error

**Limitations:**

- ❌ No policy details
- ❌ No resource breakdown
- ❌ No visibility into what will be created/modified
- ❌ Just a generic success message

### PolicyErrorReport

**Location:** `src/extensions/datahub/components/helpers/PolicyErrorReport.tsx`

**Structure:**

- Chakra UI `Accordion` with items per error
- Each item shows:
  - Node type (translated)
  - Error detail message
  - "Show in designer" button (fits view to node)
  - "Open configuration" button (opens node editor)

**UX Pattern to Follow:**

- Expandable sections for detail
- Actionable buttons
- Clear visual hierarchy

---

## Data Flow for Success Summary

### Information Available in SUCCESS State

When `status === PolicyDryRunStatus.SUCCESS`, the report structure is:

```typescript
report: DryRunResults<unknown, never>[] = [
  // Per-node validation items (for error reporting)
  { node: topicFilterNode, data: {...}, error: undefined },
  { node: validatorNode, data: {...}, error: undefined },
  { node: schemaNode1, data: PolicySchema, error: undefined },
  { node: operationNode, data: {...}, error: undefined },
  { node: scriptNode1, data: Script, error: undefined },
  { node: policyNode, data: {...}, error: undefined },
  // ... one per designer node ...

  // FINAL SUMMARY ITEM (what we use for success display!)
  {
    node: policyNode,                     // Reference to policy node
    data: DataPolicy | BehaviorPolicy,    // COMPLETE policy JSON
    error: undefined,                     // No errors on success
    resources: [                          // ALL resources for the policy
      {
        node: schemaNode1,
        data: PolicySchema,               // Complete schema definition
        error: undefined,
        resources: []
      },
      {
        node: scriptNode1,
        data: Script,                     // Complete script definition
        error: undefined,
        resources: []
      }
      // ... all schemas and scripts
    ]
  }
]
```

**For Success Summary, We Only Need the Final Item:**

```typescript
const finalSummary = [...report].pop() // Last item = complete policy validation

if (!finalSummary) return // Should not happen on SUCCESS

const policyData = finalSummary.data // Complete policy JSON
const allResources = finalSummary.resources || [] // All schemas + scripts
```

### Key Information to Extract (From Final Summary Item)

**Policy Information** (from `finalSummary.data`):

- Policy ID (`data.id`)
- Policy type (Data/Behavior)
- Creation status (new vs. modification) - determined by `designerStatus`
- Matching criteria (for Data Policies: `data.matching.topicFilters`)
- Transitions (for Behavior Policies: `data.onTransitions`)

**Resource Information** (from `finalSummary.resources[]`):

**For Each Schema:**

- ID (`resource.data.id`)
- Version (`resource.node.data.version`)
- Type (`resource.data.type`: JSON/PROTOBUF)
- Status: DRAFT = new, MODIFIED = update

**For Each Script:**

- ID (`resource.data.id`)
- Version (`resource.node.data.version`)
- Function type (`resource.data.functionType`)
- Status: DRAFT = new, MODIFIED = update

**Version Status Logic:**

```typescript
const { version } = result.node.data as ResourceState
const isNew = version === ResourceWorkingVersion.DRAFT
const isModified = version === ResourceWorkingVersion.MODIFIED
```

---

## Designer Node Types

### Resource Node Data

```typescript
enum ResourceWorkingVersion {
  DRAFT = 'DRAFT', // New resource being created
  MODIFIED = 'MODIFIED', // Existing resource being updated
  // ... other numeric versions for published resources
}

interface ResourceState {
  version: ResourceWorkingVersion | number
  name?: string
  // ... other properties
}
```

### Policy Node Data

```typescript
interface PolicyData {
  id: string
  // ... type-specific properties
}
```

---

## Key Insights for Implementation

### 1. Report Structure is Consistent

The last item in `report[]` is always the main policy:

```typescript
const payload = [...report].pop() // Main policy
const { resources } = payload // Supporting resources
```

### 2. Resource Deduplication is Required

Same schema/script can appear multiple times in the graph. Publishing logic deduplicates by ID.

### 3. Version Status Indicates Action

- `DRAFT` → Will CREATE new version
- `MODIFIED` → Will UPDATE existing version
- Numeric version → Already published (no action)

### 4. Nested Resources Structure

Resources are nested within the main policy result, reflecting their dependency relationship.

### 5. Designer Status Affects Publishing

- `DesignerStatus.DRAFT` → Create new policy
- `DesignerStatus.MODIFIED` → Update existing policy

---

## Translation Keys Structure

**Location:** `src/extensions/datahub/locales/en/translation.json`

**Relevant sections:**

- `workspace.nodes.type_*` - Node type names
- `workspace.dryRun.report.*` - Validation report text
- `publish.*` - Publishing messages
- `resource.*` - Resource-specific text

---

## Next Steps for Implementation

Based on this architecture understanding:

1. **Create Enhanced PolicySummaryReport**

   - Extract policy and resource information from report
   - Display in structured, user-friendly format
   - Follow PolicyErrorReport accordion pattern

2. **Design Resource Breakdown Component**

   - Show schemas and scripts separately
   - Indicate new vs. modified status
   - Display relevant metadata (version, type)

3. **Add JSON View (Optional)**

   - Collapsible section
   - Syntax-highlighted display
   - Copy functionality

4. **Update Translations**

   - Add new keys for detailed summary
   - Maintain consistency with existing patterns

5. **Write Tests**
   - Component tests for new UI
   - Integration tests for data extraction
   - Accessibility tests (per guidelines)
