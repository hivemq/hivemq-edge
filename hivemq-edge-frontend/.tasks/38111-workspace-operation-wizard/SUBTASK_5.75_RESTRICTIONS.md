# Wizard Restrictions & Lifecycle Management

**Subtask:** 5¾  
**Date:** November 10, 2025  
**Status:** Planning → Implementation

---

## Overview

When a wizard is active, the workspace must enter a "focused mode" where only wizard-related interactions are allowed. This prevents conflicts, confusion, and data integrity issues.

---

## Core Principle

**"Wizard Mode" = Exclusive Focus**

When `isActive === true`:

- ✅ Wizard controls work (progress bar, cancel, ghost nodes)
- ❌ Regular workspace operations disabled
- ❌ Node editing disabled
- ❌ Navigation/selection disabled

---

## 1. Trigger Button State Management

### Requirement

Once a wizard has started, the "Create New" button should be **disabled**.

### Rationale

- Prevent nested wizards
- Avoid state conflicts
- Clear UX: one task at a time

### Implementation

```tsx
// In CreateEntityButton.tsx
const { isActive } = useWizardState()

<MenuButton
  isDisabled={isActive}
  // ... other props
>
```

### Visual Feedback

- Button grayed out
- Tooltip: "Complete or cancel current wizard first"
- Cursor: not-allowed

---

## 2. Workspace Unmount Cleanup

### Requirement

When the workspace unmounts (user navigates away), cancel and reset the wizard.

### Rationale

- Prevent orphaned wizard state
- Clean navigation experience
- No ghost nodes on return

### Implementation Location

**ReactFlowWrapper.tsx** or **EdgeFlowPage.tsx**

```tsx
useEffect(() => {
  return () => {
    const { isActive, actions } = useWizardStore.getState()
    if (isActive) {
      actions.cancelWizard()
    }
  }
}, [])
```

### Edge Cases

- User clicks browser back button
- User closes tab/window (can't fully prevent)
- React Router navigation
- Page refresh (state lost, store resets)

---

## 3. Graph Editability Restrictions

### Requirement

When wizard is active and ghost node displayed, the whole graph becomes **read-only**.

### Rationale

- Prevent side panel from another node interfering
- Avoid configuration conflicts
- Clear focus on wizard task

### Affected Features

| Feature            | Normal Mode | Wizard Mode |
| ------------------ | ----------- | ----------- |
| Node selection     | ✅ Allowed  | ❌ Disabled |
| Node dragging      | ✅ Allowed  | ❌ Disabled |
| Side panel opening | ✅ Allowed  | ❌ Disabled |
| Edge connections   | ✅ Allowed  | ❌ Disabled |
| Context menu       | ✅ Allowed  | ❌ Disabled |
| Node deletion      | ✅ Allowed  | ❌ Disabled |

### Implementation Strategy

#### Option A: React Flow Props (Recommended)

```tsx
// In ReactFlowWrapper.tsx
const { isActive } = useWizardState()

<ReactFlow
  nodesDraggable={!isActive}
  nodesConnectable={!isActive}
  elementsSelectable={!isActive}
  // ... other props
>
```

**Pros:**

- Centralized control
- React Flow handles everything
- Clean and simple

**Cons:**

- All-or-nothing approach
- Can't have granular control per node

#### Option B: Per-Node Properties (More Control)

```tsx
// In useGetFlowElements or similar
const nodes = rawNodes.map((node) => ({
  ...node,
  draggable: !isActive,
  selectable: !isActive,
  connectable: !isActive,
}))
```

**Pros:**

- Granular control
- Can exclude specific nodes if needed

**Cons:**

- More complex
- Need to update all node creation logic

**Decision:** Use **Option A** for simplicity

---

## 4. Existing Nodes: Selectable & Draggable

### Requirement

All existing (non-ghost) nodes must have:

- `selectable = false`
- `draggable = false` (optional but recommended)

### Rationale

- Prevent accidental selection
- Avoid confusion with ghost nodes
- Maintain focus on wizard

### Implementation

Already covered by **Option A** above, but for clarity:

```tsx
// React Flow level (affects all nodes)
elementsSelectable={!isActive}
nodesDraggable={!isActive}
```

### Visual Feedback

- Nodes appear slightly dimmed (optional)
- No hover effects
- Cursor remains default (not pointer/grab)

---

## 5. Context Toolbar Disabling

### Requirement

Node context toolbars (the floating toolbar on node hover/selection) must be **disabled**.

### Current Context Toolbar Location

**ContextualToolbar.tsx** component

### Implementation Strategy

#### Check if Wizard Active

```tsx
// In ContextualToolbar.tsx
const { isActive } = useWizardState()

if (isActive) {
  return null // Don't render toolbar during wizard
}

// ... rest of component
```

### Affected Toolbars

- **NodeAdapter** toolbar
- **NodeBridge** toolbar
- **NodeGroup** expand/collapse toolbar
- **NodeCombiner** toolbar
- Any other node-specific toolbars

---

## 6. Side Panel Interactions

### Requirement

Prevent any side panel from opening during wizard.

### Current Side Panel Triggers

| Trigger            | Component            | Action                |
| ------------------ | -------------------- | --------------------- |
| Node click         | Various nodes        | Opens property drawer |
| Observability icon | ObservabilityEdgeCTA | Opens metrics panel   |
| DataHub icon       | NodeDatahubToolbar   | Opens policy panel    |

### Implementation

#### Option A: Prevent at Source (Recommended)

```tsx
// In each node component that opens drawers
const { isActive } = useWizardState()

const handleClick = () => {
  if (isActive) return // Ignore during wizard
  // ... normal click logic
}
```

#### Option B: Centralized Guard

```tsx
// Create a wrapper hook
export const useDrawerGuard = (onOpen: () => void) => {
  const { isActive } = useWizardState()

  return () => {
    if (isActive) return
    onOpen()
  }
}

// Usage
const handleOpen = useDrawerGuard(() => {
  // ... drawer logic
})
```

**Decision:** Use **Option B** for consistency

---

## 7. Edge Interactions

### Requirement

Edge clicks/hovers should not trigger any actions during wizard.

### Current Edge Features

- Click to view edge details
- Hover to show metrics
- Monitoring edge CTA button

### Implementation

```tsx
// In edge components
const { isActive } = useWizardState()

if (isActive) {
  // Render edge but with no interactions
  return <BaseEdge {...props} interactionWidth={0} />
}
```

---

## 8. Keyboard Shortcuts

### Requirement

Disable workspace keyboard shortcuts during wizard.

### Current Shortcuts

- **Ctrl+L** - Apply layout
- **Delete** - Delete selected nodes
- **Ctrl+A** - Select all
- **Ctrl+Z** - Undo (if implemented)

### Implementation

```tsx
// In useKeyboardShortcut usage
const { isActive } = useWizardState()

useKeyboardShortcut({
  key: 'l',
  ctrl: true,
  callback: isActive ? undefined : applyLayout,
  disabled: isActive,
})
```

---

## 9. Canvas Interactions

### Requirement

Canvas-level interactions should be limited during wizard.

### Features to Control

| Feature          | Normal     | Wizard Mode              |
| ---------------- | ---------- | ------------------------ |
| Pan              | ✅ Keep    | ✅ Keep (for navigation) |
| Zoom             | ✅ Keep    | ✅ Keep (for navigation) |
| Click background | Deselects  | ❌ No effect             |
| Box selection    | ✅ Enabled | ❌ Disabled              |
| Fit view         | ✅ Enabled | ✅ Keep (helpful)        |

### Implementation

```tsx
<ReactFlow
  panOnDrag={true} // Keep for both modes
  zoomOnScroll={true} // Keep for both modes
  selectionOnDrag={!isActive} // Disable box selection
>
```

---

## 10. MiniMap Interactions

### Requirement

MiniMap should remain visible but interactions limited.

### Implementation

- Keep visible (helps user see ghost node position)
- Disable node dragging via MiniMap
- Keep pan/zoom functionality

```tsx
<MiniMap
  nodesDraggable={!isActive}
  // ... other props
/>
```

---

## 11. Toolbar Controls

### Requirement

Other toolbar controls behavior during wizard.

### CanvasToolbar Components

| Component           | Normal     | Wizard Mode | Rationale                  |
| ------------------- | ---------- | ----------- | -------------------------- |
| **Create New**      | ✅ Enabled | ❌ Disabled | Covered above              |
| **Search**          | ✅ Enabled | ❌ Disabled | No node selection          |
| **Filter**          | ✅ Enabled | ❌ Disabled | No filtering during wizard |
| **Layout Selector** | ✅ Enabled | ❌ Disabled | Don't rearrange            |
| **Apply Layout**    | ✅ Enabled | ❌ Disabled | Don't rearrange            |
| **Layout Presets**  | ✅ Enabled | ❌ Disabled | Don't rearrange            |
| **Layout Options**  | ✅ Enabled | ❌ Disabled | Don't rearrange            |
| **Canvas Controls** | ✅ Enabled | ✅ Keep     | Pan/zoom useful            |

### Implementation

```tsx
// In CanvasToolbar.tsx
const { isActive } = useWizardState()

<SearchEntities isDisabled={isActive} />
<DrawerFilterToolbox isDisabled={isActive} />
<LayoutSelector isDisabled={isActive} />
<ApplyLayoutButton isDisabled={isActive} />
```

---

## 12. Visual Feedback System

### Requirement

User should clearly understand workspace is in "wizard mode".

### Visual Indicators

#### 1. Overlay Approach (Optional)

```tsx
{
  isActive && <Box position="absolute" inset={0} bg="blackAlpha.200" pointerEvents="none" zIndex={1} />
}
```

**Pros:** Very obvious
**Cons:** May be too intrusive

#### 2. Dimming Approach (Recommended)

```tsx
// Apply to node styles
const nodeStyle = {
  ...baseStyle,
  opacity: isWizardActive ? 0.5 : 1,
}
```

**Pros:** Subtle, clear focus
**Cons:** Requires per-node changes

#### 3. Cursor Approach

```tsx
<ReactFlow
  style={{
    cursor: isActive ? 'not-allowed' : 'default'
  }}
>
```

**Pros:** Clear feedback on hover
**Cons:** May be annoying

**Decision:** Use **Dimming Approach** for existing nodes

---

## 13. Error Prevention

### Requirement

Gracefully handle edge cases and errors.

### Scenarios

#### User Refreshes Page

- Zustand store resets (not persisted)
- Wizard state lost
- Ghost nodes disappear
- ✅ No issue - clean slate

#### User Opens Multiple Tabs

- Each tab has separate Zustand store
- No shared state
- ✅ Each tab independent

#### Wizard Crashes

- Error boundary should catch
- Should call `cancelWizard()` in error handler
- Clean recovery

```tsx
// Error boundary integration
componentDidCatch(error) {
  const { actions } = useWizardStore.getState()
  actions.cancelWizard()
}
```

#### API Call Fails During Wizard

- Show error in wizard (errorMessage state)
- Keep wizard active
- User can retry or cancel

---

## 14. Testing Strategy

### Test Categories

#### Unit Tests

- Wizard state changes correctly disable features
- Helper functions work correctly

#### Component Tests

- Buttons disabled when `isActive=true`
- Nodes not selectable/draggable
- Toolbars don't render

#### E2E Tests (Cypress)

- Start wizard → verify restrictions
- Try to click node → verify blocked
- Cancel wizard → verify restrictions lifted

---

## Implementation Plan

### Phase 1: Core Restrictions (Immediate)

1. ✅ Disable "Create New" button
2. ✅ Add unmount cleanup
3. ✅ Set React Flow props to disable interactions
4. ✅ Disable context toolbars

### Phase 2: UI Feedback (Next)

5. ✅ Dim existing nodes
6. ✅ Add tooltips to disabled buttons
7. ✅ Update cursor styles

### Phase 3: Edge Cases (Polish)

8. ✅ Disable keyboard shortcuts
9. ✅ Guard drawer openings
10. ✅ Add error boundaries

---

## Files to Modify

### High Priority

1. **CreateEntityButton.tsx** - Disable when active
2. **ReactFlowWrapper.tsx** - React Flow props + unmount cleanup
3. **ContextualToolbar.tsx** - Conditionally render
4. **CanvasToolbar.tsx** - Disable search/filter/layout

### Medium Priority

5. **Node components** - Add dimming style
6. **Edge components** - Disable interactions
7. **useKeyboardShortcut** - Add disabled prop

### Low Priority

8. **Error boundaries** - Add wizard cleanup
9. **Drawer components** - Add guard hook
10. **MiniMap** - Disable dragging

---

## Success Criteria

✅ User cannot start multiple wizards  
✅ Wizard cleans up on unmount  
✅ Existing nodes cannot be interacted with  
✅ Context toolbars hidden  
✅ Side panels don't open  
✅ Layout controls disabled  
✅ Visual feedback clear  
✅ Error recovery works  
✅ Tests pass

---

## Timeline

**Estimated:** 2-3 hours  
**Priority:** High (blocks Subtask 6)

---

**End of Planning Document**
