# Task: 38139-wizard-group

## Objective

Implement the Group wizard for the Workspace Operation Wizard system, extending the work from task 38111 to support creating groups through the wizard interface.

## Context

This task continues the wizard system implementation from task 38111. Groups are a special entity type in the workspace that allows users to organize related nodes (adapters, bridges, and other groups) into collapsible containers. The group wizard shares similarities with the Combiner wizard (both require node selection), but has unique requirements for ghost node preview and React Flow group implementation.

## Key Requirements

### Selection Constraints

- **Selectable Node Types**: ADAPTER_NODE, BRIDGE_NODE, CLUSTER_NODE (groups)
- **Minimum nodes**: 2
- **Exclusions**:
  - Nodes already in a group cannot be selected for a different group
  - Ghost nodes cannot be selected
  - EDGE_NODE cannot be selected
- **Auto-inclusion**: When adapters/bridges are selected, their associated DEVICE/HOST nodes are automatically included (but cannot be selected directly)

### Ghost Node Behavior

Groups require a special ghost approach different from Combiner/Asset Mapper:

**Real-time Ghost Group Formation** (Improved UX):

1. **Step 0 (Selection)**: Ghost group appears and updates dynamically

   - **Initial state**: No ghost group (nothing selected yet)
   - **First node selected**: Ghost group appears containing that node
   - **Additional nodes selected**: Ghost group boundary expands to include new nodes
   - **Node deselected**: Ghost group boundary shrinks, excluding that node
   - **Last node deselected**: Ghost group disappears (back to initial state)
   - **Throughout**: DEVICE/HOST nodes auto-included and visible in ghost group
   - Ghost group uses `getGroupBounds` to calculate boundary for current selection
   - Ghost group updates on every selection change (add/remove)

2. **Step 1 (Preview)**: Ghost group persists with finalized selection
   - Ghost group shows final structure before configuration
   - User can click "Back" to modify selection (ghost updates accordingly)
   - Selected nodes show as "children" with semi-transparent styling
   - Ghost group positioned using existing `getGroupBounds` utility

### React Flow Group Implementation

Groups in React Flow have specific requirements:

- **Parent-Child Relationship**: Selected nodes must have `parentId` set to group node ID
- **Node Order**: Group node must be added to nodes array BEFORE its children
- **Positioning**: Children positions are relative to group (existing behavior maintained)
- **Expand/Collapse**: Groups support `isOpen` property (set to `true` by default)

### Configuration

- **Step 2**: Configure group settings (title, color scheme)
- Reuse existing `createGroup` utility from `group.utils.ts`
- Form similar to existing group creation (ContextualToolbar pattern)

## Constraints

### Minimize Code Changes

- Reuse existing wizard infrastructure (metadata, store, components)
- Leverage existing `createGroup`, `getGroupBounds` utilities
- Minimal changes to WizardSelectionRestrictions
- Use existing GhostNodeRenderer patterns where possible

### Testing Requirements

- All new functions must have test suites
- All new components must have Cypress component tests
- Follow accessibility testing patterns (unskipped a11y tests)
- Test group-specific constraints (no duplicate memberships)

### Ghost Node Challenge

The main technical challenge is handling dynamic ghost preview for groups:

- **Problem**: Groups contain other nodes as children (parentId relationship), and the ghost must update in real-time
- **Solution**: Create a reactive ghost group that:
  - Appears dynamically in Step 0 when first node is selected
  - Updates boundary immediately when nodes are added/removed from selection
  - Shows selected nodes as semi-transparent children within ghost boundary
  - Includes auto-added DEVICE/HOST nodes (updated dynamically)
  - Handles edge case: disappears when selection becomes empty
  - Persists through Step 1 (preview) without re-creation
  - Can be cleanly converted to real group on completion

**Key Implementation Detail**: Use `selectedNodeIds` from wizard store as reactive trigger - any change recalculates ghost group bounds and updates ghost node positions.

## Reference Implementation

### Existing Group Creation (ContextualToolbar.tsx)

```typescript
const onCreateGroup = () => {
  if (!selectedGroupCandidates) return
  const rect = getNodesBounds(selectedGroupCandidates)
  const groupRect = getGroupBounds(rect)
  const { newGroupNode, newGroupEdge } = createGroup(selectedGroupCandidates, groupRect, theme)
  onInsertGroupNode(newGroupNode, newGroupEdge, groupRect)
}
```

### Wizard Metadata (Already Exists)

```typescript
[EntityTypeEnum.GROUP]: {
  type: EntityTypeEnum.GROUP,
  category: 'entity',
  icon: LuFolderTree,
  requiresSelection: true,
  requiresGhost: true,
  steps: [
    {
      index: 0,
      descriptionKey: 'step_GROUP_0', // "Select nodes to group"
      requiresSelection: true,
      selectionConstraints: {
        minNodes: 2,
        // No allowedNodeTypes restriction - can select any type
      },
    },
    {
      index: 1,
      descriptionKey: 'step_GROUP_1', // "Review group preview"
      showsGhostNodes: true,
    },
    {
      index: 2,
      descriptionKey: 'step_GROUP_2', // "Configure group settings"
      requiresConfiguration: true,
    },
  ],
},
```

## Acceptance Criteria

### Subtask 1: Selection Constraints Enhancement

- [ ] Update WizardSelectionRestrictions to handle group-specific constraints
- [ ] Filter out nodes already in groups
- [ ] Implement constraint: "Node X is already in group Y"
- [ ] Add tests for group selection constraints

### Subtask 2: Ghost Group Factory

- [ ] Create `createGhostGroup` function in ghostNodeFactory.ts
- [ ] Handle ghost group with children nodes
- [ ] Position ghost group using `getGroupBounds`
- [ ] Show auto-included DEVICE/HOST nodes
- [ ] Add tests for ghost group creation

### Subtask 3: Ghost Group Renderer Enhancement

- [ ] Update GhostNodeRenderer to handle Step 1 ghost groups
- [ ] Render ghost group only in Step 1 (not Step 0)
- [ ] Apply semi-transparent styling to children nodes
- [ ] Handle parent-child relationships in ghost state
- [ ] Add tests for ghost group rendering

### Subtask 4: Configuration Panel

- [ ] Create WizardGroupConfiguration component
- [ ] Form for group title and color scheme
- [ ] Integrate with WizardConfigurationPanel
- [ ] Add i18n keys for group configuration
- [ ] Add Cypress component tests

### Subtask 5: Wizard Completion

- [ ] Implement wizard completion logic for groups
- [ ] Convert ghost group + children to real nodes
- [ ] Use existing `createGroup` utility
- [ ] Handle workspace state update
- [ ] Add E2E tests for complete group creation flow

### Subtask 6: Integration & Polish

- [ ] Update wizard metadata with group-specific constraints
- [ ] Add i18n keys for all group wizard steps
- [ ] Test complete wizard flow (E2E)
- [ ] Update documentation
- [ ] Verify accessibility compliance

## Related Files

### To Modify

- `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts` - Add ghost group creation
- `src/modules/Workspace/components/wizard/GhostNodeRenderer.tsx` - Handle ghost groups
- `src/modules/Workspace/components/wizard/WizardSelectionRestrictions.tsx` - Add group constraints
- `src/modules/Workspace/components/wizard/WizardConfigurationPanel.tsx` - Add group config route
- `src/modules/Workspace/components/wizard/utils/wizardMetadata.ts` - Update constraints
- `src/locales/en/translation.json` - Add i18n keys

### To Create

- `src/modules/Workspace/components/wizard/WizardGroupConfiguration.tsx` - Group config form
- `src/modules/Workspace/components/wizard/WizardGroupConfiguration.spec.cy.tsx` - Component tests
- `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.spec.ts` - Test ghost group (update existing)

### Reference (No Changes)

- `src/modules/Workspace/utils/group.utils.ts` - Use existing utilities
- `src/modules/Workspace/components/nodes/ContextualToolbar.tsx` - Reference implementation

## Guidelines to Follow

- **DATAHUB_ARCHITECTURE.md** - Workspace state management patterns
- **TESTING_GUIDELINES.md** - Accessibility tests mandatory
- **I18N_GUIDELINES.md** - Translation key structure
- **DESIGN_GUIDELINES.md** - UI component patterns
- **WORKSPACE_TOPOLOGY.md** - Node relationships

## Success Metrics

- Users can create groups through wizard with 2+ selected nodes
- Ghost preview accurately shows future group structure
- DEVICE/HOST nodes auto-included in group
- Existing group creation functionality remains unchanged
- No nodes can belong to multiple groups
- All accessibility tests pass
- Complete E2E wizard flow works

---

**Created**: November 21, 2025  
**Depends On**: Task 38111 (Workspace Operation Wizard)  
**Status**: Planning
