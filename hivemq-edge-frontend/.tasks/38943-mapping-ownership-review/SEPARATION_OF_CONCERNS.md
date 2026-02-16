# Separation of Concerns Analysis

## Overview

The mapping ownership issue reveals a deeper architectural problem: **ownership logic is scattered across display, queries, and validation layers**, creating a distributed system of responsibilities that's difficult to maintain and prone to inconsistencies.

## Current State: Scattered Concerns

### Logic Distribution Map

```mermaid
graph TD
    subgraph "Query Layer"
        Q1[useDomainModel<br/>Fetches with adapterId]
        Q2[buildQueriesForCombiner<br/>Index-based pairing]
        Q3[buildOptionsForCombiner<br/>Combines queries]
    end

    subgraph "Display Layer"
        D1["CombinedEntitySelect<br/>Extracts IDs ❌"]
        D2[DataCombiningTableField<br/>Shows string values]
        D3[PrimarySelect<br/>Reconstructs context]
    end

    subgraph "Validation Layer"
        V1[useValidateCombiner<br/>Validates existence]
        V2[Status checking<br/>Operational matching]
    end

    subgraph "Instruction Layer"
        I1[DestinationSchemaLoader<br/>Auto-generates with scope]
        I2[MappingInstruction<br/>Uses sourceRef]
    end

    subgraph "Storage Layer"
        S1["sources.tags string[]<br/>❌ No ownership"]
        S2["instructions[].sourceRef<br/>✅ Has ownership"]
    end

    Q1 --> D1
    Q1 --> D3
    Q2 --> D1
    Q3 --> D1

    D1 --> S1
    D3 --> S1

    S1 --> V1
    S2 --> V2

    I1 --> S2
    I2 --> S2

    style S1 fill:#f99
    style D1 fill:#f99
    style V1 fill:#f99

    note1["❌ Ownership logic<br/>scattered across layers"]

```

### Problem: Multiple Sources of Truth

```mermaid
graph LR
    A[Ownership Information] --> B[Query Layer<br/>Has adapterId]
    A --> C[Storage Layer<br/>Missing from arrays]
    A --> D[Instruction Layer<br/>Has scope]

    B -.->|Lost during| E[Display Layer]
    E -.->|Saves without| C

    D -.->|Separate path| C

    F["❌ THREE different<br/>representations of<br/>ownership"]

    style F fill:#f99,stroke:#333,stroke-width:3px
```

## Detailed Analysis by Layer

### 1. Query Layer: Has Context But Doesn't Enforce

**Files:**

- `useDomainModel.ts:24-85`
- `combining.utils.ts:26-69, 71-101`

**Responsibilities:**

- ✅ Fetch data from multiple adapters
- ✅ Track `adapterId` with each entity
- ✅ Return `DomainModel<T>` with full context
- ❌ **Doesn't enforce** that consumers preserve this context

**Current Implementation:**

```typescript
// useDomainModel.ts
export interface DomainModel<T> extends DataIdentifierReference {
  adapterId: string // ✅ Context provided
  node?: T
}

// Returns full context
const { data: options } = useDomainModel(DomainModelTypeEnum.tag)
// options: DomainModel<Tag>[] with adapterId
```

**Problem:** Query layer provides ownership context, but has no control over what happens next.

**Coupling Issues:**

```mermaid
graph TD
    A[Query Layer] -->|Provides DomainModel| B[Display Layer]
    B -->|Extracts string| C[Storage Layer]

    D[Query Layer] -.->|Should validate| C

    X["❌ No feedback loop:<br/>Query doesn't know<br/>Display discarded context"]

    style X fill:#f99
```

### 2. Display Layer: Transformation Without Preservation

**Files:**

- `CombinedEntitySelect.tsx:45-82`
- `DataCombiningEditorField.tsx:96-136`
- `DataCombiningTableField.tsx:44-57`
- `PrimarySelect.tsx:24-49`

**Current Responsibilities:**

- ✅ Present options to user
- ✅ Handle user selection
- ❌ **Transform** full context to partial data
- ❌ **Reconstruct** context from partial data

**The Transformation Problem:**

```mermaid
sequenceDiagram
    participant Q as Query Layer
    participant D as Display Layer
    participant S as Storage Layer

    Q->>D: DomainModel[] with adapterId
    Note over Q: Full context

    D->>D: User selects
    Note over D: ❌ TRANSFORMATION:<br/>DomainModel → string

    D->>S: string[] without adapterId
    Note over S: ❌ Partial context

    rect rgb(255, 200, 200)
        Note over D,S: Information loss<br/>in display layer
    end
```

**Code Example - The Transformation:**

```typescript
// CombinedEntitySelect.tsx:45-82
const CombinedEntitySelect: FC<Props> = ({ value, onChange }) => {
  // Receives full context from query layer
  const options = useMemo(() => {
    return buildOptionsForCombiner(domainTagQueries)
    // Returns: DomainModel<Tag>[] with adapterId ✅
  }, [domainTagQueries])

  // ❌ LOSES CONTEXT HERE
  const handleOnChange = useCallback(
    (value: MultiValue<DomainModel<unknown>>) => {
      onChange?.(
        value.map((val) => val.id)  // ❌ Transformation: Object → String
      )
    },
    [onChange]
  )

  return <Select options={options} onChange={handleOnChange} />
}
```

**Why This is Problematic:**

1. **Display has too much power:** Can arbitrarily transform data structure
2. **No type enforcement:** Nothing prevents extracting only partial data
3. **Silent data loss:** No warning that context is being discarded
4. **Reconstruction burden:** Other components must rebuild what was lost

**The Reconstruction Problem:**

```typescript
// PrimarySelect.tsx:24-49
const PrimarySelect: FC<Props> = ({ value }) => {
  // Must reconstruct context from other sources
  const { data: options } = useDomainModel(DomainModelTypeEnum.tag)

  // If value is just a string "tag1", how do we know which adapter?
  // Must query ALL adapters to find it
  const displayValue = options?.find((opt) => opt.id === value)

  // ❌ Inefficient: O(n) search across all adapters
  // ❌ Ambiguous: Multiple adapters might have same ID
}
```

### 3. Storage Layer: Dual Representation

**Files:**

- `DataCombining.ts:1-47`
- Form state in React Hook Form

**Current Responsibilities:**

- ❌ Store **TWO representations** of same data:
  - `sources.tags[]` - Partial (strings)
  - `instructions[].sourceRef` - Complete (with scope)
- ❌ Keep them synchronized
- ❌ Handle migration between formats

**Dual Representation Diagram:**

```mermaid
graph TD
    subgraph "Storage Layer"
        A["sources.tags:<br/>string[]"]
        B["instructions[].sourceRef:<br/>DataIdentifierReference"]
    end

    A -.->|Should match| B
    B -.->|Should match| A

    C[Display reads from A]
    D[Validation reads from A]
    E[Backend uses B]
    F[Status checking uses both]

    A --> C
    A --> D
    B --> E
    A --> F
    B --> F

    G["❌ Synchronization risk:<br/>A and B can diverge"]

    style A fill:#f99
    style B fill:#9f9
    style G fill:#f99
```

**Synchronization Problems:**

```typescript
// Scenario: Instructions updated but arrays not synced
{
  sources: {
    tags: ["tag1", "tag3"]  // ❌ Out of date
  },
  instructions: [
    {
      sourceRef: { id: "adapter1", type: "adapter", scope: "adapter1" },
      sourcePath: ["tag2"],  // ✅ Updated to tag2, not tag1
      // ...
    }
  ]
}

// Now display shows "tag1" but instructions reference "tag2"
// ❌ INCONSISTENT STATE
```

### 4. Validation Layer: Incomplete Information

**Files:**

- `useValidateCombiner.ts:149-175, 238-260`
- `status-adapter-edge-operational.utils.ts`

**Current Responsibilities:**

- ✅ Validate tag existence
- ❌ **Cannot validate ownership** (missing context)
- ✅ Validate instruction structure
- ⚠️ **Different logic** for tags vs. instructions

**Validation Split:**

```mermaid
graph TD
    subgraph "Validation Logic"
        V1[validateTags<br/>Sources validation]
        V2[validateInstructions<br/>Instruction validation]
    end

    V1 -->|Reads| S1["sources.tags:<br/>string[]"]
    V2 -->|Reads| S2["instructions[].sourceRef:<br/>DataIdentifierReference"]

    V1 --> R1["❌ Can only check:<br/>Tag exists SOMEWHERE"]
    V2 --> R2["✅ Can check:<br/>sourceRef valid with scope"]

    style V1 fill:#f99
    style V2 fill:#9f9
    style R1 fill:#f99
    style R2 fill:#9f9
```

**Code Example - Incomplete Validation:**

```typescript
// useValidateCombiner.ts:149-175
const validateTags = (tags: string[], domainEntities: DomainModel<Tag>[]) => {
  return tags.every(
    (tag) => domainEntities.some((entity) => entity.id === tag)
    // ❌ Can only check ID match
    // ❌ Cannot verify: entity.adapterId === correctAdapter
    //    because tag is just a string, no adapter info
  )
}

const validateInstructions = (instructions: Instruction[], domainEntities: DomainModel<Tag>[]) => {
  return instructions.every((inst) =>
    domainEntities.some(
      (entity) => entity.id === inst.sourceRef?.id && entity.adapterId === inst.sourceRef?.scope // ✅ Can validate ownership
    )
  )
}
```

**Problem:** Two different validation strategies for logically related data.

### 5. Instruction Layer: Does It Right

**Files:**

- `DestinationSchemaLoader.tsx:87-103`
- `MappingInstruction.tsx:49, 85-92`

**Responsibilities:**

- ✅ Auto-generate instructions with full context
- ✅ Store complete `DataIdentifierReference` with scope
- ✅ Display with full ownership information

**What This Layer Gets Right:**

```typescript
// DestinationSchemaLoader.tsx:87-103
const instructionsFromDestinationSchema = useMemo(() => {
  const instructions: InstructionType[] = []

  destinationSchema.forEach((element) => {
    const sourceRef: DataIdentifierReference = {
      id: element.adapter || firstAdapter,
      type: 'adapter',
      // ✅ Scope included from the start
    }

    instructions.push({
      sourceRef, // ✅ Full reference, no transformation
      sourcePath: [element.tag],
      destinationPath: [element.name],
    })
  })

  return instructions
}, [destinationSchema, firstAdapter])
```

**Why This Works:**

- ✅ **No transformation:** Query → Storage preserves full context
- ✅ **Single representation:** One source of truth
- ✅ **Type enforced:** `DataIdentifierReference` required
- ✅ **Validation works:** Has all needed information

## Risk Analysis: Distributed Logic

### Risk 1: Logic Duplication

**Current State:**
Multiple components implement similar ownership logic:

```mermaid
graph TD
    A[Ownership Logic] -.-> B[Query: Extract adapterId]
    A -.-> C[Display: Transform to string]
    A -.-> D[Validation: Check existence]
    A -.-> E[Status: Match by adapter]
    A -.-> F[Instruction: Store with scope]

    G["❌ 5 different places<br/>handling ownership"]

    style G fill:#f99,stroke:#333,stroke-width:2px
```

**Examples:**

```typescript
// 1. Query layer - combining.utils.ts:26-69
const adapterId = queryData?.[0]?.adapterId // Extract from first item

// 2. Display layer - CombinedEntitySelect.tsx:45-82
const handleOnChange = (value) => {
  onChange?.(value.map((val) => val.id)) // Extract ID only
}

// 3. Validation layer - useValidateCombiner.ts:149-175
const validateTags = (tags, entities) => {
  return tags.every((tag) => entities.some((e) => e.id === tag)) // Match by ID
}

// 4. Status layer - status-adapter-edge-operational.utils.ts
const matchByAdapter = (sources, operational) => {
  // Match using both sources.tags and instructions
}

// 5. Instruction layer - DestinationSchemaLoader.tsx:87-103
const sourceRef = { id: adapter, type: 'adapter' } // Build reference
```

**Problem:** Changes to ownership model require updates in 5+ places.

### Risk 2: Inconsistent Behavior

**Different layers handle ownership differently:**

| Layer                     | Ownership Representation | Validation Strategy |
| ------------------------- | ------------------------ | ------------------- |
| Query                     | `adapterId` field        | ✅ Full context     |
| Display                   | Extracted `id` only      | ❌ Context lost     |
| Storage (tags)            | `string`                 | ❌ No context       |
| Storage (instructions)    | `scope` field            | ✅ Full context     |
| Validation (tags)         | ID match only            | ⚠️ Partial          |
| Validation (instructions) | ID + scope match         | ✅ Complete         |

**Example Inconsistency:**

```typescript
// Scenario: Two adapters have tag with same ID
const adapter1Tags = [{ adapterId: "adapter1", id: "temperature", ... }]
const adapter2Tags = [{ adapterId: "adapter2", id: "temperature", ... }]

// User selects "temperature" from adapter1
// Display stores: "temperature"  ❌ Which adapter?

// Validation checks: Does "temperature" exist in ANY adapter?
// Result: ✅ Valid (finds it in both adapters)

// Actual intent: Should be adapter1's temperature
// ❌ FALSE POSITIVE - Validated wrong tag
```

### Risk 3: Brittle Refactoring

**Tight coupling between layers:**

```mermaid
graph TD
    A[Query Layer<br/>DomainModel structure] --> B[Display Layer<br/>Depends on DomainModel]
    B --> C[Storage Layer<br/>Depends on Display output]
    C --> D[Validation Layer<br/>Depends on Storage format]

    E[Change Query structure] --> F[Must update Display]
    F --> G[Must update Storage]
    G --> H[Must update Validation]

    I["❌ Cascading changes<br/>across all layers"]

    style I fill:#f99
```

**Example Refactoring Challenge:**

```typescript
// If we change DomainModel structure:
interface DomainModel<T> {
  adapterId: string // ❌ Change to: adapterIdentifier
  id: string
  type: string
  node?: T
}

// Must update:
// 1. buildOptionsForCombiner - Extract from queryData
// 2. CombinedEntitySelect - Read from options
// 3. DataCombiningEditorField - Handle onChange
// 4. PrimarySelect - Reconstruct context
// 5. useValidateCombiner - Match logic
// 6. Status checking - Operational matching

// ❌ 6+ files affected by single field rename
```

### Risk 4: Testing Complexity

**Must test coordination between layers:**

```mermaid
graph TD
    T1[Unit Test Query] -.->|"Pass ✅"| X
    T2[Unit Test Display] -.->|"Pass ✅"| X
    T3[Unit Test Validation] -.->|"Pass ✅"| X

    X[All units pass] -.-> Y{Integration Test}

    Y -->|"Fail ❌"| Z["Information loss<br/>between layers"]

    style Z fill:#f99
```

**Testing Burden:**

```typescript
// Must test:
// 1. Query returns correct data ✅
// 2. Display handles query data ✅
// 3. Display transforms correctly ⚠️
// 4. Storage receives correct format ⚠️
// 5. Validation reads storage ✅
// 6. Validation logic is correct ✅
// 7. Query → Display → Storage → Validation (INTEGRATION) ❌

// Steps 3, 4, 7 are where bugs hide
```

### Risk 5: Knowledge Distribution

**Understanding requires knowledge of multiple layers:**

```mermaid
graph LR
    D1[Developer 1<br/>Query Expert] -.->|Doesn't know| L1[How Display transforms data]
    D2[Developer 2<br/>Display Expert] -.->|Doesn't know| L2[How Validation uses data]
    D3[Developer 3<br/>Validation Expert] -.->|Doesn't know| L3[How Query provides data]

    P["❌ No single developer<br/>understands full flow"]

    style P fill:#f99,stroke:#333,stroke-width:2px
```

**Documentation Burden:**

- Each layer must document its expectations
- Interfaces become implicit contracts
- Changes require cross-team coordination

## Proper Separation of Concerns

### Ideal Architecture

```mermaid
graph TD
    subgraph "Data Layer (Single Responsibility)"
        D1[Domain Model<br/>Ownership logic ONLY here]
    end

    subgraph "Query Layer"
        Q1[Fetch + Transform to Domain Model]
    end

    subgraph "Display Layer"
        V1[Present Domain Model<br/>NO transformation]
    end

    subgraph "Storage Layer"
        S1[Store Domain Model<br/>NO transformation]
    end

    subgraph "Validation Layer"
        VA1[Validate Domain Model<br/>Using ownership rules]
    end

    D1 -.->|Defines structure| Q1
    D1 -.->|Defines structure| V1
    D1 -.->|Defines structure| S1
    D1 -.->|Defines structure| VA1

    Q1 -->|Domain Model| V1
    V1 -->|Domain Model| S1
    S1 -->|Domain Model| VA1

    style D1 fill:#9f9,stroke:#333,stroke-width:3px
```

### Principle: Data Preservation Through Layers

```mermaid
sequenceDiagram
    participant Q as Query
    participant D as Display
    participant S as Storage
    participant V as Validation

    Q->>D: DomainModel (full)
    Note over D: ✅ Preserve structure<br/>NO transformation

    D->>S: DomainModel (full)
    Note over S: ✅ Store as-is<br/>NO transformation

    S->>V: DomainModel (full)
    Note over V: ✅ Validate complete data

    rect rgb(200, 255, 200)
        Note over Q,V: Information preserved<br/>through entire flow
    end
```

### Clear Responsibilities

| Layer           | Should Do                         | Should NOT Do                        |
| --------------- | --------------------------------- | ------------------------------------ |
| **Query**       | Fetch data, build DomainModel     | ❌ Know about display/validation     |
| **Display**     | Present DomainModel to user       | ❌ Transform or extract fields       |
| **Storage**     | Store DomainModel as-is           | ❌ Maintain multiple representations |
| **Validation**  | Validate DomainModel fields       | ❌ Reconstruct missing data          |
| **Instruction** | Generate instructions from schema | ❌ Duplicate storage logic           |

## How Each Solution Affects Separation of Concerns

### Option A: Upgrade Arrays to DataIdentifierReference[]

**Impact on Separation:**

```mermaid
graph TD
    subgraph "Single Domain Model"
        DM[DataIdentifierReference<br/>Used everywhere]
    end

    Q[Query] -->|Returns| DM
    D[Display] -->|Presents| DM
    S[Storage] -->|Stores| DM
    V[Validation] -->|Validates| DM

    DM --> O["✅ Single source of truth<br/>✅ No transformation<br/>✅ Consistent behavior"]

    style DM fill:#9f9,stroke:#333,stroke-width:3px
    style O fill:#9f9
```

**Benefits:**

- ✅ **Single representation** throughout entire flow
- ✅ **No transformation** between layers
- ✅ **Type enforcement** prevents information loss
- ✅ **Consistent validation** for all data
- ✅ **Easy testing** - Same structure everywhere

**Code Example:**

```typescript
// Query Layer
const { data: options }: DomainModel<Tag>[] = useDomainModel()

// Display Layer
const handleOnChange = (value: MultiValue<DomainModel<Tag>>) => {
  onChange?.(
    value.map(v => ({
      id: v.id,
      type: v.type,
      scope: v.adapterId  // ✅ Preserve full structure
    }))
  )
}

// Storage Layer
sources: {
  tags: DataIdentifierReference[]  // ✅ Same structure as input
}

// Validation Layer
const validateTags = (
  tags: DataIdentifierReference[],  // ✅ Complete data
  entities: DomainModel<Tag>[]
) => {
  return tags.every(tag =>
    entities.some(e =>
      e.id === tag.id &&
      e.adapterId === tag.scope  // ✅ Can validate ownership
    )
  )
}
```

### Option B: Remove Arrays, Use Only Instructions

**Impact on Separation:**

```mermaid
graph TD
    subgraph "Single Storage"
        I["instructions[]<br/>Only source"]
    end

    D[Display] -->|Must reconstruct| I
    V[Validation] -->|Must reconstruct| I
    S[Status] -->|Must reconstruct| I

    I --> O["⚠️ Single source BUT<br/>❌ Reconstruction logic<br/>in multiple places"]

    style I fill:#ff9,stroke:#333
    style O fill:#f99
```

**Problems:**

- ❌ **Reconstruction logic duplicated** in display, validation, status
- ❌ **Violates DRY principle** - Same extraction code everywhere
- ❌ **Display layer does business logic** - Reconstructing from instructions
- ❌ **Cannot migrate** - Breaks separation completely for old data

### Option C: Arrays as Display-Only

**Impact on Separation:**

```mermaid
graph TD
    I["instructions[]<br/>Source of truth"] --> S[Sync Logic]
    S --> A["sources.tags[]<br/>Display cache"]

    D[Display] -.->|Reads| A
    V[Validation] -.->|Reads| I
    E[Edit] -.->|Updates| I

    O["⚠️ Dual representation<br/>⚠️ Sync responsibility<br/>❌ Not truly separate"]

    style S fill:#ff9,stroke:#333
    style O fill:#f99
```

**Problems:**

- ⚠️ **Sync logic is a new concern** - Where does it live?
- ⚠️ **Display vs. Validation use different sources** - Inconsistency risk
- ⚠️ **Arrays become "cache"** - Cache invalidation is hard
- ❌ **Violates single responsibility** - Sync logic touches multiple layers

### Option D: Parallel Arrays with Ownership

**Impact on Separation:**

```mermaid
graph TD
    D[Display/Edit] --> DW[Dual Write Logic]

    DW --> A1[sources.tags<br/>Old format]
    DW --> A2[sources.tagsRefs<br/>New format]

    V[Validation] --> R[Read Logic:<br/>Try new, fallback old]

    R --> A1
    R --> A2

    O["❌ Worst separation:<br/>❌ Dual write everywhere<br/>❌ Conditional read everywhere<br/>❌ Transition logic forever"]

    style DW fill:#f99,stroke:#333
    style R fill:#f99,stroke:#333
    style O fill:#f99
```

**Problems:**

- ❌ **Every layer must know about both formats**
- ❌ **Dual write logic scattered** across components
- ❌ **Validation becomes complex** - Check which format to use
- ❌ **Technical debt increases** - More distributed logic
- ❌ **Cleanup phase may never happen** - Forever maintaining both

### Option E: Runtime Reconstruction

**Impact on Separation:**

```mermaid
graph TD
    S1["sources.tags[]<br/>Partial data"] --> R[Reconstruction Utils]
    S2["instructions[]<br/>Full data"] --> R

    R --> M[Map<id, ownership>]

    D[Display] -.->|Uses| M
    V[Validation] -.->|Uses| M
    ST[Status] -.->|Uses| M

    O["❌ Reconstruction logic<br/>becomes its own layer<br/>❌ Every consumer depends on it"]

    style R fill:#f99,stroke:#333
    style O fill:#f99
```

**Problems:**

- ❌ **New intermediary layer** - Reconstruction becomes a concern
- ❌ **Performance overhead** - Build map on every render
- ❌ **Implicit dependency** - Every component needs reconstruction
- ❌ **Workaround becomes architecture** - Temporary fix becomes permanent
- ❌ **Doesn't fix root cause** - Information still lost, just reconstructed

## Recommendations

### 1. Choose Option A for Proper Separation

**Reasoning:**

- ✅ Single domain model throughout all layers
- ✅ No transformation or reconstruction
- ✅ Each layer has clear, single responsibility
- ✅ Type system enforces proper separation
- ✅ Easy to understand and maintain

### 2. Establish Clear Boundaries

```typescript
// Define domain model at top level
// All layers MUST use this exact structure
export interface DataIdentifierReference {
  id: string
  type: 'adapter' | 'tag' | 'metric' | 'behavior'
  scope: string // Ownership
}

// Query Layer: Build domain model
export function useDomainModel(): DomainModel<T>[] {
  // Fetch and transform TO domain model
  // NO other layer should do this transformation
}

// Display Layer: Present domain model
export function CombinedEntitySelect({
  onChange,
}: {
  onChange: (value: DataIdentifierReference[]) => void // ✅ Type enforced
}) {
  // Present options, return SAME structure
  // NO transformation allowed
}

// Storage Layer: Store domain model
export interface Sources {
  tags?: DataIdentifierReference[] // ✅ Exact same structure
}

// Validation Layer: Validate domain model
export function validateTags(
  tags: DataIdentifierReference[], // ✅ Complete data
  entities: DomainModel<T>[]
): boolean {
  // Validate with full context
  // NO reconstruction needed
}
```

### 3. Enforce with TypeScript

```typescript
// Make transformation impossible
type PreserveStructure<T> = T extends DataIdentifierReference
  ? DataIdentifierReference // ✅ Must return same structure
  : never

// Use in components
function CombinedEntitySelect<T extends DataIdentifierReference>({
  value,
  onChange,
}: {
  value?: T[]
  onChange?: (value: PreserveStructure<T>[]) => void // ✅ Enforced
}) {
  // TypeScript prevents returning different structure
}
```

### 4. Single Point of Transformation

**Only ONE place should transform data: Query Layer**

```typescript
// ONLY useDomainModel should do this:
const domainModel = rawApiData.map((item) => ({
  id: item.id,
  type: item.type,
  scope: item.adapterId, // ✅ Transform once, here only
  adapterId: item.adapterId,
  node: item,
}))

// After this, NO other layer transforms
// Everyone uses DomainModel / DataIdentifierReference as-is
```

## Summary: Risks of Distributed Logic

| Risk                       | Current State          | Option A                 | Other Options     |
| -------------------------- | ---------------------- | ------------------------ | ----------------- |
| **Logic Duplication**      | 🔴 5+ places           | ✅ 1 place               | 🔴 More places    |
| **Inconsistent Behavior**  | 🔴 Different per layer | ✅ Consistent            | ⚠️ More complex   |
| **Brittle Refactoring**    | 🔴 Cascading changes   | ✅ Isolated              | 🔴 More cascading |
| **Testing Complexity**     | 🔴 Integration issues  | ✅ Unit tests sufficient | 🔴 Worse          |
| **Knowledge Distribution** | 🔴 No single owner     | ✅ Clear structure       | 🔴 More confusion |
| **Maintainability**        | 🔴 Hard to change      | ✅ Easy to change        | 🔴 Harder         |

**Conclusion:** Option A (Upgrade Arrays to DataIdentifierReference[]) is the only solution that properly addresses separation of concerns by establishing a single domain model used consistently throughout all layers.
