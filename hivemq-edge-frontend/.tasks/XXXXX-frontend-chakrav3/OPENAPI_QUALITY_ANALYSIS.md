# OpenAPI Specification Quality Analysis

**Document Purpose:** Qualitative and quantitative assessment of the OpenAPI specs quality, focusing on DataHub and Workspace areas, with attention to gaps that the frontend has had to compensate for.

**Date:** December 12, 2025

---

## Executive Summary

The HiveMQ Edge OpenAPI specification provides a solid foundation for API-driven development but has significant gaps in **type safety**, **descriptive power**, and **completeness** that force the frontend to maintain a substantial layer of custom type definitions, enums, and business logic. These gaps are particularly acute in the **DataHub** domain.

---

## 1\. Quantitative Assessment

### 1.1 Generated vs. Custom Types

| Category     | Generated Types | Frontend Custom Types | Ratio |
| :----------- | :-------------- | :-------------------- | :---- |
| **Models**   | 104 files       | \-                    | \-    |
| **Services** | 24 files        | \-                    | \-    |
| **Schemas**  | 104 files       | \-                    | \-    |

**DataHub-specific frontend additions:**

| Custom Type/Enum         | Lines | Purpose                                                                       |
| :----------------------- | :---- | :---------------------------------------------------------------------------- |
| `DataHubNodeType`        | 15    | 11 node types for designer                                                    |
| `StrategyType`           | 4     | Validation strategy (missing from API)                                        |
| `SchemaType`             | 4     | JSON/Protobuf distinction                                                     |
| `BehaviorPolicyType`     | 5     | FSM model types                                                               |
| `StateType`              | 10    | FSM state definitions                                                         |
| `ResourceStatus`         | 4     | Draft/Loaded/Modified states                                                  |
| `OperationData.Function` | 12    | Function ID enums                                                             |
| `OperationData.Handle`   | 8     | Connection handle types                                                       |
| **Node Data Types**      | \~150 | TopicFilterData, ValidatorData, SchemaData, FunctionData, OperationData, etc. |

**Total: \~200+ lines of custom type definitions to supplement generated types**

### 1.2 TODO Comments Referencing OpenAPI Gaps

From the codebase search, there are **11 explicit TODOs** about missing OpenAPI specs:

| Issue \# | Missing Spec                         | Frontend Workaround               |
| :------- | :----------------------------------- | :-------------------------------- |
| 18740    | `StrategyType` enum (ALL_OF, ANY_OF) | Custom enum                       |
| 18755    | `SchemaType` enum (JSON, PROTOBUF)   | Custom enum                       |
| 18757    | `BehaviorPolicyType` enum            | Custom enum                       |
| 18761    | `StateType` enum (FSM states)        | Custom enum                       |
| 18763    | `FunctionDefinition` interface       | Custom interface                  |
| 20139    | `PolicyOperationArguments` type      | Custom type, duplicate of OpenAPI |
| 33539    | `BehaviorPolicyType` \+ arguments    | Custom types                      |
| \-       | `DeviceDataPoint` schema             | Manual schema extraction          |

---

## 2\. Type Safety Issues

### 2.1 Excessive Use of `JsonNode` (Record\<string, any\>)

The generated types have **8 usages** of `JsonNode` which is typed as `Record<string, any>`:

| Model             | Property       | Impact                                   |
| :---------------- | :------------- | :--------------------------------------- |
| `Adapter`         | `config`       | No type safety for adapter configuration |
| `ProtocolAdapter` | `configSchema` | Schema is untyped JSON                   |
| `ProtocolAdapter` | `uiSchema`     | UI hints untyped                         |
| `PolicyOperation` | `arguments`    | Operation args are `Record<string, any>` |
| `PolicySchema`    | `arguments`    | Schema args are `Record<string, string>` |
| `FunctionSpecs`   | `schema`       | JSON Schema is untyped                   |
| `FunctionSpecs`   | `uiSchema`     | UI Schema is untyped                     |

**Impact:** Frontend cannot type-check API responses without runtime validation.

### 2.2 Loose String Types Instead of Enums

| Model Property                 | OpenAPI Type    | Should Be                  |
| :----------------------------- | :-------------- | :------------------------- | ------------------- | ---------------- |
| `PolicySchema.type`            | `string`        | `'JSON'                    | 'PROTOBUF'`         |
| `Script.functionType`          | `enum` ✓        | Correctly typed            |
| `BehaviorPolicyBehavior.id`    | `string`        | `'Mqtt.events'             | 'Publish.duplicate' | 'Publish.quota'` |
| `PolicyOperation.functionId`   | `string`        | Enum of valid function IDs |
| `ProtocolAdapter.capabilities` | `Array<string>` | Correctly typed as union ✓ |

### 2.3 Missing Discriminated Unions

The DataHub domain has polymorphic types that would benefit from discriminated unions:

```ts
// Current: No discrimination possible
type PolicySchema = {
  arguments?: Record<string, string> // Could be JSON args OR Protobuf args
  type: string // No type narrowing available
}

// Better: Discriminated union
type PolicySchema = { type: 'JSON'; arguments?: {} } | { type: 'PROTOBUF'; arguments: { messageType: string } }
```

---

## 3\. Completeness Issues

### 3.1 DataHub: Missing FSM (Finite State Machine) Types

The Behavior Policy FSM is documented only in a Mermaid diagram in the frontend README, not in OpenAPI:

**Missing from API:**

- State definitions (Initial, Connected, Disconnected, Violated, Publishing, etc.)
- State transition rules
- Event types and their allowed transitions
- FSM arguments per behavior type

**Frontend compensation:**

```ts
// src/extensions/datahub/types.ts
export enum StateType {
  Any = 'Any.*',
  Initial = 'Initial',
  Connected = 'Connected',
  Disconnected = 'Disconnected',
  Duplicated = 'Duplicated',
  NotDuplicated = 'NotDuplicated',
  Violated = 'Violated',
  Publishing = 'Publishing',
}
```

### 3.2 DataHub: Missing Validation Strategy

The OpenAPI does not define the validation strategy options:

```ts
// Frontend must define this
export enum StrategyType {
  ALL_OF = 'ALL_OF', // All schemas must validate
  ANY_OF = 'ANY_OF', // Any schema can validate
}
```

### 3.3 DataHub: Missing Pipeline/Graph Structure

The OpenAPI defines individual resources (DataPolicy, PolicyOperation, PolicySchema, Script) but **does not define**:

- How nodes connect (handle definitions)
- Which connections are valid (source → target rules)
- The graph structure for policies

**Frontend compensation:** Entire `designer/` directory (\~40 files) implementing connection logic.

### 3.4 Workspace: Adapter Configuration Schema

Protocol adapters have a `configSchema` property for dynamic configuration, but:

```ts
// Generated type - completely untyped
export type ProtocolAdapter = {
  configSchema?: JsonNode // Record<string, any>
  uiSchema?: JsonNode // Record<string, any>
}
```

The frontend relies on runtime JSON Schema validation via RJSF, with no compile-time guarantees.

---

## 4\. Descriptive Power Issues

### 4.1 ProblemDetails Incomplete

**Generated:**

```ts
export type ProblemDetails = {
  code?: string
  detail?: string
  errors?: Array<Error>
  status?: number // Should be required per RFC 9457
  title: string
  type?: string
}
```

**RFC 9457 requires:**

- `instance` property (missing)
- Extension properties support (not typed)

**Frontend extends:**

```ts
export type ProblemDetailsExtended = ProblemDetails & Record<string, unknown>
```

### 4.2 Missing Field Descriptions

Many generated types have minimal or no JSDoc descriptions:

```ts
// Good example - has descriptions
export type DataPolicy = {
  /**
   * The formatted UTC timestamp indicating when the policy was created.
   */
  readonly createdAt?: string
  // ...
}

// Poor example - no descriptions
export type JsonNode = Record<string, any> // Only description is misleading
```

### 4.3 Missing Examples

The generated types include no example values, reducing documentation utility and making mock data creation harder.

---

## 5\. Feature-Specific Analysis

### 5.1 DataHub Quality Score

| Criterion         | Score (1-5) | Notes                                    |
| :---------------- | :---------- | :--------------------------------------- |
| **Type Coverage** | 2           | Heavy use of `any`, missing enums        |
| **Completeness**  | 2           | FSM, strategies, graph structure missing |
| **Accuracy**      | 3           | What's there matches the API             |
| **Descriptions**  | 3           | Partial, some models well documented     |
| **Relationships** | 1           | No graph/connection semantics            |

**Overall: 2.2/5**

### 5.2 Workspace Quality Score

| Criterion         | Score (1-5) | Notes                                        |
| :---------------- | :---------- | :------------------------------------------- |
| **Type Coverage** | 3           | Adapter config is `any`, but core types work |
| **Completeness**  | 3           | Most CRUD operations covered                 |
| **Accuracy**      | 4           | Types match API well                         |
| **Descriptions**  | 3           | Moderate documentation                       |
| **Relationships** | 2           | No topology/connection info in API           |

**Overall: 3.0/5**

### 5.3 Overall API Quality Score

| Criterion              | Score (1-5) | Notes                                 |
| :--------------------- | :---------- | :------------------------------------ |
| **Type Safety**        | 2           | Too many `any` types                  |
| **Completeness**       | 2.5         | Core CRUD good, domain logic missing  |
| **RFC Compliance**     | 3           | ProblemDetails partial, OpenAPI valid |
| **Frontend Usability** | 2.5         | Requires significant augmentation     |

**Overall: 2.5/5**

---

## 6\. Impact on UX Paradigm Migration

### 6.1 For Resource Listing UX

If the frontend moves to a resource-listing paradigm:

| Resource           | API Readiness | Gaps                                     |
| :----------------- | :------------ | :--------------------------------------- |
| **DataPolicy**     | ✅ CRUD ready | Missing validation strategy enum         |
| **BehaviorPolicy** | ⚠️ Partial    | Missing FSM types, state enums           |
| **Schema**         | ⚠️ Partial    | Type enum missing, protobuf args untyped |
| **Script**         | ✅ CRUD ready | Good coverage                            |
| **Adapters**       | ⚠️ Partial    | Config is untyped JSON                   |
| **Bridges**        | ✅ CRUD ready | Good coverage                            |
| **Combiners**      | ✅ CRUD ready | Good coverage                            |

### 6.2 API Improvements Needed for Clean Resource UX

1. **Add missing enums** to OpenAPI:

- `SchemaType` (JSON, PROTOBUF)
- `StrategyType` (ALL_OF, ANY_OF)
- `BehaviorPolicyType` (Mqtt.events, etc.)
- `StateType` (FSM states)

2. **Type the `JsonNode` usages:**

- Define proper schemas for `configSchema`, `uiSchema`
- Define `arguments` types per function

3. **Add FSM documentation:**

- State machine definitions per behavior type
- Valid transitions and events

4. **Improve ProblemDetails:**

- Add `instance` property
- Make `status` required
- Document extension properties

---

## 7\. Recommendations

### 7.1 Short-term (Before Migration)

1. **Document the gaps:** Create tracking issues for each missing type
2. **Move frontend types to shared package:** Consider a types package that both OpenAPI and frontend can reference
3. **Add validation:** Use Zod or io-ts in frontend to validate API responses

### 7.2 Medium-term (During Migration)

1. **Enhance OpenAPI specs:**

- Add missing enums with proper `x-enum-varnames`
- Replace `JsonNode` with proper schemas where possible
- Add discriminated unions for polymorphic types

2. **Consider OpenAPI extensions:**

- Use `x-*` extensions for frontend-specific metadata
- Document graph/connection semantics

### 7.3 Long-term (API Evolution)

1. **API versioning strategy** for breaking changes
2. **Schema-first development** for new features
3. **Automated type generation tests** to catch regressions

---

## 8\. Appendix: Frontend Type Layers

### Current Architecture

```
┌─────────────────────────────────────────────┐
│ Frontend Application                        │
├─────────────────────────────────────────────┤
│ Custom Types Layer                          │
│ - src/extensions/datahub/types.ts (~350 lines)
│ - src/modules/Workspace/types.ts (~120 lines)
│ - src/api/types/*.ts                        │
├─────────────────────────────────────────────┤
│ React Query Hooks Layer                     │
│ - src/api/hooks/                            │
│ - src/extensions/datahub/api/hooks/         │
├─────────────────────────────────────────────┤
│ Generated API Layer                         │
│ - src/api/__generated__/models/             │
│ - src/api/__generated__/services/           │
│ - src/api/__generated__/schemas/            │
├─────────────────────────────────────────────┤
│ OpenAPI Specification                       │
│ - hivemq-edge-openapi/                      │
└─────────────────────────────────────────────┘
```

### Type Flow for DataPolicy

```
OpenAPI: DataPolicy schema
    ↓
Generated: DataPolicy type (basic)
    ↓
Custom: DataPolicyData (adds handles, dryRunStatus, core reference)
    ↓
Custom: DataPolicyData.Handle enum
    ↓
Designer: Node<DataPolicyData> (React Flow node)
```

---

**End of Analysis**
