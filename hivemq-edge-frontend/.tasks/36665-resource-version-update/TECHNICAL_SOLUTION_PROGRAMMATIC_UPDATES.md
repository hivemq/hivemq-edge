# Technical Solution: Programmatic Updates Pattern

**Task**: 36665-resource-version-update  
**Date**: November 6, 2025  
**Status**: Implemented ✅

## Problem Statement

When implementing inter-field dependencies in RJSF forms (SchemaPanel, FunctionPanel), programmatic updates to form data were causing race conditions:

### Symptoms

1. **Double-click required**: Changing a field (e.g., type) after creating a new resource required clicking twice for the change to take effect
2. **First-time failure**: Creating a new resource name and immediately changing another field (type/schema) would fail on the first attempt
3. **Inconsistent behavior**: After the initial failure, subsequent changes would work correctly

### Root Cause

The `isProgrammaticUpdateRef` flag pattern was blocking user interactions that happened immediately after programmatic state updates:

```typescript
// BEFORE (Broken Pattern)
if (id?.includes('name')) {
  isProgrammaticUpdateRef.current = true  // Set flag
  setFormData({...})                      // Trigger programmatic update
  return                                   // Exit
}
// Flag remains TRUE until the next onChange event
// If user clicks immediately, their change is blocked!
```

**Timeline of the bug:**

1. User changes name → `isProgrammaticUpdateRef.current = true`
2. `setFormData()` queues React state update
3. User **immediately** changes type (before React processes the update)
4. Type onChange fires → flag is **still true** → change is **blocked** ❌
5. React processes name update → onChange fires → flag reset to `false`
6. User clicks type again → now it works ✅

## Solution: `queueMicrotask()` Pattern

### Implementation

```typescript
const onReactFlowSchemaFormChange = useCallback(
  (changeEvent: IChangeEvent, id?: string | undefined) => {
    // Guard: Ignore onChange events triggered by programmatic updates
    if (isProgrammaticUpdateRef.current) {
      isProgrammaticUpdateRef.current = false
      return
    }

    // Handle field changes with programmatic coordination
    if (id?.includes('name')) {
      isProgrammaticUpdateRef.current = true
      setFormData({
        // ... programmatic update
      })
      // Reset flag after React processes the state update
      queueMicrotask(() => {
        isProgrammaticUpdateRef.current = false
      })
      return
    }

    // Same pattern for type, version, schemaSource changes...
  },
  [dependencies]
)
```

### Why `queueMicrotask()` Works

JavaScript event loop phases:

```
┌─────────────────────────────┐
│   1. Current execution      │  ← Your code runs here
└─────────────────────────────┘
              ↓
┌─────────────────────────────┐
│   2. Microtask queue        │  ← queueMicrotask(), Promise.then()
└─────────────────────────────┘
              ↓
┌─────────────────────────────┐
│   3. Render (React updates) │  ← React batches state updates
└─────────────────────────────┘
              ↓
┌─────────────────────────────┐
│   4. Task queue (macrotask) │  ← setTimeout(), setInterval()
└─────────────────────────────┘
```

**Fixed Timeline:**

1. User changes name → `isProgrammaticUpdateRef.current = true`
2. `setFormData()` queues React state update
3. `queueMicrotask()` schedules flag reset
4. Current execution completes
5. **Microtask runs** → `isProgrammaticUpdateRef.current = false` ✅
6. React processes name's state update → onChange fires → flag is `false` → blocked correctly ✅
7. User clicks type → flag is **already false** → change works immediately! ✅

### Key Benefits

1. **Blocks programmatic onChange**: The flag is `true` when `setFormData()` triggers its onChange
2. **Ready for user interaction**: Flag is reset before the user can physically click again
3. **Minimal delay**: Microtasks run immediately after current execution, not waiting for renders
4. **Deterministic**: Unlike `setTimeout(0)`, microtasks always run at the same event loop phase

## Applied Pattern in SchemaPanel

### Business Logic Requirements

1. **Name change** → Load all properties from existing schema OR create new draft
2. **Type change** → Update schema source, version stays DRAFT if already DRAFT, otherwise MODIFIED
3. **Version change** → Load specific version's schema, set status to LOADED
4. **SchemaSource change** → Version stays DRAFT if already DRAFT, otherwise MODIFIED

### Implementation

```typescript
// Handle name change - load schema or create new draft
if (id?.includes('name')) {
  const schema = allSchemas?.items?.findLast((schema) => schema.id === changeEvent.formData.name)
  if (schema) {
    isProgrammaticUpdateRef.current = true
    setFormData({
      name: schema.id,
      type: enumFromStringValue(SchemaType, schema.type) || SchemaType.JSON,
      version: schema.version || ResourceWorkingVersion.MODIFIED,
      schemaSource: atob(schema.schemaDefinition),
      internalVersions: getSchemaFamilies(allSchemas?.items || [])[schema.id].versions,
      internalStatus: ResourceStatus.LOADED,
    })
    queueMicrotask(() => {
      isProgrammaticUpdateRef.current = false
    })
  } else {
    isProgrammaticUpdateRef.current = true
    setFormData({
      internalStatus: ResourceStatus.DRAFT,
      name: changeEvent.formData.name,
      type: SchemaType.JSON,
      version: ResourceWorkingVersion.DRAFT,
      schemaSource: MOCK_JSONSCHEMA_SCHEMA,
    })
    queueMicrotask(() => {
      isProgrammaticUpdateRef.current = false
    })
  }
  return
}

// Handle type change - preserve DRAFT status
if (id?.includes('type') && formData) {
  if (formData.type !== changeEvent.formData.type) {
    isProgrammaticUpdateRef.current = true
    setFormData({
      ...formData,
      type: changeEvent.formData.type,
      schemaSource: changeEvent.formData.type === SchemaType.JSON ? MOCK_JSONSCHEMA_SCHEMA : MOCK_PROTOBUF_SCHEMA,
      version:
        formData.version === ResourceWorkingVersion.DRAFT
          ? ResourceWorkingVersion.DRAFT
          : ResourceWorkingVersion.MODIFIED,
      internalStatus: formData.internalStatus === ResourceStatus.DRAFT ? ResourceStatus.DRAFT : ResourceStatus.MODIFIED,
    })
    queueMicrotask(() => {
      isProgrammaticUpdateRef.current = false
    })
  }
  return
}

// Handle version change - load specific version
if (id?.includes('version') && formData) {
  const schema = allSchemas?.items?.find(
    (schema) => schema.id === formData.name && schema.version?.toString() === changeEvent.formData.version.toString()
  )
  if (schema) {
    isProgrammaticUpdateRef.current = true
    setFormData({
      ...formData,
      version: changeEvent.formData.version,
      schemaSource: atob(schema.schemaDefinition),
      internalStatus: ResourceStatus.LOADED,
    })
    queueMicrotask(() => {
      isProgrammaticUpdateRef.current = false
    })
  }
  return
}

// Handle schemaSource change - preserve DRAFT status
if (id?.includes('schemaSource') && formData) {
  if (formData.internalStatus === ResourceStatus.LOADED || formData.internalStatus === ResourceStatus.DRAFT) {
    isProgrammaticUpdateRef.current = true
    setFormData({
      ...formData,
      schemaSource: changeEvent.formData.schemaSource,
      version:
        formData.version === ResourceWorkingVersion.DRAFT
          ? ResourceWorkingVersion.DRAFT
          : ResourceWorkingVersion.MODIFIED,
      internalStatus: formData.internalStatus === ResourceStatus.DRAFT ? ResourceStatus.DRAFT : ResourceStatus.MODIFIED,
    })
    queueMicrotask(() => {
      isProgrammaticUpdateRef.current = false
    })
  }
  return
}
```

## Pattern Requirements

### Essential Elements

1. **Guard at the top**: Check flag and reset early

   ```typescript
   if (isProgrammaticUpdateRef.current) {
     isProgrammaticUpdateRef.current = false
     return
   }
   ```

2. **Set flag before programmatic update**

   ```typescript
   isProgrammaticUpdateRef.current = true
   setFormData({...})
   ```

3. **Reset flag in microtask**

   ```typescript
   queueMicrotask(() => {
     isProgrammaticUpdateRef.current = false
   })
   ```

4. **Early return after each condition**
   ```typescript
   if (condition) {
     // handle field
     return // ← CRITICAL: Prevents cascade effects
   }
   ```

### Common Pitfalls to Avoid

❌ **Missing `queueMicrotask`**: Flag stays true, blocks next user interaction
❌ **Missing `return` statements**: Multiple conditions process in same onChange
❌ **Wrong reset timing**: Resetting flag synchronously defeats the purpose
❌ **Using `setTimeout(0)`**: Slower than microtasks, may cause visible delay

## Architecture Review

### Current Architecture: ✅ GOOD

**Pattern**: Widgets are presentational, Panel handles business logic coordination

```
┌─────────────────────────────────────────┐
│           SchemaPanel                    │
│  ┌────────────────────────────────────┐ │
│  │ Business Logic (onChange handler)  │ │
│  │  - Name → Load/Create             │ │
│  │  - Type → Update schema template   │ │
│  │  - Version → Load version         │ │
│  │  - Source → Mark modified         │ │
│  └────────────────────────────────────┘ │
│              ↓                           │
│  ┌────────────────────────────────────┐ │
│  │    ReactFlowSchemaForm (RJSF)     │ │
│  └────────────────────────────────────┘ │
│              ↓                           │
│  ┌─────────────────┐  ┌──────────────┐ │
│  │ Name Widget     │  │ Type Widget  │ │
│  │ (Presentational)│  │ (Simple UI)  │ │
│  └─────────────────┘  └──────────────┘ │
└─────────────────────────────────────────┘
```

### Why This is Good

✅ **Separation of Concerns**: Widgets handle UI, Panel handles coordination  
✅ **RJSF-compliant**: Using RJSF as designed - widgets are simple  
✅ **Reusable**: Widgets (`VersionManagerSelect`, `SchemaNameCreatableSelect`) work in other forms  
✅ **Testable**: Business logic isolated in Panel  
✅ **Consistent**: Same pattern used in `FunctionPanel`

### Alternative Considered: Custom Field Template ❌

**Rejected Approach**: Single widget managing all inter-field dependencies

```typescript
// NOT RECOMMENDED
<ResourceManagerField
  resourceType="schema"
  allResources={allSchemas}
  onResourceChange={handleChange}
/>
```

**Why Rejected:**

- ❌ **Against RJSF philosophy**: Widgets should be simple, not orchestrators
- ❌ **Less reusable**: Tightly couples name/version/type/source logic
- ❌ **Harder to test**: Complex widget with internal state
- ❌ **Breaks consistency**: Different from existing `FunctionPanel` pattern

## Recommended Improvements

### 1. Extract to Reusable Hook (Medium effort, High value) 🎯

Create `useResourceFormCoordination` to share pattern across panels:

```typescript
// useResourceFormCoordination.ts
export const useResourceFormCoordination = <T extends ResourceData>(
  allResources: T[],
  formData: T | null,
  setFormData: (data: T) => void,
  config: {
    loadResource: (name: string) => T | undefined
    createDraft: (name: string) => T
    handleTypeChange: (current: T, newType: string) => T
    handleVersionChange: (current: T, version: number) => T
    handleSourceChange: (current: T, source: string) => T
  }
) => {
  const isProgrammaticUpdateRef = useRef(false)

  const onChange = useCallback(
    (changeEvent: IChangeEvent, id?: string) => {
      if (isProgrammaticUpdateRef.current) {
        isProgrammaticUpdateRef.current = false
        return
      }

      // Centralized coordination logic with proper typing
      // ... implementation
    },
    [allResources, formData, config]
  )

  return { onChange }
}
```

**Benefits:**

- Reusable between `SchemaPanel`, `FunctionPanel`, etc.
- Encapsulates race condition management
- Better TypeScript support
- Unit testable in isolation

### 2. Improve Field Identification (Low effort, Medium value)

```typescript
// Instead of: id?.includes('name')
// Use more explicit approach:
const fieldName = id?.split('_').pop() // Extract from 'root_name'

switch (fieldName) {
  case 'name':
    // handle name change
    break
  case 'type':
    // handle type change
    break
  case 'version':
    // handle version change
    break
  case 'schemaSource':
    // handle source change
    break
}
```

**Benefits:**

- Type-safe field identification
- Easier to maintain
- Less fragile than string includes

### 3. Add Comprehensive Documentation (Low effort, High value)

Document the coordination flow in code comments for future maintainers.

## Files Modified

- `src/extensions/datahub/designer/schema/SchemaPanel.tsx`

  - Added `queueMicrotask()` pattern to all programmatic updates
  - Fixed type change logic to preserve DRAFT status
  - Fixed version change to set LOADED status
  - Fixed schemaSource change to handle both LOADED and DRAFT states

- `src/extensions/datahub/designer/script/FunctionPanel.tsx`
  - Reference implementation (already had correct pattern without queueMicrotask)
  - Should be updated to use queueMicrotask for consistency

## Testing Notes

### Manual Testing Checklist

- [x] Create new schema name → immediately change type → works on first click
- [x] Load existing schema → change type → preserves proper version status
- [x] Change version → loads correct schema source
- [x] Edit schema source → marks as MODIFIED (if LOADED) or stays DRAFT
- [x] Rapid field changes → no double-click required
- [x] Type change from JSON → Protobuf → correct template loaded
- [x] Type change from Protobuf → JSON → correct template loaded

### Edge Cases Covered

- ✅ Creating new draft and immediately changing fields
- ✅ Loading existing resource and modifying
- ✅ Switching between versions
- ✅ Rapid consecutive field changes
- ✅ DRAFT status preservation across type/source changes
- ✅ MODIFIED status when editing LOADED resources

## Conclusion

The `queueMicrotask()` pattern successfully resolves race conditions in RJSF form coordination. The current architecture is sound and follows RJSF best practices. Future improvements should focus on extraction to a reusable hook rather than architectural changes.

## References

- **Related Task**: `.tasks/36665-resource-version-update/`
- **Similar Implementation**: `src/extensions/datahub/designer/script/FunctionPanel.tsx`
- **RJSF Documentation**: https://rjsf-team.github.io/react-jsonschema-form/
- **Event Loop Reference**: https://developer.mozilla.org/en-US/docs/Web/API/HTML_DOM_API/Microtask_guide
