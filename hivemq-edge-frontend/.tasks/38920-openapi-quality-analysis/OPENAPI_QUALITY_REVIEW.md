# OpenAPI Specification — Full Review {#openapi-specification-—-full-review}

**File:** `.docs/openapi-bundle.yaml` **Version:** OpenAPI 3.0.1 — HiveMQ Edge REST API 2025.19-SNAPSHOT **Stats:** 8443 lines, \~105 operations across 49 paths, \~120 component schemas, 4 shared parameters

---

## Executive Summary {#executive-summary}

The specification is functional for basic code generation and documentation rendering, but has deep structural and descriptive deficiencies that make it unsuitable as a reliable backbone for agentic conversation or domain ontology extraction without significant supplementary context.

The Data Hub subsystem initially appears better-specified (proper error hierarchies, pagination, some required fields), but a comparison with the [official HiveMQ Data Hub documentation](https://docs.hivemq.com/hivemq/latest/data-hub/index.html) reveals that the spec hides most of the domain's actual structure behind opaque `type: object` fields. Functions, their arguments, validation strategies, behavior models, FSM states/transitions, interpolation variables, and transformation capabilities are all well-defined in the product but rendered as typeless maps in the spec. The Data Hub spec gives the _scaffolding_ of a good API but not the _substance_.

The Edge-native management subsystem (bridges, adapters, events, combiners, UNS, Pulse) is markedly weaker across every quality axis. The Pulse asset-mapper endpoints are an unacknowledged duplicate of the Combiners API surface.

**Key risk for agentic use:** An LLM relying solely on this spec will:

- Be unable to construct valid policy pipelines (function IDs, arguments, and constraints are all opaque)
- Hallucinate relationships between resources (e.g., which adapter owns which tags)
- Misinterpret opaque `JsonNode` fields (used in 7+ distinct semantic contexts)
- Fail to distinguish authenticated from public endpoints
- Be unable to reason about behavior model states, transitions, or events

The spec must be augmented with a domain ontology and supplementary metadata before it can serve as reliable agentic context.

---

- [OpenAPI Specification — Full Review](#openapi-specification-—-full-review)
- [Executive Summary](#executive-summary)
- [Part I — Structural & Technical Quality](#part-i-—-structural-&-technical-quality)
  - [1\. Security Declarations — MISSING ENTIRELY](#1.-security-declarations-—-missing-entirely)
  - [2\. Copy-Paste Errors in Descriptions](#2.-copy-paste-errors-in-descriptions)
  - [3\. Missing Descriptions and Titles](#3.-missing-descriptions-and-titles)
    - [3.1 Schemas with no description at all](#3.1-schemas-with-no-description-at-all)
    - [3.2 Misleading "List of result items" description](#3.2-misleading-"list-of-result-items"-description)
    - [3.3 Endpoints with missing or empty summary/description](#3.3-endpoints-with-missing-or-empty-summary/description)
    - [3.4 Parameters with missing or vacuous descriptions](#3.4-parameters-with-missing-or-vacuous-descriptions)
  - [4\. Missing required Fields on Core Schemas](#4.-missing-required-fields-on-core-schemas)
  - [5\. Inconsistent operationId Naming](#5.-inconsistent-operationid-naming)
  - [6\. Inconsistent HTTP Status Codes](#6.-inconsistent-http-status-codes)
  - [7\. Undefined Tags](#7.-undefined-tags)
  - [8\. Grammar, Typos, and en-US Style Issues](#8.-grammar,-typos,-and-en-us-style-issues)
    - [8.1 Grammar and typos](#8.1-grammar-and-typos)
    - [8.2 en-US style issues](#8.2-en-us-style-issues)
    - [8.3 Inconsistent capitalization in descriptions](#8.3-inconsistent-capitalization-in-descriptions)
  - [9\. Security Concerns in Schemas](#9.-security-concerns-in-schemas)
  - [10\. Structural Oddities](#10.-structural-oddities)
- [Part I-B — Data Hub: Spec vs. Reality](#part-i-b-—-data-hub:-spec-vs.-reality)
  - [11\. PolicyOperation.arguments — Opaque Map Hiding 9 Typed Functions](#11.-policyoperation.arguments-—-opaque-map-hiding-9-typed-functions)
  - [12\. DataPolicyValidator.arguments — Opaque Map Hiding Strategy \+ Schemas Structure](#12.-datapolicyvalidator.arguments-—-opaque-map-hiding-strategy-+-schemas-structure)
  - [13\. BehaviorPolicyBehavior.arguments — Opaque Map Hiding Per-Model Parameters](#13.-behaviorpolicybehavior.arguments-—-opaque-map-hiding-per-model-parameters)
  - [14\. FSM States and Transitions — Undocumented in Spec](#14.-fsm-states-and-transitions-—-undocumented-in-spec)
  - [15\. PolicySchema.type — Missing Enum](#15.-policyschema.type-—-missing-enum)
  - [16\. String Interpolation — Undocumented Contract](#16.-string-interpolation-—-undocumented-contract)
  - [17\. Transformation Scripts — Entire Subsystem Under-Specified](#17.-transformation-scripts-—-entire-subsystem-under-specified)
  - [18\. System Limits — Undocumented Constraints](#18.-system-limits-—-undocumented-constraints)
  - [19\. Revised Data Hub Assessment](#19.-revised-data-hub-assessment)
- [Part II — Agentic & Ontological Readiness](#part-ii-—-agentic-&-ontological-readiness)
  - [20\. Cross-Resource Relationship Modeling — ABSENT](#20.-cross-resource-relationship-modeling-—-absent)
  - [21\. Opaque JsonNode Usage — 7 Distinct Semantic Roles](#21.-opaque-jsonnode-usage-—-7-distinct-semantic-roles)
  - [22\. Enum Value Discoverability](#22.-enum-value-discoverability)
  - [23\. Pagination Inconsistency](#23.-pagination-inconsistency)
  - [24\. Content-Type Handling](#24.-content-type-handling)
  - [25\. Example Quality](#25.-example-quality)
    - [Good examples (Data Hub)](<#good-examples-(data-hub)>)
    - [Poor examples (Edge management)](<#poor-examples-(edge-management)>)
  - [26\. Versioning Strategy](#26.-versioning-strategy)
- [Part III — User-Facing Strings Review (en-US Standards)](<#part-iii-—-user-facing-strings-review-(en-us-standards)>)
  - [27\. Tag Names — Inconsistent Taxonomy](#27.-tag-names-—-inconsistent-taxonomy)
  - [28\. Summary/Description Conventions](#28.-summary/description-conventions)
  - [29\. Schema Property Description Quality](#29.-schema-property-description-quality)
- [Part IV — Summary Scorecard](#part-iv-—-summary-scorecard)
- [Part V — Recommendations for Agentic Use](#part-v-—-recommendations-for-agentic-use)
  - [Priority 1 — Create a supplementary domain ontology (work around spec)](<#priority-1-—-create-a-supplementary-domain-ontology-(work-around-spec)>)
  - [Priority 2 — Supplement descriptions for agentic context](#priority-2-—-supplement-descriptions-for-agentic-context)
  - [Priority 3 — Address critical spec defects upstream](#priority-3-—-address-critical-spec-defects-upstream)

---

## Part I — Structural & Technical Quality {#part-i-—-structural-&-technical-quality}

### 1\. Security Declarations — MISSING ENTIRELY {#1.-security-declarations-—-missing-entirely}

**Severity: Critical**

The API implements JWT-based authentication (Bearer token), yet the spec contains:

- No `securitySchemes` under `components`
- No global `security` declaration
- No per-operation `security` annotations

This means:

- Generated SDKs cannot distinguish public from authenticated endpoints
- API documentation tools (Swagger UI, Redoc) show no auth requirements
- An agentic system cannot reason about what it can access without a token
- Clients must rely on runtime 401 responses to discover auth requirements

**Public endpoints** (determined empirically): `/api/v1/auth/*`, `/api/v1/health/*`, `/api/v1/frontend/*`, `GET /` **Authenticated endpoints:** Everything else

---

### 2\. Copy-Paste Errors in Descriptions {#2.-copy-paste-errors-in-descriptions}

**Severity: High** — misleading documentation, dangerous for agentic context

| Endpoint                                                         | Issue                                                                                                                                |
| :--------------------------------------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------- |
| `POST /api/v1/auth/refresh-token`                                | Description says "Authorize the presented user to obtain a secure token" — copied from `authenticate`, should describe token refresh |
| `POST /api/v1/auth/validate-token`                               | Both summary AND description copied from `authenticate` — should describe token validation                                           |
| `GET /api/v1/management/events`                                  | Description says "Get all bridges configured in the system" — should say events                                                      |
| `GET /api/v1/management/events` (`limit` param)                  | Description says "Obtain all events since the specified epoch" — copied from the `since` param                                       |
| `PUT .../behavior-validation/policies/{policyId}` (400 response) | Says "Behavior policy creation failed" — should say "update failed"                                                                  |
| `PUT .../data-validation/policies/{policyId}` (400 response)     | Says "Data policy creation failed" — should say "update failed"                                                                      |
| `GET .../adapters/{adapterId}/status` (example)                  | Example shows `type: bridge` — should show adapter                                                                                   |
| `GET .../writing-schema/{adapterId}/{tagName}` (example)         | Example shows domain tag data, not a writing schema                                                                                  |
| `POST .../pulse/asset-mappers`                                   | Request body description says "The combiner to add"                                                                                  |
| `BehaviorPolicyNotFoundError.id`                                 | Description says "The data policy id" — should say behavior policy id                                                                |
| `SchemaInsufficientStorageError.id`                              | Description says "The policy id" — should say schema id                                                                              |
| `ScriptInsufficientStorageError.id`                              | Description says "The policy id" — should say script id                                                                              |
| `get-adapters-status` (example)                                  | Shows `type: bridge` — should show `type: adapter`                                                                                   |
| `get-bridges-status` description                                 | Says "Obtain the details." — vacuous, should describe bridge status collection                                                       |
| `get-adapters-status` description                                | Same: "Obtain the details."                                                                                                          |
| `getBehaviorPolicy` summary                                      | "Get a policy" — double space                                                                                                        |
| `Listener.description`                                           | Says "The extension description" — copied from Extension schema                                                                      |
| `Listener.hostName`                                              | Says "A mandatory ID hostName with the Listener" — garbled                                                                           |

---

### 3\. Missing Descriptions and Titles {#3.-missing-descriptions-and-titles}

**Severity: Medium–High** (for agentic use: High — an LLM cannot reason about undescribed entities)

#### 3.1 Schemas with no description at all {#3.1-schemas-with-no-description-at-all}

Core entities: `UsernamePasswordCredentials`, `ApiBearerToken`, `Bridge`, `Adapter`, `HealthStatus`, `GatewayConfiguration`, `StatusTransitionCommand`, `StatusTransitionResult`, `ISA95ApiBean`, `AdapterConfig`, `TagSchema`, `DataPoint`, `PolicySchema`, `Script`, `PulseActivationToken`.

List wrappers (15): `BridgeList`, `EventList`, `StatusList`, `AdaptersList`, `NotificationList`, `ListenerList`, `CapabilityList`, `MetricList`, `TopicFilterList`, `PayloadSampleList`, `ProtocolAdaptersList`, `ValuesTree`, `EntityReferenceList`, `DataCombiningList`, `FsmStatesInformationListItem`.

#### 3.2 Misleading "List of result items" description {#3.2-misleading-"list-of-result-items"-description}

15 schemas reuse `"List of result items that are returned by this endpoint"` as their own schema-level description. This is a copy-paste artifact from the `items` array description and does not describe the entity itself. Affected: `Capability`, `Extension`, `Module`, `Notification`, `Listener`, `Event`, `NorthboundMapping`, `SouthboundMapping`, `DomainTag`, `ObjectNode`, `ProtocolAdapter`, `PayloadSample`, `TopicFilter`, `Metric`, `FsmStateInformationItem`.

#### 3.3 Endpoints with missing or empty summary/description {#3.3-endpoints-with-missing-or-empty-summary/description}

- `GET /` — no summary, no description, no tags, response is `*/*` with empty schema

#### 3.4 Parameters with missing or vacuous descriptions {#3.4-parameters-with-missing-or-vacuous-descriptions}

- `X-Original-URI` header on `getAdapterTypes` — no description at all
- `TopicFilterId` parameter — description says "should be deleted" regardless of whether the operation is GET, PUT, or DELETE

---

### 4\. Missing `required` Fields on Core Schemas {#4.-missing-required-fields-on-core-schemas}

**Severity: Medium** — affects form validation, SDK type safety, and LLM reasoning about mandatory data

| Schema                        | Fields that should likely be required      |
| :---------------------------- | :----------------------------------------- |
| `UsernamePasswordCredentials` | `userName`, `password`                     |
| `ApiBearerToken`              | `token`                                    |
| `StatusTransitionCommand`     | `command`                                  |
| `Status`                      | `connection`, `runtime` (or at least `id`) |
| `Notification`                | `title`, `level`                           |
| `Listener`                    | `hostName`, `port`, `name`                 |
| `HealthStatus`                | `status`                                   |
| `Capability`                  | `id`                                       |
| `ProtocolAdapter`             | `id`, `name`                               |
| `Adapter`                     | `type` (only `id` is required)             |
| `Metric`                      | `name`                                     |
| `DataPoint`                   | `value`                                    |
| `ObjectNode`                  | `name`, `nodeType`                         |
| `FsmStateInformationItem`     | `stateName`, `policyId`                    |
| `ISA95ApiBean`                | `enabled`                                  |
| `TagSchema`                   | `protocolId`, `configSchema`               |

---

### 5\. Inconsistent operationId Naming {#5.-inconsistent-operationid-naming}

**Severity: Medium** — affects generated SDK method names and agentic tool-call disambiguation

Three conventions are mixed:

| Convention | Examples                                                                                | Count |
| :--------- | :-------------------------------------------------------------------------------------- | :---- |
| camelCase  | `getAllBehaviorPolicies`, `getBridges`, `addBridge`, `getAdapters`                      | \~40  |
| kebab-case | `refresh-token`, `get-capabilities`, `get-bridges-status`, `get-combiners`              | \~50  |
| Mixed      | `getCombinersById` (plural for single), `getBridgeByName` (says "Name", path says "Id") | \~5   |

The Data Hub endpoints consistently use camelCase. Edge-native management endpoints predominantly use kebab-case. Frontend/Gateway endpoints use kebab-case.

---

### 6\. Inconsistent HTTP Status Codes {#6.-inconsistent-http-status-codes}

**Severity: Medium**

| Pattern         | Data Hub endpoints               | Edge management endpoints |
| :-------------- | :------------------------------- | :------------------------ |
| Create (POST)   | `201 Created` with response body | `200 OK` with empty body  |
| Delete (DELETE) | `204 No Content`                 | `200 OK` with empty body  |
| Update (PUT)    | `200 OK` with response body      | `200 OK` with empty body  |

Edge management endpoints (bridges, adapters, topic filters, combiners, pulse, UNS) never return the created/updated resource, making it impossible for clients to confirm the result without a follow-up GET.

Additionally:

- `add-topicFilters` returns `403 Forbidden` for "Already Present" — should be `409 Conflict`
- `delete-topicFilter` returns `403 Forbidden` for "Already Present" — semantically wrong; should be `404` or none
- `update-adapter-domainTag` returns `403` for "Adapter not found" — should be `404`

---

### 7\. Undefined Tags {#7.-undefined-tags}

**Severity: Low**

Four tags are used on endpoints but never defined in the top-level `tags:` section:

- `Authentication` — used alongside the defined `Authentication Endpoint`
- `Health Check Endpoint` — used on health endpoints, not defined at all
- `Metrics` — used alongside the defined `Metrics Endpoint`
- `Combiners` — used on combiner endpoints, not defined at all

---

### 8\. Grammar, Typos, and en-US Style Issues {#8.-grammar,-typos,-and-en-us-style-issues}

**Severity: Low–Medium** (for user-facing strings: Medium — these surface in documentation, UIs, and agentic context)

#### 8.1 Grammar and typos {#8.1-grammar-and-typos}

| Location                                     | Issue                                                                                  |
| :------------------------------------------- | :------------------------------------------------------------------------------------- |
| `delete-adapter-domainTags` summary          | "an domain" → "a domain"                                                               |
| `delete-topicFilter` summary                 | "an topic" → "a topic"                                                                 |
| `getTagSchema` description                   | "portocol" → "protocol"                                                                |
| `getSchemaForTopic` summary                  | "based in" → "based on"                                                                |
| `getSamplesForTopic` summary                 | "their gathered" → "are gathered"                                                      |
| `getBehaviorPolicy` summary                  | "Get a policy" (double space)                                                          |
| `get-listeners` summary                      | Trailing space: "configured "                                                          |
| `getAdapter` description                     | Unmatched trailing quote                                                               |
| `get-adapter-status` description             | "Get the up to date status an adapter." → "of an adapter"                              |
| `list-response-b` (scripts)                  | "sripts" → "scripts"                                                                   |
| `NorthboundMapping.tagName`                  | "hould" → "should"                                                                     |
| `SouthboundMapping.tagName`                  | "hould" → "should"                                                                     |
| `RequestBodyParameterMissingError.parameter` | "The the missing" → "The missing"                                                      |
| `Bridge.cleanStart`                          | "associated the the" → "associated with the" (appears multiple times on Bridge fields) |
| `Bridge.clientId`                            | "associated the the" → "associated with the"                                           |
| `Bridge.keepAlive`                           | "associated the the" → "associated with the"                                           |
| `Bridge.sessionExpiry`                       | "associated the the" → "associated with the"                                           |
| `Bridge.password`                            | "associated the the" → "associated with the"                                           |
| `Bridge.username`                            | "associated the the" → "associated with the"                                           |

#### 8.2 en-US style issues {#8.2-en-us-style-issues}

| Location                         | Issue                                                                                                                                                                                                        |
| :------------------------------- | :----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `BridgeCustomUserProperty.key`   | "The key the from the property" — garbled                                                                                                                                                                    |
| `BridgeCustomUserProperty.value` | "The value the from the property" — garbled                                                                                                                                                                  |
| `Listener.hostName`              | "A mandatory ID hostName with the Listener" — garbled                                                                                                                                                        |
| `TopicFilter.description`        | Says "The name for this topic filter" — should describe the description field                                                                                                                                |
| `TopicFilterId` parameter        | Description says "should be deleted" but the parameter is shared across GET, PUT, and DELETE                                                                                                                 |
| All TLS fields                   | Descriptions follow pattern "The X from the config" — should describe what the field does, not where it comes from (e.g., "The password for the keystore" instead of "The keystorePassword from the config") |
| ISA95 fields                     | Descriptions are single words: "The area", "The site", "The enterprise" — should explain the ISA-95 hierarchy level                                                                                          |

#### 8.3 Inconsistent capitalization in descriptions {#8.3-inconsistent-capitalization-in-descriptions}

- Some field descriptions start with "A mandatory..." or "The ..." (sentence case)
- Others start with lowercase "The ..."
- Summary fields inconsistently use title case vs. sentence case
- Tag names mix styles: "Authentication Endpoint" vs "Bridges" vs "Data Hub \- Behavior Policies"

---

### 9\. Security Concerns in Schemas {#9.-security-concerns-in-schemas}

**Severity: Medium**

Sensitive fields not marked as `writeOnly: true`:

- `TlsConfiguration.keystorePassword`
- `TlsConfiguration.privateKeyPassword`
- `TlsConfiguration.truststorePassword`
- `Bridge.password`
- `FirstUseInformation.prefillPassword`

Additionally, the `PulseActivationToken.token` (format: `jwt`) should be `writeOnly: true`.

The example in `getBridgeByName` shows `password: password` in clear text — a bad practice even in examples.

---

### 10\. Structural Oddities {#10.-structural-oddities}

| Issue                                                 | Detail                                                                                                                                                                                                                                          |
| :---------------------------------------------------- | :---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| **Pulse asset-mappers duplicate Combiners**           | `/api/v1/management/pulse/asset-mappers/*` endpoints mirror `/api/v1/management/combiners/*` exactly, reusing `Combiner`, `CombinerList`, `DataCombiningList` schemas. Path params still use `combinerId` in the asset-mappers context.         |
| **`JsonNode` is a catch-all**                         | Used for adapter configs, FSM definitions, tag definitions, writing schemas, protocol adapter config/ui schemas, and function specs. Its description ("The arguments of the fsm derived from the behavior policy") is specific to one use case. |
| **`GET /api/v1/data-hub/functions` deprecated**       | Marked `deprecated: true` with replacement `/api/v1/data-hub/function-specs`, but no sunset timeline documented.                                                                                                                                |
| **Topic filter path param uses `$ref`**               | The `{filter}` path parameter on topic-filter endpoints uses `$ref` to a shared parameter definition — unusual but valid. However, the shared parameter description is wrong (says "should be deleted").                                        |
| **`createSchema` has `If-Match` header**              | Unusual for a POST/create operation; typically used for conditional updates.                                                                                                                                                                    |
| **`set-isa95` uses POST**                             | Semantically this is a PUT (idempotent set/replace), not a POST.                                                                                                                                                                                |
| **Naming: `AdaptersList` vs `BridgeList`**            | Inconsistent pluralization — some list schemas use `*sList`, others `*List`.                                                                                                                                                                    |
| **`getMappingInstructions` returns bare array**       | Returns `type: array` directly instead of wrapping in `{ items: [...] }` like every other list endpoint. Same for `get-asset-mapper-instructions`.                                                                                              |
| **`NorthboundMapping.messageExpiryInterval` default** | Default value `9007199254740991L` — the `L` suffix is a Java literal, not valid JSON.                                                                                                                                                           |

---

## Part I-B — Data Hub: Spec vs. Reality {#part-i-b-—-data-hub:-spec-vs.-reality}

_This section compares the OpenAPI spec against the [official HiveMQ Data Hub documentation](https://docs.hivemq.com/hivemq/latest/data-hub/index.html). The Data Hub is the best-specified subsystem in the OpenAPI, yet cross-referencing with the product documentation reveals that most of the domain's actual type structure is hidden behind opaque `type: object` fields._

### 11\. PolicyOperation.arguments — Opaque Map Hiding 9 Typed Functions {#11.-policyoperation.arguments-—-opaque-map-hiding-9-typed-functions}

**Severity: Critical for agentic use**

The `PolicyOperation` schema defines the pipeline steps that execute when a policy triggers. In the spec:

```
PolicyOperation:
  properties:
    arguments:
      type: object                    # ← completely opaque
      description: The required arguments of the referenced function.
    functionId:
      type: string                    # ← no enum constraint
    id:
      type: string
```

In reality, `functionId` is one of exactly **9 well-defined functions**, each with specific typed arguments:

| functionId                  | Arguments                                                                                   | Types            | Availability     | Terminal |
| :-------------------------- | :------------------------------------------------------------------------------------------ | :--------------- | :--------------- | :------- |
| `System.log`                | `level` (enum: DEBUG, ERROR, WARN, INFO, TRACE), `message` (string, supports interpolation) | Both required    | Data \+ Behavior | No       |
| `Metrics.Counter.increment` | `metricName` (string, no interpolation), `incrementBy` (number, supports negative)          | Both required    | Data \+ Behavior | No       |
| `Mqtt.UserProperties.add`   | `name` (string), `value` (string)                                                           | Both required    | Data \+ Behavior | No       |
| `Serdes.deserialize`        | `schemaId` (string), `schemaVersion` (string)                                               | Both required    | Data only        | No       |
| `Serdes.serialize`          | `schemaId` (string), `schemaVersion` (string)                                               | Both required    | Data only        | No       |
| `Delivery.redirectTo`       | `topic` (string), `applyPolicies` (boolean, max 20 policy eval depth)                       | `topic` required | Data only        | Yes      |
| `Mqtt.drop`                 | `reasonString` (string, optional, MQTT5 only)                                               | None required    | Data \+ Behavior | Yes\*    |
| `Mqtt.disconnect`           | _(none)_                                                                                    | —                | Data \+ Behavior | Yes      |
| `fn:com.hivemq.modules.*`   | Custom module functions                                                                     | Dynamic          | Data \+ Behavior | Varies   |

\* `Mqtt.drop` is restricted to `Mqtt.OnInboundPublish` and `Mqtt.OnInboundSubscribe` events in behavior policies.

**What the spec hides:**

- The closed set of valid `functionId` values (no enum)
- The per-function argument schemas (all collapsed into `type: object`)
- The terminal/non-terminal distinction (affects pipeline ordering)
- The data-policy-only vs. universal availability constraint
- The event-level restrictions on certain functions
- Pipeline ordering rules (e.g., `Serdes.deserialize` must precede `Serdes.serialize`)

An LLM given only the spec would have to guess valid function names and argument structures.

---

### 12\. DataPolicyValidator.arguments — Opaque Map Hiding Strategy \+ Schemas Structure {#12.-datapolicyvalidator.arguments-—-opaque-map-hiding-strategy-+-schemas-structure}

**Severity: High**

The `DataPolicyValidator` schema:

```
DataPolicyValidator:
  properties:
    arguments:
      type: object                    # ← completely opaque
      description: The required arguments of the referenced validator type.
    type:
      type: string
      enum: [SCHEMA]                  # ← at least this is constrained
```

In reality, the `arguments` object for `type: SCHEMA` has a well-defined structure:

```json
{
  "strategy": "ALL_OF", // enum: ALL_OF, ANY_OF
  "schemas": [
    {
      "schemaId": "my-schema", // references a PolicySchema.id
      "version": "latest" // specific version number or "latest"
    }
  ]
}
```

The `strategy` enum (`ALL_OF` \= all must pass, `ANY_OF` \= at least one must pass) and the `schemas` array of `SchemaReference` objects are fully defined in the product. The `SchemaReference` type already exists in the spec (used in `BehaviorPolicyDeserializer`) but is not referenced here — the same structure is typed in one place and opaque in another.

---

### 13\. BehaviorPolicyBehavior.arguments — Opaque Map Hiding Per-Model Parameters {#13.-behaviorpolicybehavior.arguments-—-opaque-map-hiding-per-model-parameters}

**Severity: High**

```
BehaviorPolicyBehavior:
  properties:
    arguments:
      type: object                    # ← completely opaque
    id:
      type: string                    # ← no enum
```

In reality, `id` is one of exactly **3 predefined behavior models**, each with specific arguments:

| Model ID            | Arguments                                                                        | Constraints                       |
| :------------------ | :------------------------------------------------------------------------------- | :-------------------------------- |
| `Mqtt.events`       | _(none)_                                                                         | No arguments required or accepted |
| `Publish.duplicate` | _(none)_                                                                         | No arguments required or accepted |
| `Publish.quota`     | `minPublishes` (integer, default 0), `maxPublishes` (integer, default UNLIMITED) | At least one must be present      |

**What the spec hides:**

- The closed set of valid behavior model IDs (no enum)
- The per-model argument schemas
- The full FSM state machines for each model (states, transitions, events)

Note: The `getFsms` endpoint returns this information at runtime as a JSON Schema (via `JsonNode`), but this means the structure is only discoverable dynamically, not from the spec itself.

---

### 14\. FSM States and Transitions — Undocumented in Spec {#14.-fsm-states-and-transitions-—-undocumented-in-spec}

**Severity: Medium–High**

Each behavior model defines a complete finite state machine. These are critical for constructing `onTransitions` rules but are entirely absent from the spec:

**Mqtt.events model:**

- States: `Initial` → `Connected` → `Disconnected`
- `Connected` loops on all MQTT events
- Events: `Mqtt.OnInboundConnect`, `Mqtt.OnInboundPublish`, `Mqtt.OnInboundSubscribe`, `Mqtt.OnInboundDisconnect`, `Connection.OnDisconnect`

**Publish.duplicate model:**

- States: `Initial` → `Connected` → `NotDuplicated` ↔ `Duplicated` → `Violated` | `Disconnected`
- `Violated` \= terminal failure (duplicate detected); `Disconnected` \= terminal success

**Publish.quota model:**

- States: `Initial` → `Connected` → `Publishing` → `Violated` | `Disconnected`
- `Violated` on quota exceeded or insufficient publishes at disconnect
- Arguments: `minPublishes`, `maxPublishes`

The `BehaviorPolicyOnTransition` schema has `fromState` and `toState` as free-form strings with no indication of valid values. The wildcard patterns (`Any.*`, `Any.Success`, `Any.Failed`) are also undocumented in the spec.

---

### 15\. PolicySchema.type — Missing Enum {#15.-policyschema.type-—-missing-enum}

**Severity: Medium**

```
PolicySchema:
  properties:
    type:
      type: string
      description: The type of the schema.     # ← no enum constraint
```

The valid values are exactly `JSON` and `PROTOBUF`, with specific sub-constraints:

- JSON: supports drafts 04, 06, 07, 2019-09, 2020-12
- Protobuf: supports proto2 and proto3
- Both: 5,000 max schemas, 100KB max definition size

None of these constraints appear in the spec.

---

### 16\. String Interpolation — Undocumented Contract {#16.-string-interpolation-—-undocumented-contract}

**Severity: Medium**

Several function arguments (notably `System.log.message`, `Delivery.redirectTo.topic`) support `${variable}` interpolation. The available variables are:

| Variable              | Type   | Available in     |
| :-------------------- | :----- | :--------------- |
| `${clientId}`         | string | Data \+ Behavior |
| `${topic}`            | string | Data only        |
| `${policyId}`         | string | Data \+ Behavior |
| `${validationResult}` | string | Data only        |
| `${fromState}`        | string | Behavior only    |
| `${toState}`          | string | Behavior only    |
| `${triggerEvent}`     | string | Behavior only    |
| `${timestamp}`        | long   | Data \+ Behavior |

The `InterpolationVariable` schema and `getVariables` endpoint exist in the spec and do model this correctly — this is one area where the runtime API compensates for what's missing from the static types. However, there is no indication in the `PolicyOperation.arguments` description that interpolation is supported, nor which argument fields accept it.

---

### 17\. Transformation Scripts — Entire Subsystem Under-Specified {#17.-transformation-scripts-—-entire-subsystem-under-specified}

**Severity: Medium**

The `Script` schema captures `id`, `source` (base64), `functionType` (enum: `TRANSFORMATION`), and `version`. But the entire transformation API — the contract that scripts must implement — is absent from the spec:

- Required `function transform(publish, context)` signature
- Optional `function init(initContext)` signature
- The `publish` object structure (`topic`, `qos`, `retain`, `userProperties`, `payload`)
- The `context` object structure (`arguments`, `policyId`, `clientId`, `branches`, `clientConnectionStates`)
- Branch system for multi-message fan-out (`initContext.addBranch()`, `context.branches[name].addPublish()`)
- Client connection state persistence (`initContext.addClientConnectionState()`)
- Runtime constraints: synchronous only, ECMAScript 2024, no browser/Node APIs, 100KB source limit, 5000 script limit

This is understandable for an OpenAPI spec (it describes the REST API, not the script runtime), but for agentic use the transformation API is a critical part of the domain model.

---

### 18\. System Limits — Undocumented Constraints {#18.-system-limits-—-undocumented-constraints}

**Severity: Low–Medium**

The product enforces hard limits that are absent from the spec:

| Resource                       | Limit           | In spec?                        |
| :----------------------------- | :-------------- | :------------------------------ |
| Data policies                  | 5,000 max       | No                              |
| Behavior policies              | 5,000 max       | No                              |
| Schemas                        | 5,000 max       | No                              |
| Scripts                        | 5,000 max       | No                              |
| Schema definition size         | 100KB           | No                              |
| Script source size             | 100KB           | No                              |
| Event message length           | 1,024 chars     | Yes (Event.message description) |
| Pagination page size           | 10–500          | Yes (query param description)   |
| `Delivery.redirectTo` depth    | 20 policies max | No                              |
| Client connection state value  | 10KB max        | No                              |
| Client connection state global | 50MB max        | No                              |

---

### 19\. Revised Data Hub Assessment {#19.-revised-data-hub-assessment}

The initial review rated "Schema quality — Data Hub" as **Good**. After cross-referencing with the official documentation, this must be revised:

| Aspect                    | Original Rating | Revised Rating | Reason                                                                                  |
| :------------------------ | :-------------- | :------------- | :-------------------------------------------------------------------------------------- |
| Error hierarchy           | Good            | Good           | Discriminated unions with typed sub-errors — genuinely well done                        |
| Pagination                | Good            | Good           | Cursor-based with consistent schema                                                     |
| Required fields           | Good            | Good           | Core entities have `required`                                                           |
| Descriptions (operations) | Good            | Fair           | Descriptions exist but are generic; don't explain the domain                            |
| Descriptions (schemas)    | Good            | Poor           | `arguments` fields on the 3 most important schemas are `type: object` with no structure |
| Type safety               | Good            | **Poor**       | 3 critical schemas use opaque objects where well-defined types exist                    |
| Domain modeling           | Good            | **Poor**       | Functions, models, states, strategies all hidden behind free-form fields                |
| Agentic usability         | Good            | **Poor**       | An LLM cannot construct a valid policy from this spec alone                             |

**Bottom line:** The Data Hub spec is structurally sound (good HTTP conventions, errors, pagination) but semantically hollow. The actual domain knowledge — what functions exist, what arguments they take, what behavior models do, how validation strategies work — is trapped in product documentation rather than expressed in the type system.

---

## Part II — Agentic & Ontological Readiness {#part-ii-—-agentic-&-ontological-readiness}

### 20\. Cross-Resource Relationship Modeling — ABSENT {#20.-cross-resource-relationship-modeling-—-absent}

**Severity: Critical for agentic use**

The spec does not model cross-resource relationships. An LLM or ontology extractor has no way to discover:

| Relationship                           | How it should be expressed           | What the spec provides                                                                                                                                               |
| :------------------------------------- | :----------------------------------- | :------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Adapter → Tags                         | An adapter owns multiple domain tags | Only discoverable via the `GET .../adapters/{adapterId}/tags` endpoint URL pattern                                                                                   |
| Adapter → Northbound Mappings          | An adapter owns northbound mappings  | Same — only from URL pattern                                                                                                                                         |
| Adapter → Southbound Mappings          | An adapter owns southbound mappings  | Same                                                                                                                                                                 |
| Tag → Adapter (reverse)                | Which adapter owns a tag             | Only via `DomainTagOwner` schema (extends `DomainTag` with `adapterId`) — but this is only returned from the global `/tags` endpoint, not from per-adapter endpoints |
| Bridge → Status                        | A bridge has runtime status          | Embedded as `status` property on `Bridge`, but also a separate endpoint                                                                                              |
| Combiner → Mappings → Instructions     | Three-level nesting                  | Only from URL path hierarchy                                                                                                                                         |
| DataPolicy → Schema                    | Policies reference schemas           | Via opaque `schemaId` string in nested objects — not a `$ref` or any formal link                                                                                     |
| Managed Asset → Data Combining Mapping | Asset maps to a combiner mapping     | Via `mappingId` UUID — but no formal link or description of how to resolve it                                                                                        |

**Recommendation:** Add `x-relationships` or equivalent extensions, or at minimum, enrich descriptions to explicitly state resource ownership and references.

---

### 21\. Opaque `JsonNode` Usage — 7 Distinct Semantic Roles {#21.-opaque-jsonnode-usage-—-7-distinct-semantic-roles}

**Severity: High for agentic use**

The `JsonNode` schema is `type: object` with a single misleading description. It is used in 7 semantically distinct contexts:

| Context                          | What it actually contains                         | Schema description            |
| :------------------------------- | :------------------------------------------------ | :---------------------------- |
| `Adapter.config`                 | Protocol-adapter-specific configuration JSON      | "The arguments of the fsm..." |
| `ProtocolAdapter.configSchema`   | JSON Schema defining the adapter config structure | Same                          |
| `ProtocolAdapter.uiSchema`       | RJSF UI schema for rendering adapter config forms | Same                          |
| `FunctionSpecs.schema`           | JSON Schema for a DataHub function                | Same                          |
| `FunctionSpecs.uiSchema`         | RJSF UI schema for function config                | Same                          |
| `DomainTag.definition`           | Protocol-specific tag address definition          | Same                          |
| `TagSchema.configSchema`         | JSON Schema for tag definition                    | Same                          |
| Response of `getFsms`            | JSON Schema of available FSM models               | Same                          |
| Response of `get-writing-schema` | JSON Schema for PLC write payloads                | Same                          |

An LLM cannot distinguish these without external context. Each should have its own wrapper type with a specific description, or at minimum `JsonNode` usages should have per-property descriptions.

---

### 22\. Enum Value Discoverability {#22.-enum-value-discoverability}

**Severity: Medium for agentic use**

Some important enums are well-defined and discoverable:

- `Status.connection`: `CONNECTED`, `DISCONNECTED`, `STATELESS`, `UNKNOWN`, `ERROR`
- `Status.runtime`: `STARTED`, `STOPPED`
- `StatusTransitionCommand.command`: `START`, `STOP`, `RESTART`
- `Notification.level`: `NOTICE`, `WARNING`, `ERROR`
- `Event.severity`: `INFO`, `WARN`, `ERROR`, `CRITICAL`
- `Capability.id`: 6 well-named enum values
- `EntityType`: `ADAPTER`, `DEVICE`, `BRIDGE`, `EDGE_BROKER`, `PULSE_AGENT`

Others are missing or opaque:

- **Adapter types**: No enum — dynamically discovered via `getAdapterTypes`. The LLM cannot enumerate possible adapter types from the spec alone.
- **Schema types** (`PolicySchema.type`): No enum — from examples it's `JSON` or `PROTOBUF`, but not constrained in the schema.
- **ISA-95 levels**: No enum or constraints beyond regex patterns.

---

### 23\. Pagination Inconsistency {#23.-pagination-inconsistency}

**Severity: Medium**

| Pattern                         | Used by                               | Mechanism                                                |
| :------------------------------ | :------------------------------------ | :------------------------------------------------------- |
| Cursor-based with `_links.next` | Data Hub (policies, schemas, scripts) | `PaginationCursor` schema, `limit`/`cursor` query params |
| `limit`/`since` offset-based    | Events                                | `limit` (int, default 100\) \+ `since` (epoch)           |
| No pagination at all            | All other Edge management endpoints   | Returns all items                                        |

An agentic system must know which endpoints support pagination to avoid requesting excessive data. This is not machine-discoverable from the spec.

---

### 24\. Content-Type Handling {#24.-content-type-handling}

**Severity: Low–Medium**

- Data Hub endpoints correctly use `application/problem+json` for error responses
- Edge management endpoints use `application/json` for error responses (inconsistent)
- `get-xml-configuration` returns `application/xml` — the only XML endpoint
- `GET /` returns `*/*` with no schema — completely opaque

---

### 25\. Example Quality {#25.-example-quality}

**Severity: Medium**

#### Good examples (Data Hub) {#good-examples-(data-hub)}

- Every Data Hub endpoint has multiple named examples with realistic data
- List endpoints show single result, multiple results, and paginated results
- Error schemas include example error bodies

#### Poor examples (Edge management) {#poor-examples-(edge-management)}

- `GET /api/v1/management/events` — example is `{}` (empty object)
- `get-capabilities` — example is a JSON string literal (not a parsed object), and is missing commas making it invalid JSON
- Many Edge endpoints have no examples at all (northbound/southbound mappings, domain tags by ID, combiners)
- Bridge GET example contains `password: password` in clear text

---

### 26\. Versioning Strategy {#26.-versioning-strategy}

**Severity: Low**

- API is versioned via URL path (`/api/v1/`)
- `info.version` is `2025.19-SNAPSHOT` — a development snapshot, not a release
- The `deprecated` flag is used on `getFunctions` but no `x-sunset` or replacement timeline is documented
- No `ETag` response headers are documented even though `If-Match` request headers are used on Data Hub endpoints

---

## Part III — User-Facing Strings Review (en-US Standards) {#part-iii-—-user-facing-strings-review-(en-us-standards)}

### 27\. Tag Names — Inconsistent Taxonomy {#27.-tag-names-—-inconsistent-taxonomy}

The top-level tags represent the API's primary navigation taxonomy. They are inconsistent:

| Tag                            | Issue                                                                      |
| :----------------------------- | :------------------------------------------------------------------------- |
| `Authentication Endpoint`      | Redundant "Endpoint" suffix — should be "Authentication"                   |
| `Gateway Endpoint`             | Same — should be "Gateway"                                                 |
| `Metrics Endpoint`             | Same — should be "Metrics"                                                 |
| `Bridges`                      | Good — bare noun                                                           |
| `Events`                       | Good                                                                       |
| `Frontend`                     | Good                                                                       |
| `Protocol Adapters`            | Good                                                                       |
| `Payload Sampling`             | Good                                                                       |
| `Topic Filters`                | Good                                                                       |
| `UNS`                          | Acronym without expansion — should be "Unified Namespace (UNS)" or similar |
| `Data Hub - Behavior Policies` | Good, hierarchical                                                         |
| `Data Hub - FSM`               | Acronym — should expand to "Finite State Machines"                         |
| `Data Hub - State`             | Ambiguous — should be "Data Hub \- Client State"                           |
| `Domain`                       | Very vague — used as a secondary tag alongside other primary tags          |
| `Pulse`                        | Good                                                                       |
| `Combiners`                    | Not defined in top-level tags                                              |

**Recommendation:** Standardize on bare nouns without "Endpoint" suffix. Expand all acronyms. Define all tags used.

---

### 28\. Summary/Description Conventions {#28.-summary/description-conventions}

Summaries should be imperative or noun phrases suitable for UI display and agentic tool descriptions. Many violations:

| Issue                                | Examples                                                                                                                      |
| :----------------------------------- | :---------------------------------------------------------------------------------------------------------------------------- |
| **Trailing periods in summaries**    | "Get the southbound mappings.", "Update a topic filter.", "Add a new domain tag to the specified adapter" (no period) — mixed |
| **Trailing spaces**                  | `get-listeners` summary: "Obtain the listeners configured "                                                                   |
| **Redundant summary \= description** | Many Edge endpoints have identical summary and description text                                                               |
| **Vacuous descriptions**             | "Obtain the details." on `get-bridges-status` and `get-adapters-status`                                                       |
| **Inconsistent verb**                | Mix of "Get", "Obtain", "List" for read operations; "Add", "Create" for write operations                                      |

**Recommendation for agentic use:** Every operation should have:

- A **summary** (\< 80 chars): imperative action phrase, no trailing period
- A **description** (1–3 sentences): explains the resource semantics, side effects, and auth requirements

---

### 29\. Schema Property Description Quality {#29.-schema-property-description-quality}

For agentic conversation and form generation, every schema property should have a clear en-US description that explains:

1. What the field represents (semantic meaning)
2. Constraints (length, pattern, valid values)
3. Whether it's user-supplied or server-generated

**Current state by subsystem:**

| Subsystem                             | Properties described | Constraints documented               | Quality |
| :------------------------------------ | :------------------- | :----------------------------------- | :------ |
| Data Hub (policies, schemas, scripts) | \~95%                | Good (required, readOnly, enums)     | Good    |
| Bridge                                | \~70%                | Partial (maxLength, pattern on some) | Fair    |
| Adapter                               | \~40%                | Poor (only `id` has constraints)     | Poor    |
| Combiner / Data Combining             | \~80%                | Good (required, format)              | Good    |
| Pulse / Managed Assets                | \~85%                | Good (required, readOnly, format)    | Good    |
| Frontend (config, notifications)      | \~60%                | Mixed                                | Fair    |
| Events                                | \~70%                | Fair                                 | Fair    |
| UNS / ISA-95                          | \~30%                | Poor (descriptions are single words) | Poor    |

---

## Part IV — Summary Scorecard {#part-iv-—-summary-scorecard}

| Aspect                                          | Rating    | Notes                                                                        |
| :---------------------------------------------- | :-------- | :--------------------------------------------------------------------------- |
| **Completeness — endpoints**                    | Good      | \~105 operations covering all product domains                                |
| **Completeness — security**                     | Missing   | No security declarations at all                                              |
| **Completeness — descriptions**                 | Poor      | 35+ schemas lack descriptions, 15 have copy-paste placeholder                |
| **Completeness — required fields**              | Poor      | 27+ schemas with no required fields, many clearly should have them           |
| **Correctness — descriptions**                  | Poor      | 18+ copy-paste errors producing misleading documentation                     |
| **Correctness — grammar (en-US)**               | Poor      | 20+ grammar/typo issues, multiple garbled descriptions                       |
| **Consistency — naming**                        | Poor      | Three operationId conventions mixed, tag naming inconsistent                 |
| **Consistency — HTTP semantics**                | Fair      | Data Hub follows REST conventions; Edge management does not                  |
| **Consistency — error handling**                | Fair      | Data Hub has discriminated error hierarchy; Edge uses generic ProblemDetails |
| **Consistency — pagination**                    | Poor      | Three different patterns, most endpoints unpaginated                         |
| **Schema quality — Data Hub**                   | Good      | Descriptions, required fields, proper status codes, examples                 |
| **Schema quality — Edge native**                | Poor      | Missing descriptions, missing required, empty response bodies                |
| **Agentic readiness — relationship modeling**   | Missing   | No formal cross-resource links                                               |
| **Agentic readiness — semantic clarity**        | Poor      | JsonNode catch-all, opaque configs, copy-paste descriptions                  |
| **Agentic readiness — discoverability**         | Fair      | Good enum usage where present, but many dynamic-only values                  |
| **User-facing strings — tag taxonomy**          | Poor      | Inconsistent naming, undefined tags, unexpanded acronyms                     |
| **User-facing strings — summaries**             | Fair      | Mixed quality, trailing punctuation inconsistency                            |
| **User-facing strings — property descriptions** | Poor–Fair | Varies wildly by subsystem                                                   |
| **Example quality**                             | Fair      | Excellent for Data Hub, poor to absent for Edge management                   |
| **Ontology extraction readiness**               | Poor      | Requires supplementary domain model to build reliable ontology               |

---

## Part V — Recommendations for Agentic Use {#part-v-—-recommendations-for-agentic-use}

### Priority 1 — Create a supplementary domain ontology (work around spec) {#priority-1-—-create-a-supplementary-domain-ontology-(work-around-spec)}

Since fixing the upstream spec may not be feasible, create a domain ontology document that:

1. **Maps all entity relationships** — adapter→tags, adapter→mappings, combiner→mappings→instructions, policy→schema, asset→mapping
2. **Provides semantic descriptions** for every `JsonNode` usage context
3. **Documents authentication requirements** per endpoint
4. **Normalizes the API taxonomy** — provides a consistent naming scheme for operations
5. **Lists all implicit constraints** — required fields, valid enums, pagination support

### Priority 2 — Supplement descriptions for agentic context {#priority-2-—-supplement-descriptions-for-agentic-context}

For each operation, provide:

- A clear 1-sentence purpose description
- The resource type it operates on
- Parent/child relationships
- Side effects (e.g., "creating a bridge also starts it")
- Pagination details

### Priority 3 — Address critical spec defects upstream {#priority-3-—-address-critical-spec-defects-upstream}

If spec changes are possible:

1. Add `securitySchemes` and per-operation `security`
2. Fix all copy-paste description errors
3. Add `required` fields to core schemas
4. Replace `JsonNode` catch-all with contextual wrapper types
5. Standardize operationId naming to camelCase
6. Standardize on `201`/`204` for create/delete
7. Fix grammar and garbled descriptions
