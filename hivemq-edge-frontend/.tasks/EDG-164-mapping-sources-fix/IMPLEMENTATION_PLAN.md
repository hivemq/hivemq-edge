# EDG-164 Implementation Plan

**Status:** Ready for review
**Branch:** epic/38915-epic-global-unique-tag-name

---

## Summary

Remove every read/write of the deprecated `sources.tags` and `sources.topicFilters`
fields from the frontend. The authoritative source of truth is `instructions[].sourceRef`
(fully scoped). This fixes two bugs:

1. **Unscoped chip appears** alongside scoped ones (deprecated `tags` prop fallback during initial render)
2. **Second schema never loads** (`reconstructSelectedSources` reads from `sources.tags`
   and uses `Array.find()` which always returns the first match for same-named tags)

---

## Files to Change

### 1. `src/modules/Mappings/utils/combining.utils.ts`

#### `reconstructSelectedSources` (lines 261–326) — REPLACE

Current logic iterates `formData.sources.tags` (string[]) and uses `instructions.find()`
to look up scope — fails for same-named tags from two adapters.

**New logic:**

```typescript
export const reconstructSelectedSources = (
  formData?: DataCombining,
  formContext?: CombinerContext
): { tags: DataIdentifierReference[]; topicFilters: DataIdentifierReference[] } => {
  if (!formData?.sources) return { tags: [], topicFilters: [] }

  // Derive from instructions (authoritative, fully scoped)
  const fromInstructions = (formData.instructions ?? [])
    .filter((inst) => inst.sourceRef != null)
    .map((inst) => inst.sourceRef!)

  // Also prepend scoped primary if not already represented
  const primary = formData.sources.primary
  const all: DataIdentifierReference[] = []
  if (primary?.scope) all.push(primary)
  all.push(...fromInstructions)

  // Deduplicate by id + type + scope
  const unique = all.reduce<DataIdentifierReference[]>((acc, ref) => {
    const exists = acc.find((r) => r.id === ref.id && r.type === ref.type && r.scope === ref.scope)
    return exists ? acc : [...acc, ref]
  }, [])

  return {
    tags: unique.filter((r) => r.type === DataIdentifierReference.type.TAG),
    topicFilters: unique.filter((r) => r.type === DataIdentifierReference.type.TOPIC_FILTER),
  }
}
```

Note: `formContext` parameter kept in signature (callers pass it) but no longer used.

#### `getFilteredDataReferences` (lines 125–139) — REMOVE Phase 1 fallback

Delete lines 125–139 (the `// Fallback to old behavior` block that reads `sources.tags`).
The `formContext?.selectedSources` branch at lines 108–123 is now the only path.
If `selectedSources` is undefined, the function returns `[]`.

---

### 2. `src/modules/Mappings/combiner/DataCombiningEditorField.tsx`

#### Lines 142–145 — REMOVE deprecated prop pass to `CombinedEntitySelect`

```tsx
// REMOVE these two lines:
tags={formData?.sources?.tags}
topicFilters={formData?.sources?.topicFilters}
```

#### Lines 175–178 — REMOVE deprecated writes in `onChange`

```typescript
// REMOVE these lines from props.onChange():
const tags = tagsWithScope.map((t) => t.id)
const filters = topicFiltersWithScope.map((tf) => tf.id)
// …and remove tags: tags, topicFilters: filters from the sources spread
```

Keep the `localContext.onSelectedSourcesChange(...)` call — that's the correct path.

---

### 3. `src/modules/Mappings/combiner/CombinedEntitySelect.tsx`

#### Interface `EntityReferenceSelectProps` (lines 21–28) — REMOVE deprecated props

```typescript
// REMOVE:
/** @deprecated */
tags?: Array<string>
/** @deprecated */
topicFilters?: Array<string>
```

#### Function signature (line 40) — REMOVE from destructuring

```typescript
// REMOVE tags, topicFilters from:
const CombinedEntitySelect: FC<...> = ({ id, tags, topicFilters, formContext, onChange, ...boxProps })
```

#### `values` useMemo (lines 128–138) — REMOVE backward-compat fallback

Delete the `// Backward compatibility: fall back to deprecated props` block.
When `formContext?.selectedSources` is undefined → return `[]`.
Update deps array to remove `tags, topicFilters`.

---

### 4. `src/modules/Mappings/combiner/DataCombiningTableField.tsx`

#### `getScopeForTag` (lines 24–32) — REMOVE (becomes dead code)

#### New mapping creation (lines 57–69, 73–87) — REMOVE deprecated fields

```typescript
// REMOVE from both handleAdd() and handleAddAsset():
tags: [],
topicFilters: [],
// (fields are optional in DataCombining type, safe to omit)
```

#### Sources column cell (lines 116–149) — REPLACE with instruction-based logic

```tsx
cell: (info) => {
  const row = info.row.original
  const primary = row.sources.primary

  const uniqueRefs = (row.instructions ?? [])
    .filter((inst) => inst.sourceRef != null)
    .map((inst) => inst.sourceRef!)
    .reduce<DataIdentifierReference[]>((acc, ref) => {
      const exists = acc.find((r) => r.id === ref.id && r.type === ref.type && r.scope === ref.scope)
      return exists ? acc : [...acc, ref]
    }, [])

  if (uniqueRefs.length === 0) return <Text>{t('combiner.unset')}</Text>

  const isPrimary = (ref: DataIdentifierReference) => primary?.type === ref.type && primary?.id === ref.id

  return (
    <HStack flexWrap="wrap">
      {uniqueRefs
        .filter((ref) => ref.type === DataIdentifierReference.type.TAG)
        .map((ref) => (
          <PrimaryWrapper key={`${ref.id}@${ref.scope ?? ''}`} isPrimary={isPrimary(ref)}>
            <PLCTag tagTitle={formatOwnershipString(ref)} />
          </PrimaryWrapper>
        ))}
      {uniqueRefs
        .filter((ref) => ref.type === DataIdentifierReference.type.TOPIC_FILTER)
        .map((ref) => (
          <PrimaryWrapper key={ref.id} isPrimary={isPrimary(ref)}>
            <TopicFilter tagTitle={ref.id} />
          </PrimaryWrapper>
        ))}
    </HStack>
  )
}
```

---

## Test File Changes

### `src/api/hooks/useCombiners/__handlers__/index.ts`

**`mockCombinerMapping`** currently has `instructions: []` and `sources.tags = ['my/tag/t1', 'my/tag/t3']`.
After the fix, empty instructions → only the scoped primary shows as a chip.
The "should render properly" test at line 78–80 checks for all 3 chips → would break.

**Fix:** Add instructions for all 3 sources:

```typescript
instructions: [
  {
    sourceRef: { id: 'my/tag/t1', type: DataIdentifierReference.type.TAG, scope: 'my-adapter' },
    source: '$.value',
    destination: '$.t1_value',
  },
  {
    sourceRef: { id: 'my/tag/t3', type: DataIdentifierReference.type.TAG, scope: 'my-adapter' },
    source: '$.value',
    destination: '$.t3_value',
  },
  {
    sourceRef: { id: 'my/topic/+/temp', type: DataIdentifierReference.type.TOPIC_FILTER, scope: null },
    source: '$.value',
    destination: '$.tf_value',
  },
]
```

### `src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx`

**Blog post screenshot test (lines 756–769):**
The formData has 3 instructions → 3 unique references → 3 schema panels.
Currently expects `have.length(2)` → change to `have.length(3)` and add third check.

**All 7 EDG-164 scenarios (S1–S7):** No changes to assertions needed — these were written
for the fixed behavior. Currently S3, S4, S5, S7 fail; after the fix all 7 should pass.

---

## What is NOT changing

- `src/api/__generated__/models/DataCombining.ts` — type stays as-is (backend-generated)
- `getAdapterIdForTag` in `combining.utils.ts` — kept (exported, may have other consumers)
- EDG-164 test scenarios S1–S7 assertions — written for the fixed behavior already

---

## Verification

```bash
# Run all DataCombiningEditorField tests (7 EDG-164 scenarios + existing)
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/DataCombiningEditorField.spec.cy.tsx"

# Run SchemaMerger tests (must still pass)
pnpm cypress:run:component --spec "src/modules/Mappings/combiner/SchemaMerger.spec.cy.tsx"

# TypeScript check — no new TS errors
pnpm build:tsc
```

Expected outcome: S3, S4, S5, S7 turn green. All other existing tests remain green.
