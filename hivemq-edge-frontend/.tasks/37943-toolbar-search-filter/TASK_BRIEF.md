# Task: 37943-toolbar-search-filter

## Objective

Combine the layout controls toolbar (top-left) and search/filter toolbar (top-right) into a single collapsible toolbar positioned at the top-left of the canvas.

## Context

This is a follow-up to task 25337 (workspace-auto-layout), where we created a layout controls toolbar positioned at the top-left of the canvas. During that task, two improvements were mentioned for the layout toolbar:

- Make the toolbar expandable to reduce footprint on the canvas
- Make the toolbar responsive, possibly turning to icon-only with small media size

There is an existing search & filter toolbar positioned on the top-right of the canvas (CanvasToolbar.tsx). This toolbar already has collapse/expand functionality.

## Current State

### Layout Controls Panel (Top-Left)

- **Location:** `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx`
- **Position:** `top-left` of canvas
- **Components:**
  - LayoutSelector - Algorithm selection dropdown
  - ApplyLayoutButton - Apply layout button
  - LayoutPresetsManager - Manage saved presets
  - Settings button - Opens LayoutOptionsDrawer
- **State:** Always expanded, no collapse functionality
- **Tests:** `LayoutControlsPanel.spec.cy.tsx` (7 tests including accessibility)

### Search/Filter Toolbar (Top-Right)

- **Location:** `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
- **Position:** `top-right` of canvas
- **Components:**
  - SearchEntities - Search input
  - DrawerFilterToolbox - Filter drawer trigger
  - Expand/Collapse buttons
- **State:** Collapsible with animated transitions
- **Tests:** `CanvasToolbar.spec.cy.tsx` (1 test)

## Goal

Merge both toolbars into a single collapsible toolbar at top-left with:

- **Section 1 (Top):** Search & Filter controls
- **Section 2 (Bottom):** Layout controls

## Proposed Layout

```
┌────────────────��────────────┐
│ [Expand Button + Icons]     │  ← Collapsed state
└─────────────────────────────┘

┌─────────────────────────────┐
│ ╔═══════════════════════╗  │
│ ║ SEARCH & FILTER       ║  │  ← Section 1
│ ║ - Search input        ║  │
│ ║ - Filter button       ║  │
│ ╚═══════════════════════╝  │
│                             │
│ ╔═══════════════════════╗  │
│ ║ LAYOUT CONTROLS       ║  │  ← Section 2
│ ║ - Algorithm selector  ║  │
│ ║ - Apply button        ║  │
│ ║ - Presets manager     ║  │
│ ║ - Settings button     ║  │
│ ╚═══════════════════════╝  │
│                             │
│ [Collapse Button]           │
└─────────────────────────────┘
```

## Acceptance Criteria

1. ✅ Single toolbar positioned at `top-left` of canvas
2. ✅ Collapsible with smooth animations (reuse existing animation constants)
3. ✅ Search & Filter section at the top
4. ✅ Layout Controls section at the bottom
5. ✅ Proper ARIA attributes for collapsed/expanded states:
   - `aria-expanded="true|false"` on toggle button
   - `aria-controls` pointing to content ID
   - `aria-label` describing the toolbar purpose
6. ✅ Visual separation between sections (divider or spacing)
7. ✅ All existing tests passing
8. ✅ New accessibility tests covering:
   - Collapsed state accessibility
   - Expanded state accessibility
   - Section navigation with screen readers
9. ✅ Responsive behavior (optional enhancement for media queries)

## Technical Requirements

### ARIA Attributes for Collapsible Widgets

According to WAI-ARIA best practices for disclosure (show/hide) widgets:

- Toggle button MUST have `aria-expanded="true"` when expanded, `"false"` when collapsed
- Toggle button SHOULD have `aria-controls="id"` pointing to the content element
- Content element SHOULD have a unique `id`
- Toggle button MUST have descriptive `aria-label`
- Sections within expanded content SHOULD have `role="region"` with `aria-label` for screen reader navigation

### Guidelines to Follow

- **Testing Guidelines:** `.tasks/TESTING_GUIDELINES.md`
  - Mandatory accessibility test pattern
  - Component test patterns
  - Test naming conventions
- **Design Guidelines:** `.tasks/DESIGN_GUIDELINES.md`
  - Button variant usage
  - UI component patterns
- **Reporting Strategy:** `.tasks/REPORTING_STRATEGY.md`
  - Create CONVERSATION_SUBTASK_N.md for each subtask
  - Update TASK_SUMMARY.md after each phase

## Implementation Notes

- Reuse existing animation constants from `src/modules/Theme/utils.ts`
- Consider creating a shared `CollapsibleToolbar` wrapper component
- Ensure feature flag `config.features.WORKSPACE_AUTO_LAYOUT` still controls layout section visibility
- Maintain existing keyboard shortcuts (Ctrl/Cmd+L for apply layout)
- Keep existing drawer components (LayoutOptionsDrawer, DrawerFilterToolbox) functional

## Related Files

### Components to Modify

- `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx`
- `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
- `src/modules/Workspace/components/ReactFlowWrapper.tsx` (imports both toolbars)

### Tests to Update

- `src/modules/Workspace/components/controls/LayoutControlsPanel.spec.cy.tsx`
- `src/modules/Workspace/components/controls/CanvasToolbar.spec.cy.tsx`

### Utility Files

- `src/modules/Theme/utils.ts` (animation constants)

## References

- WAI-ARIA Disclosure Pattern: https://www.w3.org/WAI/ARIA/apg/patterns/disclosure/
- Task 25337: `.tasks/25337-workspace-auto-layout/`
