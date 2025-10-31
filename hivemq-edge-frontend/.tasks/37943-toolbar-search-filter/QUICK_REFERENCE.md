# Task 37943 - Quick Reference

**Status:** 🟡 Ready to Start  
**Approach:** Extend existing CanvasToolbar (DON'T reinvent!)

---

## 3 Simple Subtasks

### 1️⃣ Add ARIA Attributes (10-15 min)

**File:** `CanvasToolbar.tsx`  
**Changes:** Add `aria-expanded`, `aria-controls`, `id`, `role="region"`  
**Rule:** ❌ NO test changes  
**Checkpoint:** Get user approval ✋

### 2️⃣ Move & Integrate (30-45 min)

**Files:** `CanvasToolbar.tsx`, `ReactFlowWrapper.tsx`  
**Changes:**

- Move toolbar from `top-right` to `top-left`
- Add layout controls section with divider
- Remove `<LayoutControlsPanel />` from ReactFlowWrapper

**Rule:** ❌ NO test changes yet  
**Checkpoint:** Get user approval ✋

### 3️⃣ Update Tests & Cleanup (30-45 min)

**File:** `CanvasToolbar.spec.cy.tsx`  
**Changes:**

- Add 8+ tests for layout section
- Update accessibility test
- Remove old LayoutControlsPanel files

**Checkpoint:** All tests passing ✅

---

## Key Principles

✅ **DO:**

- Leverage existing CanvasToolbar
- Stop after each subtask for approval
- Test incrementally
- Follow ARIA_REFERENCE.md

❌ **DON'T:**

- Create new WorkspaceToolbar component
- Modify passing tests before agreement
- Change existing behavior
- Break animations

---

## Existing CanvasToolbar Has

- ✅ Collapse/expand working
- ✅ Animations (400ms)
- ✅ SearchEntities
- ✅ DrawerFilterToolbox
- ✅ Tests passing

**We just need to:**

1. Add ARIA
2. Move to left + add layout section
3. Update tests

---

## Quick Commands

```bash
# Run component tests
pnpm test:component CanvasToolbar

# Run all component tests
pnpm test:component

# Check for errors
pnpm typecheck
```

---

## Files to Touch

**Modify:**

- `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
- `src/modules/Workspace/components/controls/CanvasToolbar.spec.cy.tsx`
- `src/modules/Workspace/components/ReactFlowWrapper.tsx`

**Remove (in Subtask 3):**

- `src/modules/Workspace/components/controls/LayoutControlsPanel.tsx`
- `src/modules/Workspace/components/controls/LayoutControlsPanel.spec.cy.tsx`

---

## Success Criteria

- [ ] All existing tests still pass
- [ ] ARIA attributes present
- [ ] Toolbar at top-left
- [ ] Both sections visible when expanded
- [ ] Visual separator between sections
- [ ] All controls functional
- [ ] 8+ new tests added
- [ ] Old components removed
- [ ] User approval at each checkpoint ✋

---

**Ready?** Start with Subtask 1! 🚀
