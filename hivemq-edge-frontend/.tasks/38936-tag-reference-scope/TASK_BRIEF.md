# Task 38936: Tag Reference Scope Implementation

## Overview

**Status:** 📋 Planning Complete - Ready for Implementation (Stage 1)
**Type:** Bug Fix + Feature Enhancement
**Priority:** High
**Estimated Duration:** 5-7 days

## Context

The Data Combiner system has a critical ownership issue: tags lack proper association with their owning adapters. When multiple adapters define tags with the same name, the system cannot distinguish between them, leading to:

1. **Lost Connections**: Combiner mappings cannot be safely restored because tag-adapter relationships are not persisted
2. **Ambiguity**: Same tag name from different adapters causes confusion in UI and validation
3. **Broken Status Computation**: Operational status checks cannot determine which adapter a tag belongs to

## Recent Changes

The backend API has been updated to include a `scope` field in the `DataIdentifierReference` type:

```typescript
export type DataIdentifierReference = {
  id: string // The name of the tag or topic filter
  type: DataIdentifierReference.type // TAG | TOPIC_FILTER | PULSE_ASSET
  scope?: string | null // For TAG: adapter ID. For others: undefined/null
}
```

**Implementation Note:** We're using `undefined` (omit property) for non-TAG types as a working assumption. This may need to change to explicit `null` after backend integration testing validates the actual API contract.

**Commit**: `47abad8aa` - docs(OpenAPI): Add scope to DataIdentifierReference (#1368)
**Related**: `e5981fc66` - feat: Allow same tag among multiple protocol adapters (#1336)

## Problem Statement

### Frontend Issues

The frontend codebase has NOT been updated to properly use the new `scope` field:

1. **Type Mismatch**: Internal `DataReference` type uses `adapterId?: string` instead of `scope`
2. **Missing Population**: When creating `DataIdentifierReference` objects, only `id` and `type` are set, `scope` is omitted
3. **Data Loss**: Tag ownership information is lost during save/load cycles
4. **Validation Gap**: Status computation cannot use scope to distinguish tags

### Impact

- ❌ Primary `DataIdentifierReference` saved without scope
- ❌ Instruction `sourceRef` created without scope
- ❌ Combiner restore fails with ambiguous tags
- ❌ Operational status incorrectly computes adapter-combiner connections

## Objectives

### Stage 1: Add Scope and Fix Dependencies (Current Task)

1. **Type Cleanup**: Remove redundant `adapterId` field, use `scope` from `DataIdentifierReference`
2. **Data Creation**: Ensure `scope` is set whenever creating `DataReference` objects
3. **Conversion**: Include `scope` when converting `DataReference` → `DataIdentifierReference`
4. **Validation**: Update status computation to use `scope` for tag matching
5. **Testing**: Update all tests to include `scope` field

### Stage 2: Persist Full Ownership (Future Task)

- Change `sources.tags` from `string[]` to `DataIdentifierReference[]`
- Change `sources.topicFilters` from `string[]` to `DataIdentifierReference[]`
- _Requires backend API changes_

## Scope

### In Scope (Stage 1)

- ✅ Replace `adapterId` with `scope` in internal types
- ✅ Populate `scope` in all `DataIdentifierReference` creation points
- ✅ Update primary selection to preserve scope
- ✅ Update instruction `sourceRef` to include scope
- ✅ Fix operational status to use scope-based matching
- ✅ Add helper functions for scope handling
- ✅ Update all tests and mocks

### Out of Scope

- ❌ Changing `sources.tags`/`sources.topicFilters` to full objects (Stage 2)
- ❌ Backend API modifications
- ❌ Migration of existing production data (handled by graceful degradation)

## Success Criteria

- [ ] All `DataIdentifierReference` objects have `scope` field populated
- [ ] TAG types have non-empty scope (adapterId)
- [ ] TOPIC_FILTER types have empty scope
- [ ] Primary selection preserves scope through save/load cycle
- [ ] Instruction `sourceRef` includes scope
- [ ] Operational status uses scope for tag matching
- [ ] All existing tests pass with scope field added
- [ ] No TypeScript compilation errors
- [ ] Manual testing: Create combiner with same-named tags from 2 adapters, verify scope distinguishes them

## Dependencies

- OpenAPI schema generation (already complete)
- Backend API support for scope field (already complete)

## Risks

| Risk                               | Likelihood | Impact | Mitigation                                  |
| ---------------------------------- | ---------- | ------ | ------------------------------------------- |
| Existing data has empty scope      | High       | Medium | Graceful degradation + lookup helper        |
| PrimarySelect loses scope context  | Medium     | High   | Pass formContext, add adapterId lookup      |
| Tests break with scope requirement | High       | Low    | Update all mocks systematically             |
| Type safety gaps                   | Low        | Medium | Use strict type guards + runtime validation |

## Related Tasks

- `e5981fc66` - Allow same tag among multiple protocol adapters
- `32118-workspace-status` - Operational status computation

## References

- [TASK_PLAN.md](./TASK_PLAN.md) - Implementation plan with 7 phases
- [TASK_SUMMARY.md](./TASK_SUMMARY.md) - Progress tracking
- Backend PR #1368: Add scope to DataIdentifierReference
