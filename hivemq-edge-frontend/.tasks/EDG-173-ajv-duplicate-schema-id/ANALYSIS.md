# AJV "schema with key or id already exists" — Analysis & Plan

## 1. Exact Error

```
Error: schema with key or id "###" already exists
```

Thrown by AJV when `compile()` is called for a schema whose `$id` is already registered in the
AJV instance's internal cache. The string `###` is the value of the schema's `$id` field.

---

## 2. Root Cause

**File**: `src/modules/TopicFilters/utils/topic-filter.schema.ts` — **line 47**

```typescript
validator.ajv.compile(json)
```

- `validator` is the **global singleton** imported from `@rjsf/validator-ajv8`.
  It is shared by the entire application for the lifetime of the browser session.
- `compile(schema)` registers the schema in AJV's internal cache **keyed by `schema.$id`**.
- When the same `$id` appears a second time (same upload, second render, re-open of drawer),
  AJV throws because the key already exists.
- The return value of `compile()` is **discarded** — the call is made purely for its
  side effect of throwing if the schema is structurally invalid.

### Why it's intermittent

It only fires when the uploaded schema has a `$id` property. Schemas without `$id` compile
without registration, so they never collide. Many hand-crafted schemas include `$id` as
good practice; tooling (e.g., `json-schema` CLI generators) always emits it.

### Trigger sequence

```
User uploads schema.json (has $id: "urn:my-app:sensor")
  → SchemaUploader.onUpload(dataUri)
  → DestinationSchemaLoader.handleSchemaUpload()
  → onChange(schema) → formData updated
  → component re-renders
  → validateSchemaFromDataURI(formData.destination.schema)   ← useMemo in DestinationSchemaLoader line 122
      → decodeDataUriJsonSchema(dataUri)
          → validator.ajv.compile(json)        ← SECOND CALL, same $id → BOOM
```

Or more simply: open drawer, upload schema, close drawer, re-open drawer → second mount
triggers the same `useMemo` chain.

---

## 3. All `validator.ajv.compile()` Call Sites

| File | Line | Context | Has $id risk? |
|---|---|---|---|
| `src/modules/TopicFilters/utils/topic-filter.schema.ts` | 47 | Destination schema upload validation | **YES** — user schemas may have `$id` |
| `src/components/rjsf/Form/ChakraRJSForm.tsx` | 103 | Batch upload post-validation (already strips `$schema`) | Low — adapter config schemas rarely have `$id` |
| `src/extensions/datahub/utils/node.utils.ts` | 398 | DataHub node validation | Low — internal schemas |
| `src/components/rjsf/BatchModeMappings/components/MappingsValidationStep.tsx` | 130 | Mappings validation step | Low — known internal schema |
| `src/modules/ProtocolAdapters/utils/export.utils.ts` | 75 | Adapter export validation | Low — internal schemas |

The **primary fix target** is line 47 in `topic-filter.schema.ts`. The other sites handle
internal/machine-generated schemas that are unlikely to carry a user-supplied `$id`.

---

## 4. Fix Options

### Option A — Strip `$id` before compiling (recommended)

```typescript
// Before compile, remove $id so AJV does not register the schema in its cache.
// The $id is not needed for structural validation (compile return value is discarded).
const { $id, ...schemaForValidation } = json
validator.ajv.compile(schemaForValidation)
```

- `json` (with `$id`) is still returned in `body` for downstream consumers.
- No caching → no collision. Each call compiles afresh, which is fine since the
  return value is never used.
- Same pattern RJSF itself uses in `ChakraRJSForm.tsx` line 102 for `$schema`:
  `const { $schema, ...rest } = schema`

**Pros**: One-line change, zero behaviour change for callers, fully reversible.
**Cons**: If the schema contains `$ref` pointing to its own `$id`
  (self-referential schema), AJV cannot resolve the `$ref` at compile time.
  Practically impossible for simple destination schemas.

---

### Option B — Remove from cache before compiling

```typescript
if (json.$id) validator.ajv.removeSchema(json.$id)
validator.ajv.compile(json)
```

**Pros**: Explicit lifecycle management; handles `$ref → $id` within the same schema.
**Cons**: More lines; mutates the global cache which could affect other in-flight
  uses (unlikely but possible under concurrent renders).

---

### Option C — Try/remove/retry

```typescript
try {
  validator.ajv.compile(json)
} catch (e) {
  if (json.$id && e instanceof Error && e.message.includes('already exists')) {
    validator.ajv.removeSchema(json.$id)
    validator.ajv.compile(json)
  } else throw e
}
```

**Pros**: Only removes from cache on actual collision.
**Cons**: Error-driven control flow; uses string matching on the error message.

---

## 5. Recommendation

**Use Option A.**

The compile call exists solely to validate schema structure (the return value is thrown
away). `$id` is irrelevant to structural validation. Stripping it is the minimal, safe
change that eliminates the collision entirely.

Also audit `ChakraRJSForm.tsx` line 103 for the same pattern — it already strips `$schema`
but not `$id`. Apply Option A there as well as a precaution.

---

## 6. Files to Change

### `src/modules/TopicFilters/utils/topic-filter.schema.ts` (1 line)

```typescript
// line 46-48 today:
// This will take care of some of the basic json error but not of a valid JSONSchema
validator.ajv.compile(json)

// After fix:
// This will take care of some of the basic json error but not of a valid JSONSchema
// Strip $id to prevent AJV from registering this schema in its global cache.
// The $id is irrelevant for structural validation (compile return value is discarded).
const { $id: _$id, ...schemaForValidation } = json
validator.ajv.compile(schemaForValidation)
```

### `src/modules/TopicFilters/utils/topic-filter.schema.spec.ts` (new test)

Add a `it.each` or standalone test in `describe('decodeDataUriJsonSchema')`:

```typescript
it('should not throw when called twice with the same $id', () => {
  const schemaWithId = encodeDataUriJsonSchema({
    $id: 'urn:test:duplicate-id',
    type: 'object',
    properties: { value: { type: 'number' } },
  })
  // First call registers nothing (or registers without $id)
  expect(() => decodeDataUriJsonSchema(schemaWithId)).not.toThrow()
  // Second call must not throw "schema with key or id already exists"
  expect(() => decodeDataUriJsonSchema(schemaWithId)).not.toThrow()
})
```

This test currently **fails** (reproduces the bug). After the fix it will pass.

### `src/components/rjsf/Form/ChakraRJSForm.tsx` (optional, precautionary)

Line 102–103 currently does:
```typescript
const { $schema, ...rest } = schema
const validate = validator.ajv.compile(rest)
```

This already strips `$schema`. Apply the same pattern to `$id`:
```typescript
const { $schema, $id: _$id, ...rest } = schema
const validate = validator.ajv.compile(rest)
```

---

## 7. Test Coverage Plan

| Test | File | Purpose |
|---|---|---|
| "should not throw when called twice with same $id" | `topic-filter.schema.spec.ts` | Reproduces bug; must fail before fix, pass after |
| Existing suite | `topic-filter.schema.spec.ts` | Must continue to pass |

No Cypress component tests needed — this is a pure utility function bug.

---

## 8. Scope Estimate

- `topic-filter.schema.ts`: 1–2 line change
- `topic-filter.schema.spec.ts`: 1 new test (8–10 lines)
- `ChakraRJSForm.tsx`: 1 line change (optional, precautionary)
- Run: `pnpm test` to verify Vitest suite

Total: small and contained.
