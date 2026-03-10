# Task EDG-168: Mapping Schema Inference — Scope Bug Fix

**Task ID:** EDG-168
**Branch:** epic/38915-epic-global-unique-tag-name
**Status:** 🔧 In Progress

---

## Problem

In the Combiner mapping editor's "Infer Schema" flow (`SchemaMerger.tsx`), when two adapters both
have tags with the same name (e.g., `temperature`), the generated schema contains **duplicate
property keys** — which is illegal in JSON Schema.

## Root Cause

`SchemaMerger.tsx` builds a `tagIndexMap` keyed by raw tag ID only:

```typescript
const tagIndexMap = new Map(formData?.sources?.tags?.map((tag, index) => [tag, index]))
```

`formData.sources.tags` is a `string[]`. If two adapters share a tag named `temperature`, the
array is `['temperature', 'temperature']`. `new Map()` collapses both entries to the same key
(`'temperature' => 1` — last one wins). Both references (even though they have different scopes)
look up the same index and receive the **same origin stub** (e.g., `tg1`).

The result: both schemas produce the same renamed key (`tg1_temperature`), causing duplicate
entries in the flat property list that `getSchemaFromPropertyList` silently overwrites.

## Fix

Build the `tagIndexMap` from `references` (which carry scope) using a composite `id::scope` key:

```typescript
const tagIndexMap = new Map(
  references
    .filter((r) => r.type === DataIdentifierReference.type.TAG)
    .map((ref, index) => [`${ref.id}::${ref.scope ?? ''}`, index])
)
```

Use the same composite key when looking up during the reduce:

```typescript
const index = tagIndexMap.get(`${reference.id}::${reference.scope ?? ''}`)
```

## Related Tasks

- **EDG-34** — Instruction Scope (established `sourceRef.scope` data model)
- **EDG-35** — Tag Ownership Rendering (scope-aware display)
- **38943** — Mapping Ownership Overall (parent)
- **38936** — Tag Reference Scope (prerequisite)
