# Deprecated Fields Analysis: tags[] and topicFilters[]

**Task:** 38943-mapping-ownership-review
**Date:** 2026-02-11
**Status:** Analysis of deprecated API fields usage in frontend

---

## Executive Summary

The `sources.tags` and `sources.topicFilters` fields are marked as `@deprecated` in the API, but are still **actively used** in the frontend for backward compatibility and temporary API synchronization.

**Key Findings:**

- ✅ **Reading (3 locations)**: Necessary for backward compatibility with existing mappings
- ⚠️ **Writing (2 locations)**: TEMPORARY - can be removed once API migration is complete
- 🔴 **Critical usage (1 location)**: Workspace status computation still relies on these fields

**Safe to remove:** Write operations (after API migration)
**Must keep:** Read operations (for backward compatibility)
**Needs refactoring:** Workspace status computation

---

## Usage Breakdown

### 📖 READ Operations (Backward Compatibility)

These usages are **necessary** for loading existing mappings that were created before Option H implementation:

#### 1. ✅ KEEP - Reconstruction Logic

**File:** `src/modules/Mappings/utils/combining.utils.ts:270-295`

```typescript
export const reconstructSelectedSources = (
  formData?: DataCombining,
  formContext?: CombinerContext
): { tags: DataIdentifierReference[]; topicFilters: DataIdentifierReference[] } => {
  if (!formData?.sources) {
    return { tags: [], topicFilters: [] }
  }

  // Reconstruct tags with scope
  const tags =
    formData.sources.tags?.map((tagId) => {
      // ✅ READ - Backward compat
      // Strategy 1: Check if this is the primary
      // Strategy 2: Find scope from instructions
      // Strategy 3: Fallback to context lookup
      const adapterId = getAdapterIdForTag(tagId, formContext)
      return {
        id: tagId,
        type: DataIdentifierReference.type.TAG,
        scope: adapterId ?? null,
      }
    }) || []

  // Reconstruct topic filters (scope always null)
  const topicFilters =
    formData.sources.topicFilters?.map((tfId) => {
      // ✅ READ - Backward compat
      // Similar reconstruction logic...
    }) || []

  return { tags, topicFilters }
}
```

**Why necessary:**

- Existing mappings saved before Option H only have `string[]` in these fields
- Without reconstruction, these mappings cannot be loaded/edited
- Provides 3-tier fallback to recover ownership information

**Can remove?** ❌ NO - Required until all existing mappings are migrated

---

#### 2. 🔴 CRITICAL - Workspace Status Computation

**File:** `src/modules/Workspace/utils/status-adapter-edge-operational.utils.ts:79-87`

```typescript
export const doesAdapterProvideAnyCombiningData = (
  adapterId: string,
  combinerMappings: DataCombining[],
  deviceTagsNames: string[]
): boolean => {
  const deviceTags = new Set(deviceTagsNames)

  return combinerMappings.some((mapping) => {
    // Check primary source first...

    // Check additional tags array if present
    // NOTE: sources.tags is string[] without scope info, so we can only check tag name
    // This is a limitation until Stage 2 adds full DataIdentifierReference[] for tags
    if (mapping.sources.tags) {
      // 🔴 READ - Status computation
      return mapping.sources.tags.some((tagName) => deviceTags.has(tagName))
    }

    return false
  })
}
```

**Why problematic:**

- Used for edge operational status in workspace (adapter → combiner edges)
- Only checks tag **name**, not **ownership/scope** (limitation acknowledged in comment)
- Cannot determine if adapter actually owns the tag
- **False positives possible**: Any tag with matching name will show connection, even if different adapter

**Can remove?** ❌ NO - But **NEEDS REFACTORING**

**Recommended fix:**

```typescript
// Option 1: Use instructions instead
if (mapping.instructions) {
  return mapping.instructions.some((inst) => inst.sourceRef?.scope === adapterId && deviceTags.has(inst.sourceRef.id))
}

// Option 2: Access selectedSources from context (if available)
// This would require passing formContext through to this utility
```

---

### ✏️ WRITE Operations (Temporary Synchronization)

These usages are **temporary** during the migration period and can be removed:

#### 3. ⚠️ TEMPORARY - Form Field Updates

**File:** `src/modules/Mappings/combiner/DataCombiningEditorField.tsx:161-178`

```typescript
// Update selectedSources state (Option H strategy)
if (localContext?.onSelectedSourcesChange) {
  localContext.onSelectedSourcesChange({
    tags: tagsWithScope,
    topicFilters: topicFiltersWithScope,
  })
}

// TEMPORARY: Also update deprecated API fields during transition
const tags = tagsWithScope.map((t) => t.id) // ⚠️ WRITE - Temporary
const filters = topicFiltersWithScope.map((tf) => tf.id) // ⚠️ WRITE - Temporary

props.onChange({
  ...formData,
  sources: {
    ...formData.sources,
    tags: tags, // ⚠️ WRITE to deprecated field
    topicFilters: filters, // ⚠️ WRITE to deprecated field
    primary: isPrimary ? formData.sources.primary : undefined,
  },
})
```

**Why temporary:**

- Keeps API fields synchronized during migration period
- Backend expects these fields until API version 2.0
- Frontend now uses `selectedSources` as source of truth

**Can remove?** ✅ YES - After API migration (Option H → A)

**When to remove:**

- When backend no longer expects `sources.tags` and `sources.topicFilters`
- When all on-premises customers have migrated
- As part of Option H → A migration (see migration guide)

---

#### 4. ⚠️ TEMPORARY - New Mapping Creation

**File:** `src/modules/Mappings/combiner/DataCombiningTableField.tsx:46-73`

```typescript
const newMapping: DataCombining = {
  id: uuidv4(),
  sources: {
    primary: { id: '', type: DataIdentifierReference.type.TAG },
    // DEPRECATED: sources.tags and sources.topicFilters will be removed in future API version
    // Backend reconstructs from instructions, so empty arrays are acceptable
    tags: [], // ⚠️ WRITE - Empty array to satisfy type
    topicFilters: [], // ⚠️ WRITE - Empty array to satisfy type
  },
  destination: { topic: asset.topic, assetId: asset.id, schema: asset.schema },
  instructions: [],
}
```

**Why temporary:**

- Satisfies TypeScript `DataCombining` type requirements
- Backend expects these fields to exist (even if empty)
- Empty arrays are acceptable - backend reconstructs from instructions

**Can remove?** ✅ YES - After API migration

**When to remove:**

- Update `DataCombining` type to make `tags?` and `topicFilters?` truly optional
- Or remove fields entirely from type definition
- As part of API schema update (Option H → A)

---

## Validation Usage

### ✅ NO USAGE - Validator Ignores Deprecated Fields

**File:** `src/modules/Mappings/hooks/useValidateCombiner.ts`

**Finding:** The validator does **NOT** read `formData.sources.tags` or `formData.sources.topicFilters`

**Why this is good:**

- Validation uses `allDataSourcesFromEntities` computed from queries (line 82-98)
- Validation logic already independent of deprecated fields
- No changes needed when fields are removed

---

## Migration Roadmap

### Phase 1: Current State (Option H) ✅ COMPLETE

- [x] EntityQuery type implemented
- [x] SelectedSources tracked in formContext
- [x] Reconstruction logic handles backward compatibility
- [x] Deprecated fields still written for API compatibility

### Phase 2: Deprecation Period (Current) 🔄 IN PROGRESS

**Keep:**

- ✅ Read operations for backward compatibility
- ✅ Reconstruction logic in `combining.utils.ts`

**Temporary:**

- ⚠️ Write operations in `DataCombiningEditorField.tsx`
- ⚠️ Write operations in `DataCombiningTableField.tsx`

**Critical:**

- 🔴 Refactor `status-adapter-edge-operational.utils.ts` to use instructions instead

### Phase 3: API Migration (Option H → A) 📅 FUTURE

**When on-premises constraints lift:**

1. **Backend Changes:**

   - Remove `tags` and `topicFilters` from API schema
   - Ensure instructions contain all necessary data

2. **Frontend Cleanup (can be removed safely):**

   ```typescript
   // Remove these writes:
   // DataCombiningEditorField.tsx:161-178
   tags: tags,                    // ❌ Remove
   topicFilters: filters,         // ❌ Remove

   // DataCombiningTableField.tsx:52,69
   tags: [],                      // ❌ Remove
   topicFilters: [],              // ❌ Remove
   ```

3. **Keep for backward compatibility:**

   ```typescript
   // Keep these reads:
   // combining.utils.ts:270-295
   formData.sources.tags?.map(...)        // ✅ Keep
   formData.sources.topicFilters?.map(...) // ✅ Keep
   ```

   **Rationale:** Old mappings in the database still have these fields

4. **Must refactor before removal:**
   ```typescript
   // status-adapter-edge-operational.utils.ts:82-84
   // Replace with instructions-based check
   if (mapping.sources.tags) {  // 🔴 Must refactor first
   ```

### Phase 4: Complete Removal 📅 FAR FUTURE

**Only after:**

- All mappings migrated to new format
- Database cleanup complete
- On-premises customers fully updated

**Then remove:**

- Read operations from reconstruction logic
- Type definitions (`tags?: string[]`, `topicFilters?: string[]`)
- All references to deprecated fields

---

## Summary Table

| Location                                      | Type  | Status           | Can Remove? | When?                   | Priority |
| --------------------------------------------- | ----- | ---------------- | ----------- | ----------------------- | -------- |
| `combining.utils.ts:270`                      | READ  | Backward compat  | ❌ NO       | Phase 4 (far future)    | Keep     |
| `combining.utils.ts:299`                      | READ  | Backward compat  | ❌ NO       | Phase 4 (far future)    | Keep     |
| `status-adapter-edge-operational.utils.ts:82` | READ  | Critical usage   | 🔴 NO       | Needs refactoring first | **HIGH** |
| `DataCombiningEditorField.tsx:174`            | WRITE | Temporary sync   | ✅ YES      | Phase 3 (API migration) | Medium   |
| `DataCombiningEditorField.tsx:175`            | WRITE | Temporary sync   | ✅ YES      | Phase 3 (API migration) | Medium   |
| `DataCombiningTableField.tsx:52`              | WRITE | Type requirement | ✅ YES      | Phase 3 (API migration) | Low      |
| `DataCombiningTableField.tsx:69`              | WRITE | Type requirement | ✅ YES      | Phase 3 (API migration) | Low      |

---

## Recommendations

### Immediate Actions

1. **🔴 HIGH PRIORITY - Refactor Status Computation**

   **File:** `src/modules/Workspace/utils/status-adapter-edge-operational.utils.ts`

   **Issue:** Uses deprecated `sources.tags` field without scope validation

   **Fix:** Use instructions-based check instead:

   ```typescript
   export const doesAdapterProvideAnyCombiningData = (
     adapterId: string,
     combinerMappings: DataCombining[],
     deviceTagsNames: string[]
   ): boolean => {
     const deviceTags = new Set(deviceTagsNames)

     return combinerMappings.some((mapping) => {
       // Check primary first...
       if (mapping.sources.primary?.scope === adapterId && deviceTags.has(mapping.sources.primary.id)) {
         return true
       }

       // Check instructions (has scope information)
       if (mapping.instructions) {
         return mapping.instructions.some(
           (inst) =>
             inst.sourceRef?.scope === adapterId &&
             inst.sourceRef?.type === DataIdentifierReference.type.TAG &&
             deviceTags.has(inst.sourceRef.id)
         )
       }

       return false
     })
   }
   ```

   **Test:** Update `status-adapter-edge-operational.utils.spec.ts` accordingly

### Short-Term (Current Phase)

2. **Document TEMPORARY nature clearly**

   Add comments to write operations emphasizing they're temporary:

   ```typescript
   // TEMPORARY (Option H migration): Remove after API v2.0
   // See: .tasks/38943-mapping-ownership-review/DEPRECATED_FIELDS_ANALYSIS.md
   tags: tags,
   topicFilters: filters,
   ```

3. **Add deprecation ESLint rule (optional)**

   Could add custom rule to warn when accessing these fields:

   ```typescript
   // eslint-disable-next-line @deprecated/sources-tags
   formData.sources.tags
   ```

### Long-Term (API Migration Phase)

4. **Remove write operations** when backend ready

   See Phase 3 above for specific lines to remove

5. **Keep read operations** for backward compatibility

   Reconstruction logic should remain even after API migration

6. **Plan complete removal** only after full database migration

---

## Testing Checklist

To verify safe removal of deprecated field writes:

- [ ] Load existing mapping (created before Option H) - reconstruction should work
- [ ] Create new mapping - should save successfully without tags/topicFilters
- [ ] Edit existing mapping - should preserve data without deprecated fields
- [ ] Workspace status edges - should show correct connections (after refactor)
- [ ] Validation - should work correctly (already independent)
- [ ] Backward compatibility tests pass
- [ ] No TypeScript errors after field removal

---

## Questions?

**Q: Can we remove the write operations now?**
A: No - backend still expects these fields. Wait for API v2.0.

**Q: Can we remove the read operations now?**
A: No - needed for backward compatibility with existing mappings.

**Q: What's blocking complete removal?**
A:

1. Backend API migration (removes field requirement)
2. Status computation refactor (critical blocker)
3. Database migration (existing mappings)
4. On-premises customer updates

**Q: What's the critical path?**
A: Refactor `status-adapter-edge-operational.utils.ts` FIRST, then plan API migration.

---

**Generated:** 2026-02-11
**Task:** 38943-mapping-ownership-review
**Next Action:** Refactor workspace status computation to use instructions
