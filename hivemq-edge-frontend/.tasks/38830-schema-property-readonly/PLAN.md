# Task 38830: Schema Property Readonly - Implementation Plan

## Current State Analysis

### Type Definition
**Location:** `src/components/rjsf/MqttTransformation/utils/json-schema.utils.ts:39-46`

```typescript
export interface FlatJSONSchema7 extends Omit<JSONSchema7, 'required'> {
  path: string[]
  key: string
  arrayType?: string
  origin?: string
  metadata?: DataReference
  required?: boolean  // Already handled as per-property boolean
}
```

**Note:** The `readonly` property from JSON Schema is **not** currently included.

### Property Extraction
**Location:** `src/components/rjsf/MqttTransformation/utils/json-schema.utils.ts`

- `SCHEMA_SUPPORTED_PROPERTIES` (line ~26): Whitelist of properties to extract
- `getProperty()` (lines 48-107): Recursive function that flattens schema
- `getPropertyListFrom()` (lines 141-152): Entry point for schema flattening

**Current behavior:** `readonly` is not in `SCHEMA_SUPPORTED_PROPERTIES` and is filtered out.

### Rendering
**Location:** `src/components/rjsf/MqttTransformation/components/schema/PropertyItem.tsx`

Currently renders:
- Type icon (based on `property.type`)
- Property name/title
- Examples (optional)
- Description (optional)

**No readonly indicator exists.**

### Interaction Handling
**Location:** `src/components/rjsf/MqttTransformation/components/mapping/MappingInstruction.tsx`

- Uses `isMappingSupported()` to check if property can be a drop target
- Currently only checks `property.type !== 'object'`
- No readonly check exists

---

## Implementation Plan

### Phase 1: Type and Utility Changes

#### 1.1 Extend `FlatJSONSchema7` Type
**File:** `json-schema.utils.ts`

Add `readOnly?: boolean` field to the interface.

> **Note:** JSON Schema uses `readOnly` (camelCase), not `readonly` (lowercase).

#### 1.2 Update Property Extraction
**File:** `json-schema.utils.ts`

Option A: Add `'readOnly'` to `SCHEMA_SUPPORTED_PROPERTIES` array
Option B: Handle `readOnly` separately (like `required` is handled)

**Recommendation:** Option A is simpler and consistent with other validation properties.

#### 1.3 Add Unit Tests
**File:** `json-schema.utils.spec.ts`

- Test that `readOnly: true` is preserved in flattened properties
- Test nested objects with readonly children

### Phase 2: Visual Rendering

#### 2.1 Update PropertyItem Component
**File:** `PropertyItem.tsx`

Add visual indicator for readonly properties:
- **Icon:** Lock icon (`LuLock` from lucide-react, consistent with existing icon usage)
- **Tooltip:** Explain the readonly status
- **Placement:** Near the property name, similar to how type icons are shown

**Design considerations:**
- Minimal visual impact (as per requirements)
- Clear but not intrusive
- Consistent with existing metadata display patterns

#### 2.2 Update PropertyItem Tests
**File:** `PropertyItem.spec.cy.tsx`

- Test that readonly indicator is shown when `readOnly: true`
- Test that readonly indicator is hidden when `readOnly: false/undefined`
- Ensure accessibility for readonly indicator

### Phase 3: Interaction Prevention

#### 3.1 Create `isReadOnly` Utility
**File:** `data-type.utils.ts` (where `isMappingSupported` lives)

```typescript
export const isReadOnly = (property: FlatJSONSchema7): boolean => {
  return property.readOnly === true
}
```

#### 3.2 Update MappingInstruction Component
**File:** `MappingInstruction.tsx`

- Extend the "not supported" pattern to include readonly check
- Prevent drag-and-drop onto readonly properties
- Show appropriate message/status for readonly properties
- Disable clear/edit actions for readonly properties

**Possible approaches:**
1. Treat readonly similarly to unsupported (object) types
2. Show a different status badge (e.g., "Read Only" instead of "Not Supported")

#### 3.3 Update MappingInstruction Tests
**File:** `MappingInstruction.spec.cy.tsx`

- Test that readonly properties show appropriate status
- Test that drop zone is disabled for readonly properties
- Test that clear button is disabled/hidden for readonly properties

### Phase 4: Translation and Finalization

#### 4.1 Add i18n Keys
Add translation keys for:
- Readonly tooltip text
- Readonly status badge text
- Any error/info messages

#### 4.2 Final Review
- Ensure consistent behavior across Southbound and Combiner/Asset mapper
- Verify accessibility
- Update any documentation

---

## File Change Summary

| File | Changes |
|------|---------|
| `json-schema.utils.ts` | Add `readOnly` to type and extraction |
| `json-schema.utils.spec.ts` | Add unit tests for readOnly extraction |
| `data-type.utils.ts` | Add `isReadOnly` utility function |
| `PropertyItem.tsx` | Add readonly visual indicator |
| `PropertyItem.spec.cy.tsx` | Add tests for readonly rendering |
| `MappingInstruction.tsx` | Prevent interactions with readonly properties |
| `MappingInstruction.spec.cy.tsx` | Add tests for readonly behavior |
| Translation files | Add i18n keys for readonly labels |

---

## Open Questions

### Q1: Source vs Destination readonly behavior
Should readonly properties be:
- **Draggable as sources?** (Reading from a readonly property seems fine)
- **Droppable as destinations?** (Writing to a readonly property should be prevented)

**Assumption:** Readonly only affects destination behavior (drop targets), not source behavior (drag sources).

### Q2: MappingInstruction UI Pattern for Readonly

Looking at the current code, there are two distinct UI patterns:

**Pattern A: Unsupported (object types)** - `MappingInstruction.tsx:118-129`
```
┌─────────────────────────────────────────────┐
│ [Type Icon] property.name    ⚠ Not Supported│
└─────────────────────────────────────────────┘
```
- Minimal card with property and warning alert only
- No drop zone, no buttons, no interaction

**Pattern B: Supported (all other types)** - `MappingInstruction.tsx:131-210`
```
┌─────────────────────────────────────────────┐
│ [Type Icon] property.name                   │
├─────────────────────────────────────────────┤
│ ┌─DropZone ─────────────┐ [🗑] ✓ Matched    │
│ │ source.property.path  │     ✗ Required    │
│ └───────────────────────┘                   │
└─────────────────────────────────────────────┘
```
- Full card with drop zone, clear button, status alert

**Question:** Which pattern should readonly follow?

**Option 1:** Follow Pattern A (like unsupported)
- Show "Read Only" warning instead of "Not Supported"
- No drop zone, no interactions
- Clean and clear indication that this cannot be mapped

**Option 2:** Follow Pattern B but disabled
- Show the drop zone but visually disabled
- Show "Read Only" in the status alert area
- More complex but shows where mapping would go

**Recommendation:** Option 1 - follow the unsupported pattern. Reasoning:
- Consistent with existing "cannot be mapped" pattern
- Simpler implementation
- Clearer UX (no ambiguity about whether it's temporarily disabled)

### Q3: Existing mappings to readonly properties
If a property becomes readonly after a mapping was already created:
- Should the existing mapping be displayed with a warning?
- Should it be automatically removed?

**Assumption:** Display with a warning, let user decide. This requires additional handling.

### Q4: Visual indicator in PropertyItem
What icon and style for readonly indication in the PropertyItem component?
- Lock icon (`LuLock`) next to property name
- Different color/opacity for the badge
- Tooltip explaining readonly status

**Recommendation:** Lock icon with tooltip, placed after the property name badge.

### Q5: Status text consistency
Current patterns:
- "Not Supported" for object types (`rjsf.MqttTransformationField.validation.notSupported`)
- "Required" / "Matched" for mapping status

For readonly, should we use:
- "Read Only"
- "Not Mappable"
- Something else?

**Recommendation:** "Read Only" - descriptive and matches JSON Schema terminology.

---

## Design Decisions Log

| Date | Decision | Rationale |
|------|----------|-----------|
| 2026-01-22 | Use `readOnly` (camelCase) | JSON Schema specification uses camelCase |
| 2026-01-22 | Add to SCHEMA_SUPPORTED_PROPERTIES | Simplest approach, consistent with other constraints |
| 2026-01-22 | Lock icon for visual indicator | Universal symbol for readonly/locked state |
| 2026-01-22 | **Q1: Use "Unsupported" UI pattern** | Readonly properties show minimal card with "Read Only" warning, no drop zone. Consistent with existing pattern for unmappable properties |
| 2026-01-22 | **Q2: Sources not affected** | Readonly only prevents being a drop target (destination). Readonly properties can still be dragged as sources |
| 2026-01-22 | **Q3: Auto-remove + validation error** | If a property becomes readonly with an existing mapping: (1) UI auto-removes the mapping, (2) Validation triggers an error |

---

## Testing Strategy

1. **Unit tests** for utility functions (json-schema.utils.spec.ts)
2. **Component tests** for visual rendering (PropertyItem.spec.cy.tsx)
3. **Component tests** for interaction prevention (MappingInstruction.spec.cy.tsx)
4. **Accessibility tests** for all modified components
5. **Manual testing** in Southbound and Combiner/Asset mapper contexts

---

## Implementation Outcomes

### Completed Changes

| File | Change |
|------|--------|
| `json-schema.utils.ts:38` | Added `'readOnly'` to `SCHEMA_SUPPORTED_PROPERTIES` |
| `json-schema.utils.spec.ts` | Added 2 unit tests for readOnly extraction |
| `data-type.utils.ts:22-24` | Added `isReadOnly()` utility function |
| `PropertyItem.tsx` | Added lock icon with tooltip for readonly properties |
| `PropertyItem.spec.cy.tsx` | Added 4 tests for readonly indicator |
| `MappingInstruction.tsx` | Added readonly UI pattern (info alert, no drop zone) |
| `MappingInstruction.spec.cy.tsx` | Added 2 tests for readonly behavior |
| `MappingInstructionList.tsx` | Added auto-removal logic for readonly mappings |
| `components.json` | Added i18n keys for readonly labels |
| `schema.mocks.ts` | Added `MOCK_MQTT_SCHEMA_READONLY` test fixture |

### Test Results

- **Unit tests**: 26 passing (json-schema.utils.spec.ts)
- **Component tests**: 29 passing (all MqttTransformation components)
- **TypeScript build**: Passing
- **ESLint**: Passing

### Key Implementation Details

1. **Type System**: `readOnly` is inherited from `JSONSchema7` type via the `FlatJSONSchema7` interface. Adding it to `SCHEMA_SUPPORTED_PROPERTIES` ensures it's preserved during schema flattening.

2. **Visual Indicator**: Lock icon (`LuLock`) with tooltip appears after the property name badge in `PropertyItem`.

3. **MappingInstruction Pattern**: Readonly properties follow the same UI pattern as unsupported (object) types - a minimal card with an info alert showing "Read-only".

4. **Auto-removal**: `MappingInstructionList` uses a `useEffect` to detect and remove any instructions targeting readonly properties, then calls `onChange` with the cleaned list.
