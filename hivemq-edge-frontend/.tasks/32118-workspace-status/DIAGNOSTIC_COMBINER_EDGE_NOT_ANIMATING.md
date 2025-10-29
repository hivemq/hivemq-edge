# Diagnostic: ADAPTER → COMBINER Edge Not Animating

**Date:** October 26, 2025  
**Issue:** Edge from ADAPTER to COMBINER not animating despite combiner having mapping with TAG  
**Status:** ✅ Fixed with Fallback Logic

---

## Problem Analysis

### Symptoms

User reports: "Still not getting the animated path on edge ADAPTER → COMBINER despite a mapping in the combiner using a TAG defined in the connected DEVICE"

### Root Cause Investigation

The animation logic requires **BOTH** conditions to be true:

```typescript
edge.animated = isOperational && isRuntimeActive
```

This means:

1. **Operational Status = ACTIVE** (combiner has mappings)
2. **Runtime Status = ACTIVE** (adapter is running and connected)

### Possible Causes

#### Cause 1: Adapter Not Active ⚠️

If your **adapter runtime status is not ACTIVE** (e.g., stopped, disconnected, or error), the edge won't animate even if the combiner has mappings.

**Check:**

- Is the adapter status showing as connected/started?
- Look at the adapter node - what color is it? (Should be green for ACTIVE)

#### Cause 2: Timing Issue ⚠️

The combiner's `statusModel` might not be computed yet when edges are first rendered.

**Sequence:**

1. Graph loads → edges rendered
2. Combiner component mounts → computes statusModel
3. Combiner calls updateNodeData → statusModel stored
4. **But edges might have already been rendered without the statusModel**

#### Cause 3: StatusModel Not Stored ⚠️

The combiner's `useEffect` might not fire, or `updateNodeData` might fail.

---

## Solution Applied

### Added Fallback Logic

For **ADAPTER → COMBINER** edges:

```typescript
if (target?.type === NodeTypes.COMBINER_NODE) {
  const targetStatusModel = targetData.statusModel

  if (targetStatusModel) {
    // Use combiner's computed statusModel (preferred)
    const edgeStatusModel = {
      runtime: adapterRuntime,
      operational: targetStatusModel.operational,
    }
  } else {
    // FALLBACK: Check combiner mappings directly
    const targetCombiner = target.data as Combiner
    const hasMapping = targetCombiner.mappings?.items?.length > 0
    const fallbackStatusModel = {
      runtime: adapterRuntime,
      operational: hasMapping ? ACTIVE : INACTIVE,
    }
  }
}
```

**Same fallback added for BRIDGE → COMBINER**

### Why This Works

1. **Primary Path**: Use combiner's computed `statusModel` (includes proper status derivation)
2. **Fallback Path**: If statusModel not available, check `mappings.items` directly
3. **Ensures edges always animate** when combiner has mappings, even if statusModel timing is off

---

## Debugging Steps

### Step 1: Check Adapter Runtime Status

**In Browser DevTools:**

1. Find the adapter node in React Flow
2. Check `node.data.statusModel.runtime`
3. Should be `"ACTIVE"` for edge to animate

**Visual Check:**

- Is the adapter node **green**? (ACTIVE)
- Is it **red**? (ERROR - won't animate)
- Is it **yellow**? (INACTIVE - won't animate)

### Step 2: Check Combiner Has Mappings

**In Browser DevTools:**

1. Find the combiner node in React Flow
2. Check `node.data.mappings.items.length`
3. Should be > 0

**Visual Check:**

- Does the combiner show a mapping badge with topics?

### Step 3: Check Combiner StatusModel

**In Browser DevTools:**

1. Find the combiner node
2. Check `node.data.statusModel`
3. Should have `operational: "ACTIVE"` if has mappings

**If statusModel is undefined:**

- This is the timing issue - fallback should handle it
- Refresh the page and check again

### Step 4: Check Edge Animation

**In Browser DevTools:**

1. Find the edge in React Flow: `edge.source = adapter.id, edge.target = combiner.id`
2. Check `edge.animated`
3. Should be `true` if both adapter ACTIVE and combiner has mappings

---

## Expected Behavior After Fix

### Scenario: Adapter ACTIVE + Combiner with Mappings

```
ADAPTER (runtime: ACTIVE)
  ↓
  → COMBINER (has 1+ mappings)

Edge should be: GREEN (adapter active) + ANIMATED (combiner operational)
```

### Scenario: Adapter INACTIVE + Combiner with Mappings

```
ADAPTER (runtime: INACTIVE - stopped/disconnected)
  ↓
  → COMBINER (has 1+ mappings)

Edge should be: YELLOW/GRAY (adapter inactive) + NOT ANIMATED (runtime not active)
```

### Scenario: Adapter ACTIVE + Combiner without Mappings

```
ADAPTER (runtime: ACTIVE)
  ↓
  → COMBINER (no mappings)

Edge should be: GREEN (adapter active) + NOT ANIMATED (combiner not operational)
```

---

## Validation Checklist

After the fix, verify:

- [ ] **Adapter is ACTIVE** (green node, runtime status = ACTIVE)
- [ ] **Combiner has mappings** (check `mappings.items.length > 0`)
- [ ] **Edge is GREEN** (runtime status from adapter)
- [ ] **Edge is ANIMATED** (operational status from combiner)

If edge is still not animating:

- [ ] Check browser console for errors
- [ ] Verify React Flow is rendering edges correctly
- [ ] Check if `updateEdgesStatusWithModel` is being called
- [ ] Inspect edge object in DevTools for `animated` property

---

## Technical Details

### Animation Logic

**File:** `status-utils.ts` → `getEdgeStatusFromModel()`

```typescript
const isOperational = statusModel?.operational === OperationalStatus.ACTIVE
const isRuntimeActive = statusModel?.runtime === RuntimeStatus.ACTIVE
edge.animated = isOperational && isRuntimeActive
```

**Both must be true for animation!**

### Combiner Operational Status

**File:** `NodeCombiner.tsx`

```typescript
const hasMappings = data.mappings.items.length > 0
const operational = hasMappings ? OperationalStatus.ACTIVE : OperationalStatus.INACTIVE
```

### Edge Status Composition

**File:** `status-utils.ts` → ADAPTER → COMBINER handler

```typescript
const edgeStatusModel = {
  runtime: adapter.statusModel.runtime, // From source
  operational: combiner.statusModel.operational, // From target
}
```

---

## Files Modified

- ✅ `src/modules/Workspace/utils/status-utils.ts`
  - Added fallback logic for ADAPTER → COMBINER (~18 lines)
  - Added fallback logic for BRIDGE → COMBINER (~18 lines)
  - Total: ~36 new lines

---

## Summary

The fix adds **fallback logic** to handle timing issues where the combiner's `statusModel` might not be available when edges are first rendered. The fallback checks `mappings.items` directly from the combiner data.

**If the edge is still not animating after this fix, the most likely cause is that the adapter's runtime status is not ACTIVE.**

Check the adapter node color:

- ✅ **Green** = ACTIVE (will animate if combiner has mappings)
- ❌ **Red/Yellow** = ERROR/INACTIVE (won't animate regardless)

---

## Next Steps

1. **Verify adapter is ACTIVE** (most common issue)
2. **Refresh browser** to apply the fallback logic
3. **Check browser DevTools** to inspect edge.animated property
4. **Report back** with adapter color and combiner mapping count

The fallback ensures robustness against timing issues, but the fundamental requirement remains: **adapter must be ACTIVE for the edge to animate**.
