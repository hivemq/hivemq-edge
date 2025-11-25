# Quick Reference: Task 38139 Wizard Group

**Status**: 📋 Planning Complete - Ready for Implementation  
**Dependencies**: Task 38111 (Workspace Operation Wizard)  
**Estimated Duration**: 2-3 weeks

---

## 🚀 Start Here

**Next Action**: Subtask 1 - Group Selection Constraints

**First File to Create**: `src/modules/Workspace/components/wizard/utils/groupConstraints.ts`

---

## 📚 Essential Documents

| Document          | Purpose                                   | Location                                     |
| ----------------- | ----------------------------------------- | -------------------------------------------- |
| **TASK_PLAN.md**  | Complete 8-subtask implementation plan    | `.tasks/38139-wizard-group/`                 |
| **TASK_BRIEF.md** | Requirements and constraints              | `.tasks/38139-wizard-group/`                 |
| **Group Utils**   | Existing group creation logic (reference) | `src/modules/Workspace/utils/group.utils.ts` |

---

## ⚠️ Critical Constraints

### Group Selection Rules

1. ✅ Can select: ADAPTER, BRIDGE, CLUSTER (group) nodes
2. ❌ Cannot select: Nodes already in a group
3. ❌ Cannot select: DEVICE, HOST (auto-included instead)
4. ➕ Auto-include: DEVICE nodes for selected adapters
5. ➕ Auto-include: HOST nodes for selected bridges

### Ghost Group Behavior (Different from Other Wizards!)

| Step       | Ghost State               | User Action           |
| ---------- | ------------------------- | --------------------- |
| **Step 0** | No ghost group            | Select 2+ nodes       |
| **Step 1** | Ghost group with children | Review preview        |
| **Step 2** | Ghost persists            | Configure title/color |

**Why Different?**

- Groups contain other nodes (parent-child relationship)
- Showing ghost in Step 0 would duplicate selected nodes
- Ghost appears in Step 1 to preview final structure

### React Flow Group Requirements

**CRITICAL**: Group node must be added to nodes array BEFORE its children

```typescript
// ✅ CORRECT
const nodes = [groupNode, ...childNodes]

// ❌ WRONG
const nodes = [...childNodes, groupNode]
```

---

## 🏗️ Architecture Overview

### Three-Step Wizard Flow

```
Step 0: SELECTION
├── User clicks nodes (ADAPTER, BRIDGE, CLUSTER)
├── Selection panel shows count + auto-included nodes
├── WizardSelectionRestrictions filters selectable nodes
└── Next button enabled when minNodes (2) met

Step 1: PREVIEW
├── GhostNodeRenderer creates ghost group
├── Selected nodes become ghost children (parentId set)
├── Auto-included DEVICE/HOST shown in ghost group
└── User reviews structure before configuring

Step 2: CONFIGURATION
├── Form: Group title (required)
├── Form: Color scheme (blue, green, red, purple, orange)
└── Complete button creates real group
```

### Key Components

```
Group Wizard Architecture
├── Selection Layer (Step 0)
│   ├── WizardSelectionRestrictions (filters)
│   ├── groupConstraints.ts (helpers)
│   └── AutoIncludedNodesList (UI)
│
├── Preview Layer (Step 1)
│   ├── GhostNodeRenderer (creates ghost group)
│   ├── ghostNodeFactory.ts (createGhostGroup)
│   └── Ghost group with children
│
└── Configuration Layer (Step 2)
    ├── WizardGroupConfiguration (form)
    ├── groupWizardCompletion.ts (creates real group)
    └── Uses existing createGroup utility
```

---

## 📝 Subtask Checklist

### ✅ Phase 1: Foundation (Days 1-3)

- [ ] **Subtask 1**: Group Selection Constraints (1-2 days)

  - Create `groupConstraints.ts` with helpers
  - Update `WizardSelectionRestrictions`
  - Add tests
  - File: `src/modules/Workspace/components/wizard/utils/groupConstraints.ts`

- [ ] **Subtask 2**: Auto-Inclusion UI (1 day)
  - Create `AutoIncludedNodesList` component
  - Integrate into `WizardSelectionPanel`
  - Add i18n keys
  - File: `src/modules/Workspace/components/wizard/AutoIncludedNodesList.tsx`

### ✅ Phase 2: Ghost System (Days 4-7)

- [ ] **Subtask 3**: Ghost Group Factory (2 days)

  - Add `createGhostGroup` to `ghostNodeFactory.ts`
  - Handle parent-child relationships
  - Calculate bounds with auto-included nodes
  - Add tests

- [ ] **Subtask 4**: Ghost Group Renderer (2 days)
  - Update `GhostNodeRenderer` for Step 1
  - Hide real nodes, show ghost children
  - Handle cleanup
  - Add tests

### ✅ Phase 3: Configuration (Days 8-10)

- [ ] **Subtask 5**: Configuration Form (1.5 days)

  - Create `WizardGroupConfiguration` component
  - Title input + color selection
  - Integrate into `WizardConfigurationPanel`
  - Add tests

- [ ] **Subtask 6**: Wizard Completion (2 days)
  - Create `groupWizardCompletion.ts`
  - Convert ghost to real group
  - Set parentId relationships
  - Add tests

### ✅ Phase 4: Testing & Polish (Days 11-13)

- [ ] **Subtask 7**: E2E Testing (1.5 days)

  - Create `wizard-create-group.spec.cy.ts`
  - Test complete flow
  - Test edge cases

- [ ] **Subtask 8**: Documentation (1 day)
  - Create task summary
  - Create user documentation
  - Final polish

---

## 🛠️ Key Functions to Implement

### 1. `canNodeBeGrouped(node, allNodes, selectedNodeIds): boolean`

**Location**: `groupConstraints.ts`  
**Purpose**: Check if node can be selected for grouping  
**Logic**:

- Return false if `node.data.isGhost`
- Return false if `node.id === IdStubs.EDGE_NODE`
- Return false if `node.type` is DEVICE or HOST
- Return false if `node.parentId` exists (already in group)
- Return true if type is ADAPTER, BRIDGE, or CLUSTER

### 2. `getAutoIncludedNodes(selectedNodes, allNodes, allEdges): Node[]`

**Location**: `groupConstraints.ts`  
**Purpose**: Find DEVICE/HOST nodes to auto-include  
**Logic**:

- For each adapter, find connected DEVICE via edges
- For each bridge, find connected HOST via edges
- Return array of found nodes (deduplicated)

### 3. `createGhostGroup(selectedNodes, allNodes, allEdges): GhostNodeGroup`

**Location**: `ghostNodeFactory.ts`  
**Purpose**: Create ghost group with children for preview  
**Returns**: `{ nodes: [groupNode, ...childNodes], edges: [groupEdge] }`  
**Key Points**:

- Group node FIRST in array (React Flow requirement)
- Children have `parentId` set to group ID
- Children positions relative to group origin
- All marked with `isGhost: true`

### 4. `completeGroupWizard(selectedNodeIds, config, nodes, edges, theme)`

**Location**: `groupWizardCompletion.ts`  
**Purpose**: Convert ghost group to real group  
**Returns**: `{ newGroupNode, newGroupEdge, updatedNodes }`  
**Key Points**:

- Use existing `createGroup` utility
- Apply user config (title, colorScheme)
- Update nodes with parentId
- Remove all ghost nodes

---

## 🎯 Testing Strategy

### Unit Tests (Jest/Vitest)

- `groupConstraints.spec.ts` - All helper functions
- `ghostNodeFactory.spec.ts` - Ghost group creation
- `groupWizardCompletion.spec.ts` - Completion logic

### Component Tests (Cypress)

- `AutoIncludedNodesList.spec.cy.tsx`
- `WizardGroupConfiguration.spec.cy.tsx`
- `GhostNodeRenderer.spec.cy.tsx` (update existing)

### E2E Tests (Cypress)

- `wizard-create-group.spec.cy.ts` - Complete flow
- Test edge cases (cancel, already grouped, etc.)

### Accessibility Tests

- **Mandatory**: One unskipped a11y test per component
- All other tests can be skipped (pragmatic strategy)

---

## 🚨 Common Pitfalls

### 1. Node Ordering

❌ **Wrong**: Adding group after children

```typescript
const nodes = [...childNodes, groupNode] // BREAKS!
```

✅ **Correct**: Group first, then children

```typescript
const nodes = [groupNode, ...childNodes] // Works!
```

### 2. Ghost Node Duplication

❌ **Wrong**: Showing real nodes + ghost children in Step 1

- User sees same node twice

✅ **Correct**: Hide real selected nodes when ghost group appears

- Filter out `selectedNodeIds` from visible nodes

### 3. Position Calculation

❌ **Wrong**: Absolute positions for children

```typescript
child.position = { x: 100, y: 200 } // Breaks group layout
```

✅ **Correct**: Relative positions within group

```typescript
child.position = {
  x: child.position.x - groupRect.x,
  y: child.position.y - groupRect.y,
}
```

---

## 📊 Success Metrics

| Metric             | Target |
| ------------------ | ------ |
| **New Functions**  | ~10    |
| **New Components** | 2      |
| **New Tests**      | 30+    |
| **Files Modified** | 8      |
| **Files Created**  | 6      |
| **Lines of Code**  | ~1,500 |
| **Test Coverage**  | 80%+   |

---

## 🔗 Key References

### Existing Group Code

- `src/modules/Workspace/utils/group.utils.ts` - `createGroup`, `getGroupBounds`
- `src/modules/Workspace/components/nodes/ContextualToolbar.tsx` - `onCreateGroup`

### Existing Wizard Code

- `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts` - Ghost patterns
- `src/modules/Workspace/components/wizard/WizardSelectionRestrictions.tsx` - Selection logic
- `src/modules/Workspace/components/wizard/GhostNodeRenderer.tsx` - Rendering logic

### Guidelines

- `.tasks/DATAHUB_ARCHITECTURE.md` - State management
- `.tasks/TESTING_GUIDELINES.md` - Test requirements
- `.tasks/I18N_GUIDELINES.md` - Translation keys
- `.tasks/DESIGN_GUIDELINES.md` - UI patterns

---

## 💡 Quick Tips

**Tip 1**: Study Combiner wizard first - groups share selection pattern

**Tip 2**: Ghost group is unique - appears in Step 1, not Step 0

**Tip 3**: Auto-inclusion is a UX feature - makes grouping intuitive

**Tip 4**: React Flow groups are powerful but strict about node ordering

**Tip 5**: Reuse existing `createGroup` - don't reinvent the wheel

---

**Plan Created**: November 21, 2025  
**Ready to Start**: Subtask 1 (Group Selection Constraints)  
**Questions?** Review TASK_PLAN.md for detailed implementation

---

_Good luck! The foundation from task 38111 makes this much easier._
