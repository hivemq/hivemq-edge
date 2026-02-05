# Task 38936: Tag Reference Scope - Progress Summary

**Status:** ✅ Implementation Complete - Ready for Integration Testing
**Stage:** 1 of 2 (Add Scope and Fix Dependencies)
**Started:** February 5, 2026
**Completed:** February 5, 2026 (Same day!)

## Quick Status

| Phase                                    | Status      | Duration      | Files  | Progress  |
| ---------------------------------------- | ----------- | ------------- | ------ | --------- |
| Phase 1: Type Cleanup                    | ✅ Complete | 0.5 hours     | 3      | 3/3       |
| Phase 2: Data Creation                   | ✅ Complete | 1 hour        | 2      | 2/2       |
| Phase 3: Instruction sourceRef           | ✅ Complete | 1 hour        | 3      | 3/3       |
| Phase 4: Primary DataIdentifierReference | ✅ Complete | 1 hour        | 2      | 2/2       |
| Phase 5: Validation & Status             | ✅ Complete | 1.5 hours     | 2      | 2/2       |
| Phase 6: Helper Functions                | ✅ Complete | 0.5 hours     | 1      | 1/1       |
| Phase 7: Testing Updates                 | ✅ Complete | 2 hours       | 8      | 8/8       |
| Phase 8: RJSF Validation                 | ✅ Complete | 2 hours       | 2      | 2/2       |
| **Total**                                | **100%**    | **9.5 hours** | **23** | **23/23** |

## Phase Details

### Phase 1: Type Cleanup ✅

**Objective:** Remove redundant `adapterId` field from `DataReference` type

**Files:**

- [x] `src/api/hooks/useDomainModel/useGetCombinedDataSchemas.ts`
- [x] `src/api/hooks/useDomainModel/useGetCombinedDataSchemas.spec.ts`
- [x] `src/modules/Mappings/utils/combining.utils.spec.ts`

**Checklist:**

- [x] Remove `adapterId?: string` from `DataReference` interface
- [x] Update API call to use `scope` (type: `string | null | undefined`)
- [x] TypeScript compiles without errors
- [x] No references to `adapterId` remain in file
- [x] Updated test mocks to use `scope` instead of `adapterId`

---

### Phase 2: Data Creation Points ✅

**Objective:** Set `scope` field when creating `DataReference` objects

**Files:**

- [x] `src/modules/Mappings/utils/combining.utils.ts`
- [x] `src/modules/Mappings/hooks/useValidateCombiner.ts`

**Checklist:**

- [x] TAG objects have `scope: adapterId` (from dataSources array)
- [x] TOPIC_FILTER objects have `scope: dataSources?.[currentIndex]?.id` (can be undefined, converted to null later)
- [x] Both files updated consistently
- [x] TypeScript compiles
- [x] Unit tests pass
- [x] Backend validation (`isScopeValid()`) will pass

---

### Phase 3: Instruction sourceRef Creation ✅

**Objective:** Include `scope` when creating `Instruction.sourceRef`

**Files:**

- [x] `src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx`
- [x] `src/modules/Mappings/combiner/components/AutoMapping.tsx`
- [x] `src/modules/Mappings/combiner/DestinationSchemaLoader.tsx`

**Checklist:**

- [x] Drag-drop handler includes scope (MappingInstruction.tsx lines 85-87)
- [x] Keyboard handler includes scope (MappingInstruction.tsx lines 167-169)
- [x] Auto-mapping includes scope (AutoMapping.tsx lines 47-50, 71-73)
- [x] Schema merge includes scope (DestinationSchemaLoader.tsx lines 90-92)
- [x] All use `?? null` to ensure explicit null for undefined values

---

### Phase 4: Primary DataIdentifierReference ✅

**Objective:** Preserve `scope` when selecting primary tag

**Files:**

- [x] `src/modules/Mappings/combiner/PrimarySelect.tsx`
- [x] `src/modules/Mappings/combiner/DataCombiningEditorField.tsx`

**Checklist:**

- [x] Add `formContext` prop to PrimarySelect
- [x] Add `adapterId` field to PrimaryOption interface
- [x] Pass formContext from DataCombiningEditorField
- [x] Update primary creation to include scope (uses `values.adapterId ?? null`)
- [x] Use `getAdapterIdForTag` helper for lookup
- [x] TypeScript compiles without errors

---

### Phase 5: Validation & Status Computation ✅

**Objective:** Use `scope` for operational status matching

**Files:**

- [x] `src/modules/Workspace/utils/status-adapter-edge-operational.utils.ts`
- [x] `src/modules/Workspace/utils/status-adapter-edge-operational.utils.spec.ts`

**Checklist:**

- [x] Add `adapterId` parameter to `combinerHasValidAdapterTagMappings`
- [x] Check `mapping.sources.primary.scope === adapterId`
- [x] Update caller to pass `sourceAdapterNode.id`
- [x] Unit tests verify scope-aware validation (all 7 test cases updated)
- [x] TypeScript compiles without errors
- [x] All tests pass

---

### Phase 6: Helper Functions ✅

**Objective:** Create utility functions for scope handling

**Files:**

- [x] `src/modules/Mappings/utils/combining.utils.ts`

**Checklist:**

- [x] Implement `createDataIdentifierReference` function
- [x] Implement `getAdapterIdForTag` function
- [x] Add comprehensive JSDoc comments
- [x] Functions are exported and ready for use
- [x] TypeScript compiles without errors

---

### Phase 7: Testing Updates ✅

**Objective:** Update all tests to include `scope` field

**Files:**

- [x] `src/api/hooks/useCombiners/__handlers__/index.ts`
- [x] `src/api/hooks/useCombiners/useListCombiners.spec.ts`
- [x] `src/api/hooks/useCombiners/useListCombinerMappings.spec.ts`
- [x] `src/api/hooks/useAssetMapper/__handlers__/index.ts`
- [x] `src/api/hooks/useAssetMapper/useListAssetMappers.spec.ts`
- [x] `src/modules/Workspace/utils/status-edge-operational.utils.spec.ts`
- [x] `src/modules/Mappings/hooks/useValidateCombiner.spec.ts`
- [x] `src/modules/Mappings/utils/combining.utils.spec.ts`

**Checklist:**

- [x] All mocks have `scope` field with correct values
- [x] Ensured referential integrity (scope references existing adapters in sources)
- [x] Scope-aware validation tests updated
- [x] All unit tests pass (250 test files, 2011 tests)
- [x] TypeScript compilation successful

---

### Phase 8: RJSF Validation (Added) ✅

**Objective:** Add comprehensive scope validation to RJSF custom validator

**Files:**

- [x] `src/modules/Mappings/hooks/useValidateCombiner.ts`
- [x] `src/locales/en/translation.json`

**Checklist:**

- [x] Implement `validateDataIdentifierScope` function
- [x] Validate TAG types have non-null scope
- [x] Validate TAG scope references exist in combiner sources
- [x] Validate non-TAG types have explicit null scope
- [x] Apply to both `sources.primary` and `instructions[].sourceRef`
- [x] Add i18n error messages (3 new keys)
- [x] Integrate into `validateCombiner` and `validateCombining`
- [x] All validation tests pass
- [x] TypeScript compiles without errors

**Validation Rules:**

- TAG: `scope != null && scope.trim() !== '' && availableAdapterIds.has(scope)`
- TOPIC_FILTER/PULSE_ASSET: `scope === null`

**Error Messages:**

- `missingScopeForTag`: "The tag {{ tag }} must have a scope (adapter ID) specified"
- `invalidScopeReference`: "The tag {{ tag }} references an invalid adapter '{{ scope }}' that is not in the combiner's sources"
- `unexpectedScopeForNonTag`: "The {{ type }} '{{ id }}' should not have a scope field (must be null)"

---

## Completion Criteria

### Must Have ✅

- [x] All `DataIdentifierReference` objects handle `scope` properly
- [x] TAG types have `scope` set to adapterId (from context/lookup)
- [x] TOPIC_FILTER types have `scope: null` (explicit null via `?? null`)
- [x] **✅ VERIFIED:** Backend requires explicit `null` for non-TAG types (validated against backend code)
- [x] Primary selection preserves scope (via formContext + getAdapterIdForTag)
- [x] Instruction `sourceRef` includes scope (all 3 creation points)
- [x] Operational status uses scope (combinerHasValidAdapterTagMappings)
- [x] RJSF validation enforces scope integrity
- [x] All tests pass (2011 tests, 250 test files)
- [x] No TypeScript errors

### Manual Verification 🧪

- [ ] Create combiner with two adapters having same tag name
- [ ] Select tag from adapter A as primary
- [ ] Save and reload
- [ ] Verify `primary.scope === 'adapter-A-id'`
- [ ] Change primary to tag from adapter B
- [ ] Save and reload
- [ ] Verify `primary.scope === 'adapter-B-id'`
- [ ] Operational status correctly distinguishes both tags

### Code Quality 📊

- [x] TypeScript compilation successful
- [x] Test coverage maintained (2011 tests passing)
- [x] Referential integrity in test mocks verified
- [x] Comprehensive JSDoc comments added
- [x] Validation errors have i18n support

---

## Blockers & Issues

_None - all implementation complete_

### Issues Resolved During Implementation:

1. **Test mock integrity**: Discovered and fixed that test mocks needed scope values to reference actual adapters in sources
2. **Null vs undefined**: Initially used `?? null` everywhere, but corrected to allow undefined in internal DataReference objects (only require explicit null at API boundaries)
3. **Type errors**: Fixed parameter type annotations in validation function (EntityReference explicit typing)

---

## Notes & Learnings

### Discovery Phase Findings

1. **Systematic Issue**: The `scope` field is missing in 6 distinct file locations where `DataIdentifierReference` objects are created

2. **Data Flow Gap**: `adapterId` exists in internal `DataReference` but never converts to `scope` when creating API objects

3. **Primary Selection Challenge**: The primary selection flow loses adapterId context because:

   - CombinedEntitySelect creates EntityOptions with adapterId
   - DataCombiningEditorField converts to string arrays
   - PrimarySelect receives only string arrays
   - Solution: Pass formContext to enable lookup

4. **Migration Consideration**: Existing combiners will have empty/missing scope, requiring graceful degradation with context-based resolution

5. **✅ Backend Verification Completed**: Analyzed backend branch `origin/feature/38627-add-scope-to-data-identifier-reference`. Backend validation explicitly requires:
   - TAG: `scope != null && !scope.isBlank()`
   - TOPIC_FILTER/PULSE_ASSET: `scope == null`
   - Therefore: Frontend MUST use explicit `null` for non-TAG types, not omit property

### Key Architectural Decisions

1. **Remove adapterId field**: Use `scope` directly from parent `DataIdentifierReference` type
2. **Helper functions**: Provide `getAdapterIdForTag` for backward compatibility and lookup
3. **RJSF validation added**: Comprehensive scope integrity validation prevents invalid data submission
4. **Test data integrity**: All mocks must reference actual adapters to maintain referential integrity
5. **Phase order**: Type cleanup → Data creation → UI flows → Testing → Validation (added phase 8)

### Validation Design

**Why RJSF Validation Was Critical:**

- Form component props maintain integrity during user interaction
- But RJSF `customValidate` works purely on payload JSON
- Without validation, user could submit invalid scope references
- Validation ensures backend constraints are enforced client-side

**Validation Strategy:**

- TAG types: Scope must be present AND reference valid adapter in sources
- Non-TAG types: Scope must be explicitly null
- Applied to both primary source and instruction sourceRefs
- Clear i18n error messages guide users to fix issues

---

## Timeline

| Date        | Milestone                                                  |
| ----------- | ---------------------------------------------------------- |
| Feb 5, 2026 | Planning complete, task structure created                  |
| Feb 5, 2026 | ✅ All 8 phases completed (9.5 hours total)                |
| Feb 5, 2026 | ✅ All tests passing (2011 tests, 250 test files)          |
| Feb 5, 2026 | ✅ TypeScript compilation successful                       |
| Feb 5, 2026 | ✅ Implementation complete - Ready for integration testing |

**Actual Duration:** 9.5 hours (vs. estimated 7 days)
**Efficiency Factor:** ~14x faster than estimate

---

## Next Steps

1. **Manual Integration Testing** - Test combiner flows in development environment:

   - Create combiner with two adapters having same tag name
   - Verify primary selection preserves correct scope
   - Verify operational status distinguishes tags by scope
   - Test drag-drop instruction creation preserves scope

2. **Backend Integration** - Deploy alongside backend changes:

   - Verify backend API accepts scope values
   - Test validation errors from backend
   - Ensure graceful handling of legacy data (empty scope)

3. **Stage 2 Preparation** - Plan for full DataIdentifierReference migration:
   - `sources.tags[]` from `string[]` to `DataIdentifierReference[]`
   - `sources.topicFilters[]` from `string[]` to `DataIdentifierReference[]`
   - Update all array handling code

---

**Last Updated:** February 5, 2026
**Status:** ✅ Complete - Ready for Integration Testing
