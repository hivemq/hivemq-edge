# Complete Fix: All Combiner Edges Now Operational-Status Aware

**Date:** October 26, 2025  
**Status:** ✅ Complete

---

## What Was Fixed

All edges connected to COMBINER nodes (both inbound and outbound) now correctly use the **combiner's operational status** for animation, ensuring consistent visual feedback about whether the combiner is configured.

### Edge Status Rules

| Edge Type            | Runtime Status From | Operational Status From | Animation Trigger               |
| -------------------- | ------------------- | ----------------------- | ------------------------------- |
| ADAPTER → COMBINER   | Adapter             | **Combiner**            | Combiner has mappings           |
| BRIDGE → COMBINER    | Bridge              | **Combiner**            | Combiner has mappings           |
| PULSE → ASSET_MAPPER | Pulse               | **Asset Mapper**        | Mapper has valid asset mappings |
| **COMBINER → EDGE**  | **Combiner**        | **Combiner**            | **Combiner has mappings**       |

### The Key Principle

**COMBINER's operational status controls animation for ALL its edges** (both incoming and outgoing), because:

1. The combiner knows if it's configured (has mappings)
2. Both inbound and outbound data flow depends on this configuration
3. Visual consistency: if combiner has mappings, ALL its edges should animate

---

## Code Changes

### File: `status-utils.ts`

Added three explicit handlers:

#### 1. ADAPTER → COMBINER (lines ~432-460)

```typescript
if (target?.type === NodeTypes.COMBINER_NODE) {
  const edgeStatusModel = {
    runtime: adapterStatusModel.runtime,
    operational: targetCombiner.statusModel.operational, // ← KEY
  }
}
```

#### 2. BRIDGE → COMBINER (lines ~483-511)

```typescript
if (target?.type === NodeTypes.COMBINER_NODE) {
  const edgeStatusModel = {
    runtime: bridgeStatusModel.runtime,
    operational: targetCombiner.statusModel.operational, // ← KEY
  }
}
```

#### 3. COMBINER → \* (Outbound) (lines ~562-571)

```typescript
if (source.type === NodeTypes.COMBINER_NODE) {
  // Use combiner's own statusModel
  // - Runtime: derived from upstream
  // - Operational: has mappings?
  return getEdgeStatusFromModel(combinerStatusModel, true, theme)
}
```

---

## How It Works

### Data Flow

```
1. COMBINER computes operational status
   ↓
   const hasMappings = data.mappings.items.length > 0
   const operational = hasMappings ? ACTIVE : INACTIVE

2. COMBINER stores in statusModel
   ↓
   updateNodeData(id, { statusModel })

3. Edge rendering reads statusModel
   ↓
   For inbound edges: use target's operational status
   For outbound edges: use source's operational status

4. Animation applied
   ↓
   edge.animated = operational === ACTIVE && runtime === ACTIVE
```

### Visual Result

```
SOURCE (ACTIVE)
  ↓
  ├─> COMBINER 1 (no mappings) ──> EDGE
  │   Both edges NOT ANIMATED ✅
  │
  └─> COMBINER 2 (has mappings) ──> EDGE
      Both edges ANIMATED ✅
```

---

## Why This Matters

### User Perspective

Users can now **immediately see** which combiners are:

- ✅ **Fully configured** (animated edges = has mappings)
- ❌ **Not configured** (no animation = no mappings)

### Consistency

All edges connected to a combiner show the **same animation state**, making it clear that the combiner itself is the configuration bottleneck, not the source or destination.

---

## Testing Validation

### Scenario 1: Combiner Without Mappings

- Create adapter (ACTIVE)
- Create combiner connected to adapter
- Don't add any mappings
- **Expected:** Both ADAPTER→COMBINER and COMBINER→EDGE edges are GREEN but NOT ANIMATED ✅

### Scenario 2: Combiner With Mappings

- Create adapter (ACTIVE)
- Create combiner connected to adapter
- Add a mapping (TAG → topic)
- **Expected:** Both ADAPTER→COMBINER and COMBINER→EDGE edges are GREEN and ANIMATED ✅

### Scenario 3: Multiple Sources

- Create adapter and bridge (both ACTIVE)
- Create combiner connected to both
- Add mappings to combiner
- **Expected:** All three edges (adapter→combiner, bridge→combiner, combiner→edge) are ANIMATED ✅

---

## Technical Details

### Combiner Status Computation

**File:** `NodeCombiner.tsx`

```typescript
const statusModel = useMemo(() => {
  const hasMappings = data.mappings.items.length > 0
  const operational = hasMappings ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE

  // Runtime derives from upstream sources
  const runtime = deriveFromUpstream(connectedNodes)

  return { runtime, operational, source: 'DERIVED' }
}, [connectedNodes, data.mappings.items.length])
```

### Edge Animation Logic

**File:** `status-utils.ts` → `getEdgeStatusFromModel()`

```typescript
edge.animated =
  forceAnimation ??
  (statusModel.operational === OperationalStatus.ACTIVE && statusModel.runtime === RuntimeStatus.ACTIVE)
```

**Animation requires BOTH:**

- Operational = ACTIVE (has mappings)
- Runtime = ACTIVE (source is active)

---

## Integration Status

✅ ADAPTER → COMBINER (inbound)  
✅ BRIDGE → COMBINER (inbound)  
✅ PULSE → ASSET_MAPPER (inbound)  
✅ COMBINER → EDGE (outbound)  
✅ COMBINER → other targets (outbound)

All combiner edges now use consistent operational status! 🎉

---

## Files Modified

1. **`status-utils.ts`**

   - Added explicit ADAPTER → COMBINER handler (~28 lines)
   - Added explicit BRIDGE → COMBINER handler (~28 lines)
   - Added explicit COMBINER outbound handler (~11 lines)
   - Total: ~67 new lines

2. **Documentation**
   - Updated `FIX_COMBINER_EDGE_ANIMATION.md`
   - Created this summary document

---

## Summary

The fix ensures **all edges connected to a combiner use the combiner's operational status**, providing:

1. **Visual Consistency** - All combiner edges behave the same way
2. **Clear Feedback** - Animation shows configuration status
3. **Correct Logic** - Combiner controls its own operational state
4. **Pattern Alignment** - Follows the same approach as Pulse/Asset Mapper

**Both inbound and outbound edges from/to combiners are now animated if and only if the combiner has mappings configured.** ✅
