# Task 38111: Workspace Operation Wizard - Architecture

**Task ID:** 38111  
**Created:** November 10, 2025  
**Last Updated:** November 11, 2025

---

## 🌟 Key Achievement: Reusable Interactive Selection System

**Status:** ✅ Production-Ready  
**Design Document:** [SUBTASK_9.25_SELECTION_DESIGN.md](./SUBTASK_9.25_SELECTION_DESIGN.md)  
**Completed:** November 11, 2025

### Overview

The **Interactive Selection System** is a fully reusable, declarative pattern for wizard steps that require users to select nodes from the canvas. This system powers the Combiner, Asset Mapper, and Group wizards, providing:

- **Visual Filtering:** Hides non-selectable nodes, highlights valid targets
- **Ghost Nodes & Edges:** Real-time preview of connections as user selects
- **Floating Panel:** Non-blocking UI showing selected nodes with validation
- **Constraint Validation:** Declarative min/max, node type filtering
- **Accessibility:** Proper ARIA labels, keyboard navigation, screen reader support

### Architecture Components

```
┌─────────────────────────────────────────────────────────────────┐
│                    Selection System Flow                         │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  1. Wizard Metadata (Declarative Constraints)                   │
│     ┌────────────────────────────────────────────────┐          │
│     │ selectionConstraints: {                        │          │
│     │   minNodes: 2,                                 │          │
│     │   maxNodes: Infinity,                          │          │
│     │   allowedNodeTypes: ['ADAPTER_NODE']           │          │
│     │ }                                              │          │
│     └────────────────────────────────────────────────┘          │
│                          ↓                                       │
│  2. WizardSelectionRestrictions (Visual Filtering)              │
│     - Hides non-allowed nodes                                   │
│     - Highlights selectable targets (blue border)               │
│     - Creates ghost edges (selected → ghost combiner)           │
│     - Manages ghost lifecycle (persist until wizard end)        │
│                          ↓                                       │
│  3. ReactFlowWrapper.onNodeClick (Event Handler)                │
│     - Checks constraints                                        │
│     - Toggles selection                                         │
│     - Enforces max nodes                                        │
│     - Shows toast on violation                                  │
│                          ↓                                       │
│  4. WizardSelectionPanel (Floating UI)                          │
│     - Lists selected nodes (scrollable)                         │
│     - Shows validation status                                   │
│     - Provides "Next" button (disabled until valid)             │
│     - Doesn't block canvas interaction                          │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Key Design Decisions

1. **Declarative Constraints:** Wizard metadata defines what's selectable - no imperative code needed
2. **React Flow Panel:** Floating UI instead of Drawer - doesn't block canvas
3. **Ghost Persistence:** Ghost nodes/edges stay visible throughout wizard (not just selection step)
4. **Selectable Ghost:** Ghost combiner can be clicked to highlight all edges
5. **Proper Pluralization:** Uses i18next count-based pluralization (no "node(s)")

### Usage Pattern

```typescript
// In wizardMetadata.ts - Define constraints declaratively
{
  index: 0,
  requiresSelection: true,
  selectionConstraints: {
    minNodes: 2,
    allowedNodeTypes: ['ADAPTER_NODE', 'BRIDGE_NODE'],
  },
}

// That's it! System handles:
// ✅ Visual filtering
// ✅ Ghost edges
// ✅ Validation
// ✅ UI feedback
```

### Benefits

- **Reusable:** Works for any wizard needing selection (Combiner, Asset Mapper, Group)
- **Declarative:** Just specify constraints, system handles UX
- **Accessible:** Full ARIA support, keyboard navigation
- **Visual:** Real-time ghost preview, edge highlighting
- **Maintainable:** Clean separation of concerns

**See:** [SUBTASK_9.25_SELECTION_DESIGN.md](./SUBTASK_9.25_SELECTION_DESIGN.md) for complete design documentation.

---

## Table of Contents

1. [Overview](#overview)
2. [Core Architecture](#core-architecture)
3. [State Management](#state-management)
4. [Component Hierarchy](#component-hierarchy)
5. [Data Flow](#data-flow)
6. [Ghost Node System](#ghost-node-system)
7. [Integration Strategy](#integration-strategy)
8. [Testing Architecture](#testing-architecture)
9. [Accessibility Architecture](#accessibility-architecture)
10. [Performance Considerations](#performance-considerations)

---

## Overview

The Workspace Operation Wizard is a multi-step, interactive system for creating entities and integration points directly within the workspace canvas. It combines visual feedback (ghost nodes), step-by-step guidance (progress bar), and configuration interfaces (side panels) into a cohesive user experience.

### Design Goals

1. **Consistency:** Unified creation experience across all entity types
2. **Discoverability:** Easy to find and understand what can be created
3. **Guidance:** Clear step-by-step process with visual feedback
4. **Reusability:** Leverage existing forms and components
5. **Extensibility:** Easy to add new wizard types
6. **Accessibility:** Full keyboard navigation and screen reader support

---

## Core Architecture

### Four-Component System

```
┌─────────────────────────────────────────────────────────────┐
│                     Workspace Canvas                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  1. TRIGGER (CanvasToolbar)                          │  │
│  │     CreateEntityButton                               │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  2. PROGRESS BAR (React Flow Panel)                  │  │
│  │     WizardProgressBar                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│         ┌─ ─ ─ ─ ─ ┐       ┌─ ─ ─ ─ ─ ┐                   │
│         │ 3. GHOST │   →   │  GHOST   │                   │
│         │  NODES   │       │  NODES   │                   │
│         └─ ─ ─ ─ ─ ┘       └─ ─ ─ ─ ─ ┘                   │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                                    │
                                    ├─────────────────────┐
                                    │ 4. CONFIGURATION    │
                                    │    PANEL (Drawer)   │
                                    └─────────────────────┘
```

### Component Responsibilities

1. **Trigger (CreateEntityButton)**

   - Entry point for wizard
   - Entity/integration point selection menu
   - Keyboard accessible dropdown

2. **Progress Bar (WizardProgressBar)**

   - Visual progress indicator
   - Current step description
   - Cancel button
   - Screen reader announcements

3. **Ghost Nodes (GhostNode)**

   - Visual preview of entities being created
   - Non-interactive placeholders
   - Positioned via layout engine
   - Removed on cancel/completion

4. **Configuration Panel**
   - Reused form components
   - Wizard-aware context
   - Validation and submission
   - Side drawer pattern

---

## State Management

### Zustand Store Architecture

**Decision:** Use Zustand for wizard state management

**Rationale:**

- Already used in workspace (`useWorkspaceStore`)
- Better performance than Context for frequent updates
- No prop drilling
- DevTools support
- Easy to access outside React tree

### Store Structure

```typescript
interface WizardStore {
  // Core state
  isActive: boolean
  entityType: EntityType | IntegrationPointType | null
  currentStep: number
  totalSteps: number

  // Selection state
  selectedNodeIds: string[]
  selectionConstraints: SelectionConstraints | null

  // Ghost nodes
  ghostNodes: GhostNode[]
  ghostEdges: GhostEdge[]

  // Configuration
  configurationData: Record<string, unknown>
  isConfigurationValid: boolean

  // UI state
  isSidePanelOpen: boolean
  errorMessage: string | null

  // Actions
  actions: {
    startWizard: (type: EntityType | IntegrationPointType) => void
    cancelWizard: () => void
    nextStep: () => void
    previousStep: () => void
    completeWizard: () => Promise<void>

    selectNode: (nodeId: string) => void
    deselectNode: (nodeId: string) => void
    clearSelection: () => void

    updateConfiguration: (data: Partial<ConfigurationData>) => void
    validateConfiguration: () => boolean

    setError: (message: string) => void
    clearError: () => void
  }
}
```

### Store Organization

```typescript
// File: src/modules/Workspace/hooks/useWizardStore.ts
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

export const useWizardStore = create<WizardStore>()(
  devtools(
    (set, get) => ({
      // Initial state
      isActive: false,
      entityType: null,
      currentStep: 0,
      totalSteps: 0,
      selectedNodeIds: [],
      selectionConstraints: null,
      ghostNodes: [],
      ghostEdges: [],
      configurationData: {},
      isConfigurationValid: false,
      isSidePanelOpen: false,
      errorMessage: null,

      // Actions
      actions: {
        startWizard: (type) => {
          const metadata = WIZARD_REGISTRY[type]
          set({
            isActive: true,
            entityType: type,
            currentStep: 0,
            totalSteps: metadata.steps.length,
            // Reset other state...
          })
        },

        cancelWizard: () => {
          // Clean up ghost nodes
          const { ghostNodes } = get()
          // ... cleanup logic

          set({
            isActive: false,
            entityType: null,
            currentStep: 0,
            totalSteps: 0,
            selectedNodeIds: [],
            ghostNodes: [],
            ghostEdges: [],
            configurationData: {},
            errorMessage: null,
          })
        },

        // ... other actions
      },
    }),
    { name: 'WizardStore' }
  )
)

// Convenience hooks
export const useWizardState = () =>
  useWizardStore((state) => ({
    isActive: state.isActive,
    entityType: state.entityType,
    currentStep: state.currentStep,
    totalSteps: state.totalSteps,
  }))

export const useWizardActions = () => useWizardStore((state) => state.actions)

export const useWizardSelection = () =>
  useWizardStore((state) => ({
    selectedNodeIds: state.selectedNodeIds,
    selectionConstraints: state.selectionConstraints,
    selectNode: state.actions.selectNode,
    deselectNode: state.actions.deselectNode,
    clearSelection: state.actions.clearSelection,
  }))
```

---

## Component Hierarchy

```
WizardOrchestrator
├── WizardProgressBar
│   ├── ProgressIndicator
│   ├── StepDescription
│   └── CancelButton
│
├── EntityWizard (dynamic based on type)
│   ├── AdapterWizard
│   │   ├── PreviewStep
│   │   ├── TypeSelectionStep
│   │   └── ConfigurationStep
│   │
│   ├── BridgeWizard
│   │   ├── PreviewStep
│   │   └── ConfigurationStep
│   │
│   ├── CombinerWizard
│   │   ├── SelectionStep
│   │   ├── PreviewStep
│   │   └── ConfigurationStep
│   │
│   └── [Other wizards...]
│
├── GhostNodeRenderer
│   ├── GhostNode (multiple)
│   └── GhostEdge (multiple)
│
└── ConfigurationPanelRouter
    └── [Dynamically loaded form component]
```

### Wizard Orchestrator

**Purpose:** Top-level coordinator that manages wizard lifecycle

```typescript
// File: src/modules/Workspace/components/wizard/WizardOrchestrator.tsx
import { FC } from 'react'
import { useWizardState, useWizardActions } from '../../hooks/useWizardStore'
import WizardProgressBar from './steps/WizardProgressBar'
import GhostNodeRenderer from './preview/GhostNodeRenderer'
import { WIZARD_REGISTRY } from './utils/wizardMetadata'

const WizardOrchestrator: FC = () => {
  const { isActive, entityType, currentStep } = useWizardState()
  const { cancelWizard } = useWizardActions()

  if (!isActive || !entityType) return null

  const metadata = WIZARD_REGISTRY[entityType]
  const WizardComponent = metadata.component

  return (
    <>
      <WizardProgressBar />
      <GhostNodeRenderer />
      <WizardComponent />
    </>
  )
}

export default WizardOrchestrator
```

---

## Data Flow

### Wizard Lifecycle Flow

```
1. User Clicks "Create New > Adapter"
   ↓
2. CreateEntityButton → wizardActions.startWizard(EntityType.ADAPTER)
   ↓
3. WizardStore updates:
   - isActive = true
   - entityType = ADAPTER
   - currentStep = 0
   - totalSteps = 3
   ↓
4. WizardOrchestrator renders:
   - WizardProgressBar (shows "Step 1 of 3: Review preview")
   - AdapterWizard component
   ↓
5. AdapterWizard Step 1 (Preview):
   - Creates ghost DEVICE, ADAPTER, EDGE nodes
   - Positions them via layout engine
   - Adds to wizardStore.ghostNodes
   - User sees preview with "Continue" button
   ↓
6. User clicks "Continue" → wizardActions.nextStep()
   ↓
7. AdapterWizard Step 2 (Type Selection):
   - Opens side panel with protocol type selector
   - User selects "OPC UA"
   - wizardActions.updateConfiguration({ type: 'OPC_UA' })
   ↓
8. User clicks "Next" → wizardActions.nextStep()
   ↓
9. AdapterWizard Step 3 (Configuration):
   - Opens side panel with OPC UA form
   - User fills form fields
   - wizardActions.updateConfiguration({ name, host, port, ... })
   ↓
10. User clicks "Create" → wizardActions.completeWizard()
    ↓
11. completeWizard():
    - Validates configuration
    - Calls API to create adapter
    - On success:
      - Removes ghost nodes
      - Adds real nodes to workspace
      - Shows success toast
      - wizardActions.cancelWizard() (cleanup)
    - On error:
      - Shows error message
      - Keeps wizard open for fixes
```

### State Update Patterns

**Starting Wizard:**

```typescript
// User action → State update → UI update
User clicks "Adapter"
  → startWizard(EntityType.ADAPTER)
  → isActive=true, entityType=ADAPTER, step=0
  → WizardOrchestrator renders AdapterWizard
  → Progress bar shows "Step 1 of 3"
```

**Node Selection (Combiner/Group):**

```typescript
// Interactive selection → State tracking → Validation
User clicks node on canvas
  → selectNode(nodeId)
  → selectedNodeIds.push(nodeId)
  → Validate against constraints (min/max, types)
  → Update canProceed flag
  → Enable/disable "Continue" button
```

**Configuration Updates:**

```typescript
// Form changes → Partial updates → Validation
User changes form field
  → updateConfiguration({ fieldName: newValue })
  → Merge into configurationData
  → Validate configuration
  → Update isConfigurationValid flag
  → Enable/disable "Create" button
```

---

## Ghost Node System

### Purpose

Ghost nodes provide immediate visual feedback showing what will be created before committing. They help users understand the topology changes the wizard will make.

### Ghost Node Characteristics

- **Visual:** 50% opacity, dashed border, lighter background
- **Non-interactive:** Cannot be selected, moved, or edited
- **Positioned:** Via layout engine for consistent placement
- **Temporary:** Removed on cancel or replaced on completion
- **Connected:** Show edges to existing nodes

### Implementation

```typescript
// File: src/modules/Workspace/components/wizard/preview/GhostNode.tsx
import { FC } from 'react'
import { Node } from 'reactflow'
import { Box, Badge } from '@chakra-ui/react'
import { useTranslation } from 'react-i18next'

interface GhostNodeProps {
  id: string
  type: NodeType
  position: { x: number; y: number }
  data: Partial<NodeData>
}

const GhostNode: FC<GhostNodeProps> = ({ id, type, position, data }) => {
  const { t } = useTranslation()

  return (
    <Node
      id={id}
      type={type}
      position={position}
      data={{
        ...data,
        isGhost: true,
      }}
      selectable={false}
      draggable={false}
      connectable={false}
      style={{
        opacity: 0.5,
        border: '2px dashed var(--chakra-colors-gray-400)',
      }}
      aria-label={t('workspace.wizard.ghost.ariaLabel', {
        entityType: data.label
      })}
    >
      {/* Node content */}
      <Badge
        position="absolute"
        top="-10px"
        right="-10px"
        colorScheme="blue"
        fontSize="xs"
      >
        {t('workspace.wizard.ghost.label')}
      </Badge>
    </Node>
  )
}

export default GhostNode
```

### Ghost Node Management

```typescript
// File: src/modules/Workspace/components/wizard/preview/GhostNodeRenderer.tsx
import { FC, useEffect } from 'react'
import { useReactFlow } from 'reactflow'
import { useWizardStore } from '../../hooks/useWizardStore'

const GhostNodeRenderer: FC = () => {
  const { setNodes, setEdges, getNodes, getEdges } = useReactFlow()
  const ghostNodes = useWizardStore((state) => state.ghostNodes)
  const ghostEdges = useWizardStore((state) => state.ghostEdges)

  useEffect(() => {
    // Add ghost nodes to canvas
    const existingNodes = getNodes()
    const allNodes = [...existingNodes, ...ghostNodes]
    setNodes(allNodes)

    // Add ghost edges
    const existingEdges = getEdges()
    const allEdges = [...existingEdges, ...ghostEdges]
    setEdges(allEdges)

    // Cleanup on unmount
    return () => {
      // Remove ghost nodes
      setNodes(existingNodes)
      setEdges(existingEdges)
    }
  }, [ghostNodes, ghostEdges])

  return null // No UI, just manages React Flow state
}

export default GhostNodeRenderer
```

### Positioning Strategy

**Decision:** Use existing layout engine for ghost node placement

```typescript
// File: src/modules/Workspace/components/wizard/utils/ghostNodePositioning.ts
import { useLayoutEngine } from '../../hooks/useLayoutEngine'
import { Node } from 'reactflow'

export const createPositionedGhostNodes = (entityType: EntityType, existingNodes: Node[]): GhostNode[] => {
  const { calculateNodePosition } = useLayoutEngine()

  // Generate ghost nodes based on entity type
  const ghostTemplates = generateGhostNodesForType(entityType)

  // Position each ghost node
  const positionedGhosts = ghostTemplates.map((ghost, index) => {
    const position = calculateNodePosition(ghost, [...existingNodes, ...ghostTemplates.slice(0, index)])

    return {
      ...ghost,
      position,
    }
  })

  return positionedGhosts
}

const generateGhostNodesForType = (entityType: EntityType): GhostNode[] => {
  switch (entityType) {
    case EntityType.ADAPTER:
      return [
        { id: 'ghost-device', type: 'DEVICE_NODE', data: { label: 'Device' } },
        { id: 'ghost-adapter', type: 'ADAPTER_NODE', data: { label: 'Adapter' } },
      ]

    case EntityType.BRIDGE:
      return [
        { id: 'ghost-host', type: 'HOST_NODE', data: { label: 'Remote Broker' } },
        { id: 'ghost-bridge', type: 'BRIDGE_NODE', data: { label: 'Bridge' } },
      ]

    // ... other entity types

    default:
      return []
  }
}
```

---

## Integration Strategy

### Reusing Existing Forms

**Goal:** Minimize code duplication, leverage existing validation and submission logic

**Approach:** Add optional `wizardContext` prop to existing form components

```typescript
// File: src/modules/ProtocolAdapters/components/AdapterForm.tsx (modified)

interface WizardContext {
  onComplete: (data: AdapterConfig) => void
  onCancel: () => void
  ghostNodeId?: string
  mode: 'wizard'
}

interface AdapterFormProps {
  mode: 'create' | 'edit'
  initialData?: AdapterConfig
  wizardContext?: WizardContext  // NEW: Optional wizard context
  onSubmit?: (data: AdapterConfig) => Promise<void>
}

const AdapterForm: FC<AdapterFormProps> = ({
  mode,
  initialData,
  wizardContext,
  onSubmit
}) => {
  const handleSubmit = async (data: AdapterConfig) => {
    if (wizardContext) {
      // Wizard mode: pass data back to wizard orchestrator
      wizardContext.onComplete(data)
    } else if (onSubmit) {
      // Normal mode: submit directly
      await onSubmit(data)
    }
  }

  const handleCancel = () => {
    if (wizardContext) {
      wizardContext.onCancel()
    } else {
      // Normal cancel logic
      navigate(-1)
    }
  }

  return (
    <form onSubmit={handleFormSubmit}>
      {/* Existing form fields */}

      <ButtonGroup>
        <Button variant="ghost" onClick={handleCancel}>
          {t('action.cancel')}
        </Button>
        <Button variant="primary" type="submit">
          {wizardContext ? t('action.next') : t('action.save')}
        </Button>
      </ButtonGroup>
    </form>
  )
}
```

### Configuration Panel Router

```typescript
// File: src/modules/Workspace/components/wizard/utils/configurationPanelRouter.tsx
import { FC } from 'react'
import { Drawer, DrawerContent, DrawerHeader, DrawerBody } from '@chakra-ui/react'
import { useWizardStore, useWizardActions } from '../../hooks/useWizardStore'
import AdapterForm from '@/modules/ProtocolAdapters/components/AdapterForm'
import BridgeForm from '@/modules/Bridges/components/BridgeForm'
// ... other imports

const ConfigurationPanelRouter: FC = () => {
  const entityType = useWizardStore((state) => state.entityType)
  const configData = useWizardStore((state) => state.configurationData)
  const isSidePanelOpen = useWizardStore((state) => state.isSidePanelOpen)
  const { updateConfiguration, cancelWizard } = useWizardActions()

  if (!entityType) return null

  const wizardContext = {
    onComplete: updateConfiguration,
    onCancel: cancelWizard,
    mode: 'wizard' as const,
  }

  const renderForm = () => {
    switch (entityType) {
      case EntityType.ADAPTER:
        return (
          <AdapterForm
            mode="create"
            initialData={configData}
            wizardContext={wizardContext}
          />
        )

      case EntityType.BRIDGE:
        return (
          <BridgeForm
            mode="create"
            initialData={configData}
            wizardContext={wizardContext}
          />
        )

      // ... other entity types

      default:
        return null
    }
  }

  return (
    <Drawer isOpen={isSidePanelOpen} onClose={cancelWizard} size="lg">
      <DrawerContent>
        <DrawerHeader>
          {t('workspace.wizard.steps.configuration.title', {
            entityType: t('workspace.wizard.entityType.name', { context: entityType })
          })}
        </DrawerHeader>
        <DrawerBody>
          {renderForm()}
        </DrawerBody>
      </DrawerContent>
    </Drawer>
  )
}

export default ConfigurationPanelRouter
```

---

## Testing Architecture

### Pragmatic Testing Strategy

**Principle:** All components have tests, but only accessibility tests are unskipped initially

**Structure:**

```typescript
// Every component test file follows this pattern
describe('ComponentName', () => {
  // ✅ ALWAYS UNSKIPPED - Must pass
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<ComponentName {...props} />)
    cy.checkAccessibility()
  })

  // ⏭️ SKIPPED during initial development
  it.skip('should render correctly', () => {
    // Test implementation...
  })

  it.skip('should handle user interactions', () => {
    // Test implementation...
  })

  it.skip('should validate inputs', () => {
    // Test implementation...
  })
})
```

**Benefits:**

- Ensures accessibility from day one
- Documents expected behavior
- Allows rapid development
- Tests ready to unskip when needed

### Test Organization

```
src/modules/Workspace/components/wizard/
├── CreateEntityButton.spec.cy.tsx
├── WizardOrchestrator.spec.cy.tsx
├── steps/
│   ├── WizardProgressBar.spec.cy.tsx
│   ├── SelectionStep.spec.cy.tsx
│   └── ConfigurationStep.spec.cy.tsx
├── preview/
│   ├── GhostNode.spec.cy.tsx
│   └── GhostNodeRenderer.spec.cy.tsx
├── entity-wizards/
│   ├── AdapterWizard.spec.cy.tsx
│   ├── BridgeWizard.spec.cy.tsx
│   └── [other wizards].spec.cy.tsx
└── integration-wizards/
    ├── TagWizard.spec.cy.tsx
    └── [other integration wizards].spec.cy.tsx
```

---

## Accessibility Architecture

### WCAG 2.1 Level AA Compliance

**Critical Requirements:**

1. **Keyboard Navigation**

   - All controls via Tab/Shift+Tab
   - Enter to activate/confirm
   - Escape to cancel
   - Arrow keys for menus

2. **Focus Management**

   - Focus trap in side panels
   - Focus return on close
   - Clear focus indicators

3. **Screen Reader Support**

   - ARIA labels on all controls
   - Live regions for status updates
   - Semantic HTML structure

4. **Visual Design**
   - 4.5:1 contrast ratio minimum
   - No color-only information
   - Visible focus indicators

### ARIA Patterns

```typescript
// Trigger Button
<MenuButton
  aria-label={t('workspace.wizard.trigger.buttonAriaLabel')}
  aria-haspopup="menu"
  aria-expanded={isOpen}
>
  {t('workspace.wizard.trigger.buttonLabel')}
</MenuButton>

// Progress Bar
<Box
  role="status"
  aria-live="polite"
  aria-label={t('workspace.wizard.progress.ariaLabel')}
>
  {t('workspace.wizard.progress.stepCounter', { current, total })}
</Box>

// Ghost Node
<Node
  aria-label={t('workspace.wizard.ghost.ariaLabel', { entityType })}
  aria-describedby="ghost-tooltip"
  role="img"
>
  <Tooltip id="ghost-tooltip">
    {t('workspace.wizard.ghost.tooltip')}
  </Tooltip>
</Node>

// Selection Mode
<Box
  role="region"
  aria-label={t('workspace.wizard.selection.instruction', { nodeType })}
  aria-live="polite"
>
  {t('workspace.wizard.selection.selected', { count })}
</Box>
```

---

## Performance Considerations

### Optimization Strategies

1. **Ghost Node Rendering**

   - Limit max 5 ghost nodes per wizard
   - Use React.memo for GhostNode component
   - Debounce position calculations

2. **State Updates**

   - Batch related updates
   - Use Zustand selectors to prevent unnecessary re-renders
   - Minimize state size (IDs, not full objects)

3. **Form Loading**

   - Lazy load configuration forms
   - Code split wizard components
   - Preload common forms

4. **Canvas Performance**
   - Remove ghost nodes immediately on cancel
   - Don't re-layout on every state change
   - Use React Flow's built-in optimizations

### Monitoring

```typescript
// Performance markers
const startWizard = (type: EntityType) => {
  performance.mark('wizard-start')

  // ... wizard logic

  performance.mark('wizard-ghosts-rendered')
  performance.measure('wizard-init', 'wizard-start', 'wizard-ghosts-rendered')
}

const completeWizard = async () => {
  performance.mark('wizard-complete-start')

  // ... API call, cleanup

  performance.mark('wizard-complete-end')
  performance.measure('wizard-completion', 'wizard-complete-start', 'wizard-complete-end')

  // Log to analytics
  const measure = performance.getEntriesByName('wizard-completion')[0]
  console.log(`Wizard completed in ${measure.duration}ms`)
}
```

---

## Future Considerations

### Extensibility Points

1. **New Entity Types**

   - Add to enum
   - Create metadata entry
   - Implement wizard component
   - Add translations

2. **Custom Steps**

   - Step registry system
   - Pluggable step components
   - Configuration-driven workflows

3. **Wizard Templates**

   - Reusable step sequences
   - Shareable configurations
   - Admin customization

4. **Advanced Features**
   - Multi-entity creation (bulk)
   - Wizard chaining
   - Template library
   - Undo/redo support

---

**Document End**
