# Task 38139: Wizard Group Implementation - Detailed Plan

**Task ID**: 38139-wizard-group  
**Created**: November 21, 2025  
**Depends On**: Task 38111 (Workspace Operation Wizard)  
**Estimated Duration**: 2-3 weeks

---

## Overview

Implement the Group wizard as the final entity wizard in the Workspace Operation Wizard system. This wizard allows users to create groups by selecting 2+ nodes (adapters, bridges, or groups) through an interactive selection interface, with a unique ghost preview approach suited for React Flow's parent-child group model.

---

## Phase Breakdown

### Phase 1: Foundation & Constraints (Subtasks 1-2)

**Focus**: Selection logic and constraints  
**Duration**: 3-4 days

### Phase 2: Ghost Group System (Subtasks 3-4)

**Focus**: Ghost node preview for groups  
**Duration**: 4-5 days

### Phase 3: Configuration & Completion (Subtasks 5-6)

**Focus**: Form integration and wizard completion  
**Duration**: 3-4 days

### Phase 4: Testing & Polish (Subtasks 7-8)

**Focus**: E2E testing, accessibility, documentation  
**Duration**: 2-3 days

---

## Subtask Breakdown

### ✅ Subtask 1: Group Selection Constraints

**Objective**: Enhance WizardSelectionRestrictions to handle group-specific selection rules

**Why This First**: Selection is Step 0 of the wizard - must work before ghost preview

**Files to Modify**:

- `src/modules/Workspace/components/wizard/WizardSelectionRestrictions.tsx`
- `src/modules/Workspace/components/wizard/utils/wizardMetadata.ts`

**Files to Create**:

- `src/modules/Workspace/components/wizard/utils/groupConstraints.ts` (helper utilities)
- `src/modules/Workspace/components/wizard/utils/groupConstraints.spec.ts`

**Implementation Steps**:

1. **Create Group Constraint Helpers** (`groupConstraints.ts`)

   ```typescript
   /**
    * Check if a node is already in a group
    */
   export const isNodeInGroup = (node: Node, allNodes: Node[]): boolean => {
     return !!node.parentId
   }

   /**
    * Get the group that contains a node
    */
   export const getNodeParentGroup = (node: Node, allNodes: Node[]): Node | null => {
     if (!node.parentId) return null
     return allNodes.find((n) => n.id === node.parentId) || null
   }

   /**
    * Check if node can be selected for a new group
    */
   export const canNodeBeGrouped = (node: Node, allNodes: Node[], selectedNodeIds: string[]): boolean => {
     // Ghost nodes cannot be grouped
     if (node.data?.isGhost) return false

     // EDGE node cannot be grouped
     if (node.id === IdStubs.EDGE_NODE) return false

     // DEVICE and HOST nodes cannot be directly selected (auto-included)
     if (node.type === NodeTypes.DEVICE_NODE || node.type === NodeTypes.HOST_NODE) {
       return false
     }

     // Node already in a group cannot be selected for another group
     if (isNodeInGroup(node, allNodes)) return false

     // Allowed types: ADAPTER, BRIDGE, CLUSTER (group)
     const allowedTypes = [NodeTypes.ADAPTER_NODE, NodeTypes.BRIDGE_NODE, NodeTypes.CLUSTER_NODE]
     return allowedTypes.includes(node.type || '')
   }

   /**
    * Get DEVICE/HOST nodes that should be auto-included with selection
    */
   export const getAutoIncludedNodes = (selectedNodes: Node[], allNodes: Node[], allEdges: Edge[]): Node[] => {
     const autoIncluded: Node[] = []

     selectedNodes.forEach((node) => {
       // For adapters, find connected DEVICE node
       if (node.type === NodeTypes.ADAPTER_NODE) {
         const deviceEdge =
           allEdges.find((e) => e.source === node.id && e.target.startsWith(IdStubs.DEVICE_NODE)) ||
           allEdges.find((e) => e.target === node.id && e.source.startsWith(IdStubs.DEVICE_NODE))
         if (deviceEdge) {
           const deviceId = deviceEdge.source.startsWith(IdStubs.DEVICE_NODE) ? deviceEdge.source : deviceEdge.target
           const deviceNode = allNodes.find((n) => n.id === deviceId)
           if (deviceNode && !autoIncluded.some((n) => n.id === deviceNode.id)) {
             autoIncluded.push(deviceNode)
           }
         }
       }

       // For bridges, find connected HOST node
       if (node.type === NodeTypes.BRIDGE_NODE) {
         const hostEdge =
           allEdges.find((e) => e.source === node.id && e.target.startsWith(IdStubs.HOST_NODE)) ||
           allEdges.find((e) => e.target === node.id && e.source.startsWith(IdStubs.HOST_NODE))
         if (hostEdge) {
           const hostId = hostEdge.source.startsWith(IdStubs.HOST_NODE) ? hostEdge.source : hostEdge.target
           const hostNode = allNodes.find((n) => n.id === hostId)
           if (hostNode && !autoIncluded.some((n) => n.id === hostNode.id)) {
             autoIncluded.push(hostNode)
           }
         }
       }
     })

     return autoIncluded
   }
   ```

2. **Update wizardMetadata.ts**

   - Add `customFilter` to GROUP selection constraints
   - Add `excludeAlreadyGrouped: true` flag
   - Add `autoIncludeRelated: true` flag

3. **Update WizardSelectionRestrictions.tsx**

   - In `checkConstraints`, add group-specific logic
   - Use `canNodeBeGrouped` helper
   - Show toast if user tries to select already-grouped node

4. **Write Tests** (`groupConstraints.spec.ts`)
   - Test `isNodeInGroup` with various scenarios
   - Test `canNodeBeGrouped` with all exclusion cases
   - Test `getAutoIncludedNodes` for adapters and bridges
   - Test edge cases (no connected device/host)

**Acceptance Criteria**:

- [x] Nodes in groups show disabled state during selection
- [x] Clicking grouped node shows toast: "Node X is already in group Y"
- [x] Only ADAPTER, BRIDGE, CLUSTER nodes are selectable
- [x] DEVICE/HOST nodes cannot be directly selected
- [x] All tests passing (accessibility test unskipped)

**Estimated Time**: 1-2 days

---

### ✅ Subtask 2: Auto-Inclusion Visual Feedback

**Objective**: Show visual indication of DEVICE/HOST nodes that will be auto-included

**Why After Subtask 1**: Depends on selection constraint logic

**Files to Modify**:

- `src/modules/Workspace/components/wizard/WizardSelectionPanel.tsx`

**Files to Create**:

- `src/modules/Workspace/components/wizard/AutoIncludedNodesList.tsx`
- `src/modules/Workspace/components/wizard/AutoIncludedNodesList.spec.cy.tsx`

**Implementation Steps**:

1. **Create AutoIncludedNodesList Component**

   ```typescript
   interface AutoIncludedNodesListProps {
     autoIncludedNodes: Node[]
   }

   const AutoIncludedNodesList: FC<AutoIncludedNodesListProps> = ({ autoIncludedNodes }) => {
     const { t } = useTranslation()

     if (autoIncludedNodes.length === 0) return null

     return (
       <Box mt={4} p={3} bg="blue.50" borderRadius="md">
         <Text fontSize="sm" fontWeight="medium" mb={2}>
           {t('workspace.wizard.group.autoIncluded')}
         </Text>
         <VStack align="stretch" spacing={1}>
           {autoIncludedNodes.map(node => (
             <HStack key={node.id} fontSize="sm">
               <Icon as={LuPlus} color="blue.500" />
               <Text>{node.data?.label || node.id}</Text>
               <Text color="gray.500">
                 ({node.type === NodeTypes.DEVICE_NODE ? 'Device' : 'Host'})
               </Text>
             </HStack>
           ))}
         </VStack>
       </Box>
     )
   }
   ```

2. **Integrate into WizardSelectionPanel**

   - Calculate auto-included nodes using `getAutoIncludedNodes`
   - Show AutoIncludedNodesList below selected nodes
   - Update selection count to include auto-included nodes

3. **Add i18n Keys**

   ```json
   {
     "workspace.wizard.group.autoIncluded": "These nodes will also be included:",
     "workspace.wizard.group.selectionCount": "{{count}} node selected ({{autoCount}} auto-included)",
     "workspace.wizard.group.selectionCount_other": "{{count}} nodes selected ({{autoCount}} auto-included)"
   }
   ```

4. **Write Component Tests**
   - Test rendering with 0 auto-included nodes (hidden)
   - Test rendering with 1+ auto-included nodes
   - Test correct device/host type labels
   - Accessibility test (unskipped)

**Acceptance Criteria**:

- [x] Auto-included nodes shown in separate section
- [x] Section hidden if no auto-included nodes
- [x] Selection count includes auto-included nodes
- [x] Visual distinction (blue background, plus icon)
- [x] Accessibility compliant

**Estimated Time**: 1 day

---

### ✅ Subtask 3: Ghost Group Factory Function

**Objective**: Create ghost group node with children for preview step

**Why After Subtask 1-2**: Needs selection data to create ghost group

**Files to Modify**:

- `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.ts`
- `src/modules/Workspace/components/wizard/utils/ghostNodeFactory.spec.ts`

**Implementation Steps**:

1. **Add Ghost Group Creation Function**

   ```typescript
   /**
    * Create a ghost group node with children
    *
    * NOTE: This differs from other ghost nodes because:
    * - Ghost group appears only in Step 1 (preview), not Step 0 (selection)
    * - Children nodes are marked as ghosts but retain their IDs
    * - Children nodes have parentId set to ghost group ID
    * - Group must be added to nodes array BEFORE children
    */
   export const createGhostGroup = (selectedNodes: Node[], allNodes: Node[], allEdges: Edge[]): GhostNodeGroup => {
     // Calculate auto-included DEVICE/HOST nodes
     const autoIncludedNodes = getAutoIncludedNodes(selectedNodes, allNodes, allEdges)

     // All nodes that will be in the group
     const allGroupNodes = [...selectedNodes, ...autoIncludedNodes]

     // Calculate group bounds
     const rect = getNodesBounds(allGroupNodes)
     const groupRect = getGroupBounds(rect)

     // Create ghost group ID
     const ghostGroupId = `ghost-group-${Date.now()}`

     // Create ghost group node
     const ghostGroupNode: GhostNode = {
       ...GHOST_BASE,
       id: ghostGroupId,
       type: NodeTypes.CLUSTER_NODE,
       position: { x: groupRect.x, y: groupRect.y },
       style: {
         ...GHOST_STYLE_ENHANCED,
         width: groupRect.width,
         height: groupRect.height,
       },
       data: {
         isGhost: true,
         childrenNodeIds: allGroupNodes.map((n) => n.id),
         title: i18n.t('workspace.grouping.untitled'),
         isOpen: true,
         colorScheme: 'blue',
       },
       selectable: false,
       draggable: false,
     }

     // Create ghost children - these are clones with parentId set
     const ghostChildren: GhostNode[] = allGroupNodes.map((node) => ({
       ...node,
       id: `${node.id}`, // Keep original ID for edge connections
       data: {
         ...node.data,
         isGhost: true, // Mark as ghost
         _originalNode: node, // Store reference for restoration
       },
       parentId: ghostGroupId, // Set parent relationship
       position: {
         // Position relative to group (subtract group position)
         x: node.position.x - groupRect.x,
         y: node.position.y - groupRect.y,
       },
       style: {
         ...node.style,
         ...GHOST_STYLE, // Apply ghost styling
       },
       draggable: false,
       selectable: false,
     }))

     // Create ghost edge from group to EDGE node
     const ghostEdge: GhostEdge = {
       id: `${IdStubs.CONNECTOR}-${IdStubs.EDGE_NODE}-${ghostGroupId}`,
       source: ghostGroupId,
       target: IdStubs.EDGE_NODE,
       targetHandle: 'Top',
       type: EdgeTypes.DYNAMIC_EDGE,
       animated: false,
       style: GHOST_EDGE_STYLE,
       markerEnd: {
         type: MarkerType.ArrowClosed,
         width: 20,
         height: 20,
         color: '#4299E1',
       },
       hidden: false,
       focusable: false,
       data: { isGhost: true },
     }

     return {
       // Group node MUST come first in array (React Flow requirement)
       nodes: [ghostGroupNode, ...ghostChildren],
       edges: [ghostEdge],
     }
   }
   ```

2. **Add Helper: Remove Ghost Group**

   ```typescript
   /**
    * Remove ghost group and restore original nodes
    */
   export const removeGhostGroup = (ghostGroupId: string, allNodes: Node[]): Node[] => {
     return allNodes.filter((node) => {
       // Remove ghost group itself
       if (node.id === ghostGroupId) return false

       // Remove ghost children, restore originals if they were real nodes
       if (node.parentId === ghostGroupId && node.data?.isGhost) {
         // Ghost child - remove it
         return false
       }

       return true
     })
   }
   ```

3. **Update Tests** (`ghostNodeFactory.spec.ts`)
   - Test `createGhostGroup` with 2 adapters (includes 2 devices)
   - Test `createGhostGroup` with mixed adapters + bridges
   - Test `createGhostGroup` with nested groups
   - Test parentId relationships
   - Test node ordering (group first)
   - Test `removeGhostGroup` cleanup
   - Accessibility test (if applicable)

**Acceptance Criteria**:

- [x] Ghost group created with correct bounds
- [x] Children have parentId set to ghost group
- [x] Children positions are relative to group
- [x] Group node appears first in nodes array
- [x] Auto-included DEVICE/HOST nodes included
- [x] All ghost nodes marked with `isGhost: true`
- [x] Tests passing

**Estimated Time**: 2 days

---

### ✅ Subtask 4: Ghost Group Renderer Integration

**Objective**: Update GhostNodeRenderer to handle ghost groups in Step 1

**Why After Subtask 3**: Needs ghost group factory function

**Files to Modify**:

- `src/modules/Workspace/components/wizard/GhostNodeRenderer.tsx`
- `src/modules/Workspace/components/wizard/GhostNodeRenderer.spec.cy.tsx`

**Implementation Steps**:

1. **Add Step-Aware Ghost Rendering Logic**

   ```typescript
   const GhostNodeRenderer: FC = () => {
     const { isActive, entityType, currentStep, selectedNodeIds } = useWizardState()
     const { getNodes, setNodes, getEdges, setEdges } = useReactFlow()

     // ... existing ghost logic for adapters, bridges, combiners ...

     // GROUP ghost logic - only in Step 1 (preview step)
     useEffect(() => {
       if (!isActive || entityType !== EntityType.GROUP) return

       // Step 0: Selection (no ghost group yet)
       if (currentStep === 0) {
         // Clean up any existing ghost group from previous attempts
         const nodes = getNodes()
         const cleaned = nodes.filter((n) => !n.data?.isGhost)
         if (cleaned.length !== nodes.length) {
           setNodes(cleaned)
         }
         return
       }

       // Step 1: Preview (show ghost group)
       if (currentStep === 1) {
         const nodes = getNodes()
         const edges = getEdges()

         // Get selected nodes
         const selectedNodes = nodes.filter((n) => selectedNodeIds.includes(n.id) && !n.data?.isGhost)

         if (selectedNodes.length < 2) {
           console.warn('[GhostNodeRenderer] Not enough nodes selected for group')
           return
         }

         // Create ghost group with children
         const { nodes: ghostNodes, edges: ghostEdges } = createGhostGroup(selectedNodes, nodes, edges)

         // Remove real selected nodes (they'll be shown as ghost children)
         const nodesWithoutSelected = nodes.filter((n) => !selectedNodeIds.includes(n.id) || n.data?.isGhost)

         // Add ghost group (group first, then children)
         const updatedNodes = [...nodesWithoutSelected, ...ghostNodes]
         const updatedEdges = [...edges, ...ghostEdges]

         setNodes(updatedNodes)
         setEdges(updatedEdges)

         // Fit view to show ghost group
         fitView({
           padding: 0.2,
           duration: 500,
           nodes: ghostNodes,
         })
       }
     }, [isActive, entityType, currentStep, selectedNodeIds, getNodes, getEdges, setNodes, setEdges])

     // Cleanup on wizard cancel or completion
     useEffect(() => {
       if (!isActive && entityType === EntityType.GROUP) {
         // Remove all ghost nodes
         const nodes = getNodes()
         const cleaned = removeGhostGroup('ghost-group-*', nodes)
         setNodes(cleaned)
       }
     }, [isActive, entityType])

     return null
   }
   ```

2. **Update Component Tests**
   - Test Step 0: No ghost group visible
   - Test Step 1: Ghost group appears with children
   - Test Step 1: Selected nodes hidden, shown as ghost children
   - Test Step 1: Auto-included nodes shown in ghost group
   - Test cleanup on cancel
   - Accessibility test (unskipped)

**Acceptance Criteria**:

- [x] No ghost group in Step 0 (selection mode)
- [x] Ghost group appears in Step 1 (preview mode)
- [x] Selected nodes become ghost children
- [x] Auto-included nodes visible in ghost group
- [x] Viewport auto-fits to show ghost group
- [x] Cleanup works correctly
- [x] Tests passing

**Estimated Time**: 2 days

---

### ✅ Subtask 5: Group Configuration Component

**Objective**: Create configuration form for group settings (title, color)

**Why After Subtask 4**: Preview must work before configuration

**Files to Create**:

- `src/modules/Workspace/components/wizard/WizardGroupConfiguration.tsx`
- `src/modules/Workspace/components/wizard/WizardGroupConfiguration.spec.cy.tsx`

**Files to Modify**:

- `src/modules/Workspace/components/wizard/WizardConfigurationPanel.tsx`
- `src/locales/en/translation.json`

**Implementation Steps**:

1. **Create WizardGroupConfiguration Component**

   ```typescript
   interface WizardGroupConfigurationProps {
     onComplete: (config: GroupConfig) => void
     onBack: () => void
   }

   interface GroupConfig {
     title: string
     colorScheme: string
   }

   const WizardGroupConfiguration: FC<WizardGroupConfigurationProps> = ({
     onComplete,
     onBack
   }) => {
     const { t } = useTranslation()
     const [title, setTitle] = useState(t('workspace.grouping.untitled'))
     const [colorScheme, setColorScheme] = useState('blue')

     const colorOptions = [
       { value: 'blue', label: t('workspace.group.color.blue') },
       { value: 'green', label: t('workspace.group.color.green') },
       { value: 'red', label: t('workspace.group.color.red') },
       { value: 'purple', label: t('workspace.group.color.purple') },
       { value: 'orange', label: t('workspace.group.color.orange') },
     ]

     const handleSubmit = () => {
       onComplete({ title, colorScheme })
     }

     const isValid = title.trim().length > 0

     return (
       <VStack align="stretch" spacing={6} p={6}>
         <Heading size="md">
           {t('workspace.wizard.group.configTitle')}
         </Heading>

         <FormControl isRequired>
           <FormLabel>{t('workspace.wizard.group.titleLabel')}</FormLabel>
           <Input
             value={title}
             onChange={(e) => setTitle(e.target.value)}
             placeholder={t('workspace.wizard.group.titlePlaceholder')}
             aria-label={t('workspace.wizard.group.titleLabel')}
           />
           <FormHelperText>
             {t('workspace.wizard.group.titleHelp')}
           </FormHelperText>
         </FormControl>

         <FormControl>
           <FormLabel>{t('workspace.wizard.group.colorLabel')}</FormLabel>
           <RadioGroup value={colorScheme} onChange={setColorScheme}>
             <Stack direction="row" spacing={4}>
               {colorOptions.map(option => (
                 <Radio
                   key={option.value}
                   value={option.value}
                   colorScheme={option.value}
                 >
                   {option.label}
                 </Radio>
               ))}
             </Stack>
           </RadioGroup>
         </FormControl>

         <HStack justify="space-between" pt={4}>
           <Button variant="ghost" onClick={onBack}>
             {t('workspace.wizard.button.back')}
           </Button>
           <Button
             colorScheme="blue"
             onClick={handleSubmit}
             isDisabled={!isValid}
           >
             {t('workspace.wizard.button.complete')}
           </Button>
         </HStack>
       </VStack>
     )
   }
   ```

2. **Integrate into WizardConfigurationPanel**

   - Add route for EntityType.GROUP in step 2
   - Pass onComplete and onBack handlers
   - Store config in wizard store

3. **Add i18n Keys**

   ```json
   {
     "workspace.wizard.group.configTitle": "Configure Group",
     "workspace.wizard.group.titleLabel": "Group Title",
     "workspace.wizard.group.titlePlaceholder": "Enter group name",
     "workspace.wizard.group.titleHelp": "Give your group a descriptive name",
     "workspace.wizard.group.colorLabel": "Group Color",
     "workspace.group.color.blue": "Blue",
     "workspace.group.color.green": "Green",
     "workspace.group.color.red": "Red",
     "workspace.group.color.purple": "Purple",
     "workspace.group.color.orange": "Orange"
   }
   ```

4. **Write Component Tests**
   - Test rendering with default values
   - Test title input validation
   - Test color selection
   - Test Back button
   - Test Complete button (enabled/disabled)
   - Accessibility test (unskipped)

**Acceptance Criteria**:

- [x] Form shows title input and color selection
- [x] Title is required (complete button disabled if empty)
- [x] Color options displayed with visual previews
- [x] Back button returns to Step 1 (preview)
- [x] Complete button triggers group creation
- [x] Accessibility compliant
- [x] Tests passing

**Estimated Time**: 1.5 days

---

### ✅ Subtask 6: Wizard Completion Logic

**Objective**: Convert ghost group to real group on wizard completion

**Why After Subtask 5**: Needs configuration data

**Files to Modify**:

- `src/modules/Workspace/components/wizard/WizardGroupConfiguration.tsx`
- `src/modules/Workspace/hooks/useWorkspaceStore.ts` (if needed)

**Files to Create**:

- `src/modules/Workspace/components/wizard/utils/groupWizardCompletion.ts`
- `src/modules/Workspace/components/wizard/utils/groupWizardCompletion.spec.ts`

**Implementation Steps**:

1. **Create Group Wizard Completion Utility**

   ```typescript
   /**
    * Complete group wizard: convert ghost group to real group
    */
   export const completeGroupWizard = (
     selectedNodeIds: string[],
     config: GroupConfig,
     allNodes: Node[],
     allEdges: Edge[],
     theme: Partial<WithCSSVar<Dict>>
   ): {
     newGroupNode: Node
     newGroupEdge: Edge
     updatedNodes: Node[]
   } => {
     // Get selected nodes (original, non-ghost)
     const selectedNodes = allNodes.filter((n) => selectedNodeIds.includes(n.id) && !n.data?.isGhost)

     // Calculate auto-included nodes
     const autoIncludedNodes = getAutoIncludedNodes(selectedNodes, allNodes, allEdges)
     const allGroupNodes = [...selectedNodes, ...autoIncludedNodes]

     // Calculate group bounds
     const rect = getNodesBounds(allGroupNodes)
     const groupRect = getGroupBounds(rect)

     // Create real group using existing utility
     const { newGroupNode, newGroupEdge } = createGroup(allGroupNodes, groupRect, theme)

     // Apply user configuration
     newGroupNode.data.title = config.title
     newGroupNode.data.colorScheme = config.colorScheme

     // Update group children nodes with parentId
     const updatedNodes = allNodes.map((node) => {
       if (allGroupNodes.some((n) => n.id === node.id)) {
         return {
           ...node,
           parentId: newGroupNode.id,
           position: {
             // Make position relative to group
             x: node.position.x - groupRect.x,
             y: node.position.y - groupRect.y,
           },
           // Ensure not selected
           selected: false,
         }
       }
       return node
     })

     // Remove ghost nodes
     const nodesWithoutGhosts = updatedNodes.filter((n) => !n.data?.isGhost)

     // Add group node at the beginning (React Flow requirement)
     const finalNodes = [newGroupNode, ...nodesWithoutGhosts]

     return {
       newGroupNode,
       newGroupEdge,
       updatedNodes: finalNodes,
     }
   }
   ```

2. **Integrate Completion in WizardGroupConfiguration**

   ```typescript
   const handleComplete = () => {
     const config = { title, colorScheme }

     // Get workspace data
     const nodes = getNodes()
     const edges = getEdges()

     // Complete wizard
     const { newGroupNode, newGroupEdge, updatedNodes } = completeGroupWizard(
       selectedNodeIds,
       config,
       nodes,
       edges,
       theme
     )

     // Update workspace
     setNodes(updatedNodes)
     setEdges([...edges, newGroupEdge])

     // Show success toast
     toast({
       title: t('workspace.wizard.group.success.title'),
       description: t('workspace.wizard.group.success.description', {
         title: config.title,
       }),
       status: 'success',
     })

     // Complete wizard
     completeWizard()

     // Highlight new group briefly
     setTimeout(() => {
       setNodes((nodes) => nodes.map((n) => (n.id === newGroupNode.id ? { ...n, selected: true } : n)))
       setTimeout(() => {
         setNodes((nodes) => nodes.map((n) => ({ ...n, selected: false })))
       }, 1500)
     }, 100)
   }
   ```

3. **Add i18n Keys**

   ```json
   {
     "workspace.wizard.group.success.title": "Group Created",
     "workspace.wizard.group.success.description": "Group \"{{title}}\" has been created successfully"
   }
   ```

4. **Write Tests** (`groupWizardCompletion.spec.ts`)
   - Test completeGroupWizard with 2 adapters
   - Test completeGroupWizard with adapters + bridges
   - Test parentId relationships set correctly
   - Test positions relative to group
   - Test ghost nodes removed
   - Test group node order (first in array)
   - Test config applied to group

**Acceptance Criteria**:

- [x] Ghost group converted to real group
- [x] Selected nodes become children with parentId
- [x] Auto-included nodes become children
- [x] Child positions relative to group
- [x] Group edge to EDGE node created
- [x] Ghost nodes cleaned up
- [x] Success toast shown
- [x] Group briefly highlighted
- [x] Wizard closes
- [x] Tests passing

**Estimated Time**: 2 days

---

### ✅ Subtask 7: E2E Testing

**Objective**: Create end-to-end Cypress tests for complete group wizard flow

**Why After Subtask 6**: Needs complete implementation

**Files to Create**:

- `cypress/e2e/workspace/wizard/wizard-create-group.spec.cy.ts`

**Implementation Steps**:

1. **Create E2E Test Suite**

   ```typescript
   describe('Workspace Wizard - Group Creation', () => {
     beforeEach(() => {
       cy.visit('/workspace')
       cy.intercept('GET', '/api/v1/management/protocol-adapters/types', {
         fixture: 'protocol-adapters.json',
       })
       cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters', {
         fixture: 'adapters.json',
       })
     })

     it('should complete group wizard successfully', () => {
       // Step 0: Open wizard
       cy.get('[data-testid="create-new-button"]').click()
       cy.get('[data-testid="entity-group"]').click()

       // Step 0: Verify selection mode
       cy.get('[data-testid="wizard-progress"]')
         .should('contain', 'Step 1 of 3')
         .should('contain', 'Select nodes to group')

       // Step 0: Select 2 adapter nodes
       cy.get('[data-testid^="adapter-node-"]').first().click()
       cy.get('[data-testid^="adapter-node-"]').eq(1).click()

       // Step 0: Verify selection panel
       cy.get('[data-testid="wizard-selection-panel"]').should('contain', '2 nodes selected')

       // Step 0: Verify auto-included devices shown
       cy.get('[data-testid="auto-included-nodes"]').should('be.visible').should('contain', 'auto-included')

       // Step 0: Click Next
       cy.get('[data-testid="wizard-next"]').click()

       // Step 1: Verify preview mode
       cy.get('[data-testid="wizard-progress"]')
         .should('contain', 'Step 2 of 3')
         .should('contain', 'Review group preview')

       // Step 1: Verify ghost group visible
       cy.get('[data-node-type="CLUSTER_NODE"]').should('have.length', 1).should('have.attr', 'data-ghost', 'true')

       // Step 1: Verify selected nodes are children
       cy.get('[data-node-type="ADAPTER_NODE"][data-ghost="true"]').should('have.length', 2)

       // Step 1: Click Next
       cy.get('[data-testid="wizard-next"]').click()

       // Step 2: Verify configuration panel
       cy.get('[data-testid="wizard-progress"]')
         .should('contain', 'Step 3 of 3')
         .should('contain', 'Configure group settings')

       // Step 2: Fill in title
       cy.get('[aria-label="Group Title"]').clear().type('My Test Group')

       // Step 2: Select color
       cy.get('[value="green"]').click()

       // Step 2: Complete wizard
       cy.get('[data-testid="wizard-complete"]').click()

       // Verify: Real group created
       cy.get('[data-node-type="CLUSTER_NODE"]:not([data-ghost])')
         .should('have.length', 1)
         .should('contain', 'My Test Group')

       // Verify: Ghost nodes removed
       cy.get('[data-ghost="true"]').should('not.exist')

       // Verify: Success toast
       cy.get('.chakra-toast').should('contain', 'Group Created').should('contain', 'My Test Group')

       // Verify: Wizard closed
       cy.get('[data-testid="wizard-progress"]').should('not.exist')
     })

     it('should not allow selecting nodes already in groups', () => {
       // ... test implementation ...
     })

     it('should handle wizard cancellation', () => {
       // ... test implementation ...
     })

     it('should be accessible', () => {
       cy.injectAxe()
       cy.get('[data-testid="create-new-button"]').click()
       cy.checkAccessibility()
     })
   })
   ```

2. **Create Test Fixtures**
   - `cypress/fixtures/adapters-for-grouping.json`
   - Include 3-4 adapters with connected devices

**Acceptance Criteria**:

- [x] E2E test covers complete wizard flow
- [x] Test verifies selection constraints
- [x] Test verifies auto-included nodes
- [x] Test verifies ghost preview
- [x] Test verifies configuration
- [x] Test verifies group creation
- [x] Test handles edge cases (cancel, already grouped)
- [x] Accessibility test passes

**Estimated Time**: 1.5 days

---

### ✅ Subtask 8: Documentation & Polish

**Objective**: Update documentation and finalize implementation

**Why Last**: After full implementation and testing

**Files to Modify**:

- `.tasks/38139-wizard-group/TASK_SUMMARY.md` (create)
- `.tasks/38139-wizard-group/USER_DOCUMENTATION.md` (create)
- `README.md` (if needed)

**Files to Create**:

- `.tasks/38139-wizard-group/TASK_SUMMARY.md`
- `.tasks/38139-wizard-group/GROUP_GHOST_DESIGN.md` (technical doc)

**Implementation Steps**:

1. **Create Task Summary**

   - List all completed subtasks
   - Document key decisions
   - Note any deviations from plan
   - Metrics (files changed, tests added)

2. **Create User Documentation**

   - Follow USER_DOCUMENTATION_GUIDELINE.md format
   - Explain group wizard workflow
   - Highlight auto-inclusion feature
   - Provide troubleshooting tips

3. **Create Technical Design Doc**

   - Explain ghost group architecture
   - Document parent-child handling
   - Diagram showing Step 0 vs Step 1 states
   - Notes for future developers

4. **Polish Implementation**

   - Code review and refactoring
   - Ensure consistent code style
   - Add missing JSDoc comments
   - Verify all i18n keys present
   - Check TypeScript types

5. **Final Testing**
   - Run all tests (`pnpm test`)
   - Run E2E tests (`pnpm cypress:run`)
   - Manual testing in different scenarios
   - Accessibility audit

**Acceptance Criteria**:

- [x] Task summary complete and accurate
- [x] User documentation clear and helpful
- [x] Technical design documented
- [x] All tests passing (unit, component, E2E)
- [x] Code review completed
- [x] No TypeScript errors
- [x] Accessibility audit passed

**Estimated Time**: 1 day

---

## Risk Management

### High-Risk Areas

1. **React Flow Parent-Child Ordering**

   - **Risk**: Group node must be added before children, or layout breaks
   - **Mitigation**: Enforce ordering in `completeGroupWizard` utility
   - **Test**: Verify node array order in tests

2. **Ghost Group vs Real Nodes Conflict**

   - **Risk**: Selected nodes shown twice (as real + as ghost children)
   - **Mitigation**: Hide real nodes when ghost group appears in Step 1
   - **Test**: Verify node counts in each step

3. **Auto-Inclusion Edge Cases**
   - **Risk**: DEVICE/HOST not found, or multiple connections
   - **Mitigation**: Defensive programming, handle missing nodes gracefully
   - **Test**: Test with missing connections, multiple devices

### Medium-Risk Areas

1. **Wizard State Consistency**

   - **Risk**: selectedNodeIds out of sync with actual selection
   - **Mitigation**: Use wizard store as single source of truth
   - **Test**: Test navigation back/forward through steps

2. **Performance with Large Graphs**
   - **Risk**: Creating ghost group slow with 100+ nodes
   - **Mitigation**: Optimize `getAutoIncludedNodes` with memoization
   - **Test**: Performance test with large fixture

---

## Dependencies

### Internal Dependencies

- Task 38111 (Workspace Operation Wizard) - REQUIRED
- Existing `createGroup` utility - REUSE
- Existing `getGroupBounds` utility - REUSE
- WizardSelectionRestrictions component - EXTEND
- GhostNodeRenderer component - EXTEND

### External Dependencies

- React Flow v11+ (parent-child groups)
- Chakra UI v2+ (form components)
- i18next v23+ (translations)

---

## Timeline

### Week 1 (Days 1-5)

- Monday-Tuesday: Subtask 1 (Selection Constraints)
- Wednesday: Subtask 2 (Auto-Inclusion UI)
- Thursday-Friday: Subtask 3 (Ghost Group Factory)

### Week 2 (Days 6-10)

- Monday-Tuesday: Subtask 4 (Ghost Group Renderer)
- Wednesday: Subtask 5 (Configuration Form)
- Thursday-Friday: Subtask 6 (Wizard Completion)

### Week 3 (Days 11-13)

- Monday: Subtask 7 (E2E Testing)
- Tuesday: Subtask 8 (Documentation & Polish)
- Wednesday: Buffer / Code Review / Handoff

**Total Estimated Duration**: 13 days (2-3 weeks with buffer)

---

## Success Criteria

- [x] Group wizard accessible from "Create New" dropdown
- [x] User can select 2+ nodes (adapters, bridges, groups)
- [x] Nodes already in groups cannot be selected
- [x] DEVICE/HOST nodes auto-included and visible
- [x] Step 1 shows ghost group preview with children
- [x] Step 2 allows configuring title and color
- [x] Wizard completion creates real group with all features
- [x] All nodes have correct parentId relationships
- [x] Group node ordered before children in nodes array
- [x] Existing group functionality remains unchanged
- [x] All tests passing (unit, component, E2E, accessibility)
- [x] Documentation complete and accurate

---

**Plan Created**: November 21, 2025  
**Plan Status**: Ready for Implementation  
**Next Action**: Begin Subtask 1 (Group Selection Constraints)
