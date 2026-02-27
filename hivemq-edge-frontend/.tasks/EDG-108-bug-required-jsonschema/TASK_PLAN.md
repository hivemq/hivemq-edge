# EDG-108: Bug Analysis & Fix — `required: boolean` Leaks into JSON Schema Property Definitions

## Discovery Context

This bug was found as a side-effect of **EDG-35** (tag ownership rendering). EDG-35 introduced
`scope` fields onto `DataIdentifierReference` objects, which changed how topic filter schemas are
constructed. Topic filter schemas now correctly emit `"required": ["fieldA", "fieldB"]` at the
object level (valid JSON Schema Draft-07). That valid `required` array is then read by
`getPropertyListFrom()`, which converts it to a per-property `required: boolean` flag on
`FlatJSONSchema7` nodes. The bug existed before EDG-35 but was dormant — it only triggers when
source schemas actually have `required` fields, which the EDG-35 changes caused to happen.

The failure manifests in the E2E test `combiner.spec.cy.ts` ("should create the first combiner")
when clicking **Infer Schema** in the mapping editor.

---

## Root Cause Analysis

### The Schema Pipeline

```
JSON Schema (from API / topic filter)
        │
        ▼  getPropertyListFrom()
FlatJSONSchema7[]          ← internal representation; required: boolean per property
        │
        │  (SchemaMerger assigns origin prefixes: tf0_description, tg1_name, …)
        ▼  getSchemaFromPropertyList()
RJSFSchema                 ← output; must be valid JSON Schema Draft-07
        │
        ▼  encodeDataUriJsonSchema()
data:application/json;...  ← stored as destination.schema
        │
        ▼  AJV8 meta-validates on compile
❌ Error: required must be array
```

### The Type Mismatch

`FlatJSONSchema7` extends `JSONSchema7` but overrides the `required` field:

```typescript
// json-schema.utils.ts
export interface FlatJSONSchema7 extends Omit<JSONSchema7, 'required'> {
  path: string[]
  key: string
  arrayType?: string
  origin?: string
  metadata?: DataReference
  required?: boolean // ← boolean, NOT string[]
}
```

This is a deliberate internal convenience: `getPropertyListFrom()` reads the parent object's
`required: string[]` array and sets `required: true` on each matching child property node,
making it easy to track which properties are required as the list is passed through the pipeline.

### Where It Breaks

`getSchemaFromPropertyList()` converts `FlatJSONSchema7[]` back into a proper `RJSFSchema`.
The bug was in the destructuring — `required` was not extracted, so it fell through into `...rest`
and was spread directly onto the output property definition:

```typescript
// Before (broken)
const { path, key, arrayType, origin, ...rest } = property
//                                      ^^^^ required: boolean is still in rest

;(root.properties as RJSFSchema)[property.key] = {
  type: property.type,
  ...rest,   // ← required: true appears here as a boolean on the property
  ...
}
```

**Why this is invalid:** JSON Schema Draft-07 (and AJV8's meta-schema) require that `required`
on a property definition is a `string[]` declared on the _parent object_ — it is not a valid
keyword on an individual property node. The Draft-04 style of `"required": true` per-property
was removed in later drafts.

AJV8 meta-validates every schema it compiles. When the generated schema contains
`"tf0_description": { "type": "string", "required": true }`, AJV8 rejects it:

```
schema is invalid: data/properties/tf0_description/required must be array
```

This error propagates up to the UI as an error banner, making the destination schema unusable
and breaking all instruction resolution downstream.

### Why It Was Dormant Before EDG-35

The bug only triggers when source schemas have `"required"` arrays at the object level.
Before EDG-35, the topic filter schemas used in tests (`MOCK_TOPIC_FILTER_SCHEMA_VALID`) either
lacked `required` arrays or were not exercised via the `SchemaMerger` → `getSchemaFromPropertyList`
path in automated tests. EDG-35 changes caused the E2E test to exercise this path with a schema
that has `"required": ["description", "name"]`, surfacing the latent bug.

---

## The Fix

Extract `required` from the `...rest` destructuring in **both** the root-level and nested property
branches of `getSchemaFromPropertyList()`. Instead of leaking it into the output property
definition, accumulate required property keys into the parent schema's `required: string[]` array —
the correct JSON Schema Draft-07 location.

**File:** `src/components/rjsf/MqttTransformation/utils/json-schema.utils.ts`

### Root-level properties (`property.path.length === 0`)

```typescript
// Before
const { path, key, arrayType, origin, ...rest } = property
;(root.properties as RJSFSchema)[property.key] = {
  type: property.type,
  ...rest,
  ...(property.type === 'object' && { properties: {} }),
  ...(property.type === 'array' && { items: { type: arrayType } }),
}

// After
const { path, key, arrayType, origin, required, ...rest } = property
;(root.properties as RJSFSchema)[property.key] = {
  type: property.type,
  ...rest,
  ...(property.type === 'object' && { properties: {} }),
  ...(property.type === 'array' && { items: { type: arrayType } }),
}
if (required) {
  if (!root.required) root.required = []
  ;(root.required as string[]).push(key)
}
```

### Nested properties (`property.path.length > 0`)

```typescript
// Before
const { path, key, arrayType, origin, ...rest } = property
newRoot[property.path[0]].properties[property.key] = {
  type: property.type,
  ...rest,
  ...(property.type === 'array' && { items: { type: arrayType } }),
}

// After
const { path, key, arrayType, origin, required, ...rest } = property
newRoot[property.path[0]].properties[property.key] = {
  type: property.type,
  ...rest,
  ...(property.type === 'array' && { items: { type: arrayType } }),
}
if (required) {
  const parentProp = newRoot[property.path[0]]
  if (!parentProp.required) parentProp.required = []
  ;(parentProp.required as string[]).push(key)
}
```

---

## Test Coverage

### Unit tests (`json-schema.utils.spec.ts`)

Four new test cases added to the `getSchemaFromPropertyList` describe block:

| Test                                                                        | Validates                                                                               |
| --------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Updated "filter out internal properties"                                    | `required` is NOT present as boolean on property definition                             |
| "promote required:true on root-level properties to the root required array" | `root.required` is a `string[]` containing required keys                                |
| "should not set root required array when no properties are required"        | `root.required` is `undefined` when nothing is required                                 |
| "promote required:true on nested properties to the parent required array"   | Parent property's `required` is a `string[]`; nested property has no boolean `required` |

**Result:** 32/32 tests pass (was 28/28 before; 4 new tests added).

### Component tests

All relevant component tests pass (run individually — Cypress has a known cross-spec uncaught
error when multiple specs are combined in a single run):

| Spec                                    | Tests | Result                              |
| --------------------------------------- | ----- | ----------------------------------- |
| `DestinationSchemaLoader.spec.cy.tsx`   | 3     | ✅ 3 pass                           |
| `SchemaMerger.spec.cy.tsx`              | 3     | ✅ 2 pass, 1 pre-existing `it.skip` |
| `DataCombiningEditorField.spec.cy.tsx`  | 4     | ✅ 2 pass, 2 pre-existing `it.skip` |
| `DataCombiningEditorDrawer.spec.cy.tsx` | 2     | ✅ 2 pass                           |

### E2E test

`cypress/e2e/mappings/combiner.spec.cy.ts` — "should create the first combiner": ✅ passes.

This test exercises the full schema inference path:

1. Select topic filter source
2. Click **Infer Schema**
3. Verify 2 schema fields rendered
4. Verify instructions resolved correctly

---

## Files Modified

| File                                                                     | Change                                                                                 |
| ------------------------------------------------------------------------ | -------------------------------------------------------------------------------------- |
| `src/components/rjsf/MqttTransformation/utils/json-schema.utils.ts`      | Extract `required` from `...rest` spread; promote to parent `required: string[]` array |
| `src/components/rjsf/MqttTransformation/utils/json-schema.utils.spec.ts` | 4 new unit tests covering `required` promotion behaviour                               |

---

## Design Notes

### Why `FlatJSONSchema7.required` is `boolean`, not `string[]`

The internal `boolean` flag is intentional and correct for the pipeline:
`getPropertyListFrom()` processes one property at a time, and the per-property `boolean` is simpler
to carry through the flattened list than reconstructing a `string[]` at each level. The conversion
back to `string[]` is the responsibility of `getSchemaFromPropertyList()` — this fix ensures that
responsibility is fulfilled correctly.

### Why the fix is in `getSchemaFromPropertyList`, not `FlatJSONSchema7`

Changing `FlatJSONSchema7.required` back to `string[]` would require every call site in the
pipeline to reconstruct arrays, adding noise to code that only needs a boolean signal. The fix
is correctly placed at the output boundary where `FlatJSONSchema7` is converted back to valid
`RJSFSchema` / `JSONSchema7`.
