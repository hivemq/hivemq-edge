# EDG-38 — Implementation Proposal

## Problem Statement

`PropertyItem` unconditionally renders a lock icon for any property where `readOnly === true`. This
component is shared across many contexts. In most of them (topic filters, combiner sources,
combiner destinations) the readOnly flag is semantically irrelevant and should be invisible to the
user.

Only `TagSchemaPanel` (device tag write schema) has a legitimate need to display the lock icon,
because it tells the user which device tag fields cannot be written.

---

## Proposed Solution

**Add a single `showReadOnly` boolean prop** to `PropertyItem` and `JsonSchemaBrowser`, defaulting
to `true` to preserve the current behaviour. Each call site that should suppress the indicator
passes `showReadOnly={false}`.

This is the minimal-change approach: two component definitions change, N call sites add one prop.
No logic moves, no abstractions are introduced, no data is mutated.

---

## Changes Required

### 1. `PropertyItem` — add `showReadOnly` prop

```tsx
interface PropertyItemProps {
  // ... existing props
  showReadOnly?: boolean // default: true
}
```

Condition the lock icon:

```tsx
// Before
{
  isReadOnly(property) && <LockIcon />
}

// After
{
  showReadOnly !== false && isReadOnly(property) && <LockIcon />
}
```

### 2. `JsonSchemaBrowser` — add `showReadOnly` prop and thread it through

```tsx
interface JsonSchemaBrowserProps extends ListProps {
  // ... existing props
  showReadOnly?: boolean // default: true
}
```

Pass it to each `PropertyItem` rendered inside the list.

### 3. `MappingInstruction` — no change

`MappingInstruction` renders a blocking "Read-only" card (no drop zone) when
`isReadOnly(property)` is true, and `MappingInstructionList` calls `filterReadOnlyInstructions`
to strip readOnly properties from the active instruction list.

This is the **governing behaviour** for the combiner destination: readOnly destination properties
are intentionally excluded from mapping. The Linear ticket predates this filtering logic and did
not account for it. This behaviour is kept as-is and is out of scope for EDG-38.

### 4. Call-site changes

The deciding question for each context is: **does the user need to know that a field is read-only
in order to act correctly?**

| File                       | `showReadOnly` | Rationale                                                                                                                                                                                      |
| -------------------------- | -------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `TagSchemaPanel.tsx`       | `true`         | Shows the write schema for a device tag. readOnly signals which fields the adapter will not accept writes to — actionable information for the user configuring tag writes.                     |
| `MetadataExplorer.tsx`     | `true`         | Explores live MQTT topic data for informational purposes. Showing readOnly preserves the full fidelity of the schema as sampled, which is useful for understanding the data model.             |
| `TopicSchemaManager.tsx`   | `false`        | Displays the schema attached to a topic filter. Topic filters are sources we read _from_; the readOnly flag describes Java deserialization constraints that have no meaning in this context.   |
| `SchemaSampler.tsx`        | `false`        | Infers a schema by sampling live topic messages. Same reasoning as `TopicSchemaManager` — readOnly is a backend serialization concept irrelevant to message sampling.                          |
| `DataModelSources.tsx`     | `false`        | Shows source properties available for drag-and-drop mapping. Users are selecting fields to read _from_; readOnly on a source field is not a constraint here.                                   |
| `DataModelDestination.tsx` | `false`        | Shows destination properties for the MQTT write mapping panel. The destination is an assembled outbound message; readOnly describes inbound deserialization and should be ignored.             |
| `CombinedSchemaLoader.tsx` | `false`        | Browses source schemas in the combiner editor (tags and topic filters used as inputs). readOnly has no bearing on reading values out of incoming messages.                                     |
| `SchemaMerger.tsx`         | `false`        | Previews properties inferred from source schemas before generating the combiner destination schema. These are source-side properties being reviewed, not a write target.                       |
| `SchemaWidget.tsx`         | `false`        | RJSF form widget that renders a topic filter schema for display. The schema is stored as a data URI and represents an MQTT message structure; readOnly is a backend annotation, not UI intent. |

---

## Alternatives Considered

### Option B: Default `showReadOnly={false}` (opt-in instead of opt-out)

Only `TagSchemaPanel` would need to add `showReadOnly={true}` — just one change instead of eight.
However, this changes the default behaviour and could silently suppress the lock icon in any
future consumer of `PropertyItem` that forgets to opt in.

**Rejected** in favour of the safe backward-compatible default.

### Option C: Filter readOnly at data level

Strip `readOnly` from properties in the affected data flows before they reach `PropertyItem`.
Requires mutating schemas that are also used for other purposes, and is less transparent.

**Rejected** — side-effects risk.

### Option D: React context flag

Wrap affected subtrees in a context provider that suppresses readOnly. More flexible but
introduces invisible action-at-a-distance and is over-engineered for this case.

**Rejected** — unnecessary complexity.

---

## Test Coverage

No existing test verifies the absence of the lock icon in non-combiner contexts. New tests needed:

### `PropertyItem.spec.cy.tsx`

- Existing tests already cover `showReadOnly` display (lock icon present / absent based on
  `readOnly` flag).
- Add: with `readOnly: true` **and** `showReadOnly={false}` → lock icon must **not** exist.

### `JsonSchemaBrowser.spec.cy.tsx`

- Add: with a schema containing a `readOnly` property and `showReadOnly={false}` → no lock icon
  rendered in the list.

### Context-level specs (where test files exist)

For each context that passes `showReadOnly={false}`, add at minimum one test asserting that a
`readOnly` property renders **without** the lock icon. Priority order:

1. `TopicSchemaManager` / `SchemaSampler` (topic filter contexts — directly cited in the issue)
2. `DataModelDestination` / `DataModelSources` (combiner MQTT mapping)
3. `SchemaMerger` / `CombinedSchemaLoader` (combiner schema browser)
4. `SchemaWidget` (RJSF form widget)

---

## Decision Log

**`MappingInstruction` blocking behaviour**: kept as-is. The `filterReadOnlyInstructions` call in
`MappingInstructionList` and the blocking card in `MappingInstruction` are the intentional
governing behaviour for combiner destinations. Out of scope for EDG-38.
