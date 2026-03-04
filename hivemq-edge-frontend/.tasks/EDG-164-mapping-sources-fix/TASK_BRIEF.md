# Task EDG-164: Remove deprecated sources.tags / sources.topicFilters

**Linear:** EDG-164
**Branch:** epic/38915-epic-global-unique-tag-name
**Status:** 🔧 In Progress

---

## The Two Bugs

### Bug 1 — Unscoped tag appears alongside scoped ones in integration points

When a mapping has same-named tags from two adapters (e.g. `o1t1` from adapters `o1` and `o2`):

- `o1t1` (unscoped) appears as a chip — comes from the deprecated `tags` prop passed to
  `CombinedEntitySelect`, visible during the initial render before `selectedSources` is set
- `o1::o1t1` appears twice (duplicate) — `reconstructSelectedSources` resolves both
  `sources.tags` entries to the same scope because `Array.find()` always returns the first
  matching instruction

### Bug 2 — Schema for second tag never loads

Because `reconstructSelectedSources` resolves both entries to `{scope: 'o1'}`,
`getFilteredDataReferences` deduplicates them to a single reference. The second adapter's
schema (`o2::o1t1`) is never requested.

---

## Root Cause

`reconstructSelectedSources` iterates `formData.sources.tags` (a `string[]`) to rebuild
`selectedSources`. For same-named tags:

```
sources.tags = ['o1t1', 'o1t1']
  → Strategy 2: instructions.find(inst => inst.sourceRef.id === 'o1t1')
  → Returns the FIRST match every time → both entries get scope 'o1'
  → selectedSources = [{id:'o1t1', scope:'o1'}, {id:'o1t1', scope:'o1'}]
  → deduplicated to one reference → o2's schema never loads
```

The authoritative source of truth is **`instructions[].sourceRef`** — fully scoped.
`sources.tags` is a deprecated, scope-less remnant.

---

## Fix Strategy

### 1. `reconstructSelectedSources` — derive from instructions only

Instead of iterating `sources.tags`, collect unique `sourceRef` objects from `instructions`:

```typescript
// NEW: derive from instructions (authoritative, scoped)
const sourceRefs = (formData.instructions ?? []).filter((inst) => inst.sourceRef).map((inst) => inst.sourceRef!)

// Also include scoped primary if not already represented
const primary = formData.sources.primary
if (primary?.scope) {
  /* deduplicate-add */
}

// Deduplicate by id+type+scope
```

### 2. Remove all reads/writes of `sources.tags` / `sources.topicFilters`

Use the TS rename trick to find every access:

- Rename `sources.tags` → `sources.DUMMY_TAGS`
- Rename `sources.topicFilters` → `sources.DUMMY_TOPICFILTERS`
- Fix all TS errors → those are the targets to remove

**Locations confirmed:**

| File                                      | Access                            | Action                             |
| ----------------------------------------- | --------------------------------- | ---------------------------------- |
| `DataCombiningEditorField.tsx:144-145`    | READ (prop pass)                  | Remove deprecated prop pass        |
| `DataCombiningEditorField.tsx:176-178`    | WRITE (onChange)                  | Remove write                       |
| `CombinedEntitySelect.tsx:22-28, 128-138` | READ (fallback values)            | Remove deprecated props + fallback |
| `combining.utils.ts:126-139`              | READ (Phase 1 fallback)           | Remove fallback block              |
| `combining.utils.ts:271-296`              | READ (reconstructSelectedSources) | Replace with instruction-based     |
| `DataCombiningTableField.tsx:118-140`     | READ (display)                    | Replace with instruction-based     |

---

## Test Scenarios (DataCombiningEditorField)

| #   | Scenario                                                   | Expected                                             |
| --- | ---------------------------------------------------------- | ---------------------------------------------------- |
| 1   | Fresh mapping, no instructions, no tags                    | Integration points empty                             |
| 2   | Single adapter, scoped tags, with instructions             | Correct scoped chips, schema loads                   |
| 3   | **Same-named tags from two adapters, with instructions**   | TWO distinct chips (o1::o1t1, o2::o1t1), TWO schemas |
| 4   | Old deprecated data: sources.tags present, no instructions | Empty selection (deprecated ignored)                 |
| 5   | Old deprecated data: sources.tags + some instructions      | Only instruction-based selection shown               |
| 6   | Topic filters with instructions                            | Correct chips, schemas load                          |
| 7   | Mixed tags + topic filters with instructions               | All schemas load                                     |
