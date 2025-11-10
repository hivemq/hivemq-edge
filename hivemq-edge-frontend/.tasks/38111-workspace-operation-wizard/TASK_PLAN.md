# Task 33811: Workspace Operation Wizard - Implementation Plan

**Created:** November 10, 2025  
**Task ID:** 33811  
**Status:** Planning Phase

---

## Executive Summary

This task implements a comprehensive wizard system for creating and modifying entities and integration points directly within the workspace canvas. The wizard provides a consistent, guided experience that replaces the current fragmented approach where some entities can only be created in the workspace while others require navigation to different pages.

### Key Design Principles

1. **Progressive Disclosure**: Start simple, show complexity only when needed
2. **Visual Feedback**: Ghost nodes and progress indicators guide users through multi-step processes
3. **Accessibility First**: Full keyboard navigation, screen reader support, ARIA labels
4. **Reusability**: Leverage existing forms and components from other modules
5. **Extensibility**: Future-proof architecture for adding new entity types
6. **Consistency**: Unified creation experience across all entity types

---

## Architecture Overview

### Four Core Components

```
┌─────────────────────────────────────────────────────────────┐
│                     WORKSPACE CANVAS                         │
│                                                              │
│  ┌────────────────┐                                         │
│  │  TRIGGER       │  ← Button in CanvasToolbar              │
│  │  "Create New"  │                                         │
│  └────────────────┘                                         │
│                                                              │
│  ┌────────────────────────────────────────────────────┐    │
│  │  PROGRESS BAR (React Flow Panel)                    │    │
│  │  Step 2 of 4: Configure adapter...     [Cancel]    │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│         ┌──────────┐         ┌──────────┐                  │
│         │  GHOST   │    →    │  GHOST   │                  │
│         │  DEVICE  │         │ ADAPTER  │                  │
│         └──────────┘         └──────────┘                  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                                    │
                                    ├─────────────────────┐
                                    │ CONFIGURATION PANEL │
                                    │   (Side Drawer)     │
                                    │                     │
                                    │  [Form Content]     │
                                    │                     │
                                    │  [Cancel] [Create]  │
                                    └─────────────────────┘
```

---

## Implementation Phases

### Phase 1: Foundation & Adapter Wizard (Priority 1)

**Goal:** Establish core architecture and complete the adapter creation flow end-to-end.

**Deliverables:**

1. Wizard context and state management
2. Trigger button with entity type selector
3. Progress bar component
4. Ghost node system for visual preview
5. Full adapter creation flow
6. Configuration panel integration

**Timeline:** Subtasks 1-7

---

### Phase 2: Entity Wizards Expansion (Priority 2)

**Goal:** Add remaining entity types using the established patterns.

**Deliverables:**

1. Bridge wizard
2. Combiner wizard (with source selection)
3. Asset Mapper wizard (with Pulse Agent requirement)
4. Group wizard (with multi-selection)

**Timeline:** Subtasks 8-11

---

### Phase 3: Integration Point Wizards (Priority 3)

**Goal:** Support creation of integration points on existing entities.

**Deliverables:**

1. TAG wizard (device tags)
2. TOPIC FILTER wizard (edge broker filters)
3. DATA MAPPING wizards (northbound/southbound)
4. DATA COMBINING wizard (combiner mappings)

**Timeline:** Subtasks 12-16

---

### Phase 4: Polish & Enhancement (Priority 4)

**Goal:** Refinements, edge cases, and user experience improvements.

**Deliverables:**

1. Error handling and validation
2. Keyboard shortcuts
3. Animation polish
4. Documentation
5. User testing feedback

**Timeline:** Subtasks 17-20

---

## Detailed Subtask Breakdown

### Subtask 1: Wizard State Management & Types

**File:** `src/modules/Workspace/components/wizard/hooks/useWizardState.ts`

**Purpose:** Central state management for wizard lifecycle

**State Interface:**

```typescript
interface WizardState {
  // Core state
  isActive: boolean
  entityType: EntityType | IntegrationPointType | null
  currentStep: number
  totalSteps: number

  // Selection state (for multi-step wizards)
  selectedNodeIds: string[]

  // Ghost nodes
  ghostNodes: GhostNode[]

  // Configuration state
  configurationData: Partial<EntityConfiguration>

  // UI state
  isSidePanelOpen: boolean
  canProceed: boolean
}

interface WizardActions {
  startWizard: (type: EntityType | IntegrationPointType) => void
  cancelWizard: () => void
  nextStep: () => void
  previousStep: () => void
  completeWizard: () => void
  selectNode: (nodeId: string) => void
  updateConfiguration: (data: Partial<EntityConfiguration>) => void
}
```

**Implementation Notes:**

- Use Zustand or Context + useReducer
- Persist minimal state only
- Clear state on wizard completion/cancellation
- Provide utility hooks for common operations

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ State transitions (skipped)
- ⏭️ Multi-step navigation (skipped)
- ⏭️ Configuration updates (skipped)

---

### Subtask 2: Entity Type Metadata & Registry

**File:** `src/modules/Workspace/components/wizard/utils/wizardMetadata.ts`

**Purpose:** Centralized metadata for all wizard types

**Structure:**

```typescript
enum EntityType {
  ADAPTER = 'ADAPTER',
  BRIDGE = 'BRIDGE',
  COMBINER = 'COMBINER',
  ASSET_MAPPER = 'ASSET_MAPPER',
  GROUP = 'GROUP',
}

enum IntegrationPointType {
  TAG = 'TAG',
  TOPIC_FILTER = 'TOPIC_FILTER',
  DATA_MAPPING_NORTH = 'DATA_MAPPING_NORTH',
  DATA_MAPPING_SOUTH = 'DATA_MAPPING_SOUTH',
  DATA_COMBINING = 'DATA_COMBINING',
}

interface WizardMetadata {
  type: EntityType | IntegrationPointType
  category: 'entity' | 'integration'
  icon: IconType
  requiresSelection: boolean
  requiresGhost: boolean
  steps: WizardStepConfig[]
}

// Registry
export const WIZARD_REGISTRY: Record<string, WizardMetadata>
```

**i18n Keys (using context):**

```typescript
// Translation keys (PLAIN STRINGS with context)
t('workspace.wizard.entityType.name', { context: type })
t('workspace.wizard.entityType.description', { context: type })
t('workspace.wizard.entityType.step.title', { context: `${type}_${stepIndex}` })
```

**Translation Structure:**

```json
{
  "workspace": {
    "wizard": {
      "entityType": {
        "name_ADAPTER": "Adapter",
        "name_BRIDGE": "Bridge",
        "name_COMBINER": "Combiner",
        "description_ADAPTER": "Connect to devices via protocol",
        "description_BRIDGE": "Connect to remote MQTT broker"
      }
    }
  }
}
```

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Metadata completeness (skipped)
- ⏭️ Icon rendering (skipped)

---

### Subtask 3: Trigger Button Component

**File:** `src/modules/Workspace/components/controls/CreateEntityButton.tsx`

**Purpose:** Dropdown menu in CanvasToolbar to initiate wizard

**Design:**

```
┌─────────────────────────┐
│ [+] Create New...   [▼] │  ← Button
└─────────────────────────┘
         │
         ▼
┌─────────────────────────────────┐
│ Entities                         │
│  ├─ 📊 Adapter                   │
│  ├─ 🌉 Bridge                    │
│  ├─ 🔀 Combiner                  │
│  ├─ 🗺️  Asset Mapper             │
│  └─ 📁 Group                     │
│                                  │
│ Integration Points               │
│  ├─ 🏷️  Tags                     │
│  ├─ 🔍 Topic Filters             │
│  ├─ ⬆️  Data Mapping (North)     │
│  ├─ ⬇️  Data Mapping (South)     │
│  └─ 🔀 Data Combining            │
└─────────────────────────────────┘
```

**Implementation:**

- Use Chakra `Menu` / `MenuButton` / `MenuList`
- Group by category with dividers
- Icons from react-icons
- Disabled states for unavailable options
- Keyboard navigation (arrow keys, Enter)

**Integration:**

- Add to `CanvasToolbar.tsx` after search/filter controls
- Use existing spacing/styling patterns
- Responsive: collapse to icon-only on mobile

**Accessibility:**

- `aria-label="Create new entity or integration point"`
- `role="menu"` with proper menu items
- Focus management on open/close
- Announce selection to screen readers

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Menu rendering (skipped)
- ⏭️ Category grouping (skipped)
- ⏭️ Click handling (skipped)

---

### Subtask 4: Progress Bar Component

**File:** `src/modules/Workspace/components/wizard/steps/WizardProgressBar.tsx`

**Purpose:** Visual feedback for multi-step wizards

**Design:**

```
┌──────────────────────────────────────────────────────────────┐
│  Step 2 of 4: Configure adapter settings      [Cancel Wizard] │
└──────────────────────────────────────────────────────────────┘
```

**Implementation:**

- React Flow `<Panel position="bottom-center" />`
- Compact horizontal layout
- Progress indicator (steps counter or bar)
- Current step description
- Cancel button

**States:**

- Hidden when wizard not active
- Animated entry/exit
- Update on step change

**Accessibility:**

- `role="status"` for live updates
- `aria-live="polite"` for step announcements
- Clear step indicators for screen readers

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Step progression (skipped)
- ⏭️ Cancel handling (skipped)

---

### Subtask 5: Ghost Node System

**File:** `src/modules/Workspace/components/wizard/preview/GhostNode.tsx`

**Purpose:** Visual preview of entities being created

**Design Characteristics:**

- 50% opacity
- Dashed border
- Lighter background
- Non-interactive (cannot be moved/clicked)
- Animated entrance
- Clear "Preview" indicator

**Implementation:**

```typescript
interface GhostNodeProps {
  type: NodeType
  position: { x: number; y: number }
  data: Partial<NodeData>
  connections?: GhostConnection[]
}

const GhostNode: FC<GhostNodeProps> = ({ type, position, data }) => {
  return (
    <Node
      type={type}
      position={position}
      data={{
        ...data,
        isGhost: true,  // Flag for styling
      }}
      selectable={false}
      draggable={false}
      style={{
        opacity: 0.5,
        border: '2px dashed var(--chakra-colors-gray-400)',
      }}
    />
  )
}
```

**Ghost Node Management:**

- Add to React Flow nodes during wizard
- Position automatically (use layout engine)
- Show connections to existing nodes
- Remove on cancel or replace on completion

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Rendering (skipped)
- ⏭️ Positioning (skipped)
- ⏭️ Removal (skipped)

---

### Subtask 6: Configuration Panel Integration

**File:** `src/modules/Workspace/components/wizard/utils/configurationPanelRouter.ts`

**Purpose:** Route wizard configuration to appropriate forms

**Approach:**

- Reuse existing drawer components
- Adapt forms for wizard context
- Pass wizard state as props
- Handle validation and submission

**Examples:**

**Adapter:**

```typescript
// Reuse: src/modules/ProtocolAdapters/components/ProtocolAdapterForm.tsx
<AdapterConfigurationPanel
  mode="create"
  wizardContext={{
    onComplete: (data) => wizardActions.updateConfiguration(data),
    onCancel: wizardActions.cancelWizard,
  }}
/>
```

**Bridge:**

```typescript
// Reuse: src/modules/Bridges/components/BridgeForm.tsx
<BridgeConfigurationPanel
  mode="create"
  wizardContext={{
    onComplete: (data) => wizardActions.updateConfiguration(data),
    onCancel: wizardActions.cancelWizard,
  }}
/>
```

**Strategy:**

- Minimal modifications to existing forms
- Wizard context passed as optional prop
- Forms remain usable outside wizard
- Validation logic unchanged

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Panel routing (skipped)
- ⏭️ Form integration (skipped)

---

### Subtask 7: Adapter Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/entity-wizards/AdapterWizard.tsx`

**Purpose:** Complete end-to-end adapter creation flow

**Steps:**

1. **Initial Trigger** (Step 0)

   - User clicks "Create New > Adapter"
   - Wizard activates

2. **Ghost Preview** (Step 1)

   - Create ghost DEVICE node
   - Create ghost ADAPTER node
   - Create ghost connection: DEVICE → ADAPTER → EDGE
   - Position using layout engine
   - Show progress: "Step 1 of 3: Review preview"

3. **Select Adapter Type** (Step 2)

   - Open side panel with adapter type selector
   - Show available protocol types
   - Progress: "Step 2 of 3: Select protocol"
   - User selects type (e.g., OPC UA)

4. **Configure Adapter** (Step 3)

   - Load protocol-specific configuration form
   - Progress: "Step 3 of 3: Configure settings"
   - User fills form
   - Validate inputs

5. **Create** (Final Step)
   - Submit configuration to API
   - Remove ghost nodes
   - Add real nodes to canvas
   - Show success feedback
   - Close wizard

**Error Handling:**

- API errors: Show toast, keep wizard open
- Validation errors: Highlight fields
- Cancel: Confirm if data entered

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Complete flow (skipped)
- ⏭️ Ghost nodes (skipped)
- ⏭️ Configuration (skipped)
- ⏭️ API integration (skipped)

---

### Subtask 8: Bridge Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/entity-wizards/BridgeWizard.tsx`

**Purpose:** Complete bridge creation flow

**Steps:**

1. Trigger
2. Ghost preview: HOST → BRIDGE → EDGE
3. Configure bridge (single step)
4. Create

**Similar to adapter but:**

- No type selection step
- Single configuration form
- Host node creation

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Complete flow (skipped)

---

### Subtask 9: Combiner Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/entity-wizards/CombinerWizard.tsx`

**Purpose:** Create combiner with source selection

**Steps:**

1. Trigger
2. **Source Selection** (Interactive)
   - Instruction: "Click to select data sources"
   - Highlight selectable nodes (adapters/bridges)
   - Multi-select support
   - Visual feedback on selection
   - Progress: "Select at least 1 source"
3. Ghost preview: SOURCES → COMBINER → EDGE
4. Configure combiner mappings
5. Create

**New Features:**

- Interactive node selection on canvas
- Visual selection feedback
- Multi-select with shift/ctrl
- Min/max source constraints

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Source selection (skipped)
- ⏭️ Multi-select (skipped)

---

### Subtask 10: Asset Mapper Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/entity-wizards/AssetMapperWizard.tsx`

**Purpose:** Create asset mapper with Pulse Agent requirement

**Steps:**

1. Trigger
2. **Source Selection** (Interactive)
   - **Mandatory:** Pulse Agent node must be selected
   - Additional sources: adapters/bridges
   - Validation: Ensure Pulse Agent included
3. Ghost preview: SOURCES + PULSE → ASSET MAPPER → EDGE
4. Configure asset mappings
5. Create

**Special Requirements:**

- Pulse Agent mandatory
- Visual indication of required selection
- Warning if Pulse Agent not selected

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Pulse requirement (skipped)
- ⏭️ Source selection (skipped)

---

### Subtask 11: Group Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/entity-wizards/GroupWizard.tsx`

**Purpose:** Create logical grouping of nodes

**Steps:**

1. Trigger
2. **Node Selection** (Interactive)
   - Select nodes to group
   - Constraint: Nodes not already in a group
   - Visual feedback
3. Ghost preview: GROUP container around selections
4. Configure group (name, color, description)
5. Create

**New Features:**

- Group boundary visualization
- Exclusion of already-grouped nodes
- Container-style ghost preview

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Selection constraints (skipped)
- ⏭️ Group boundary (skipped)

---

### Subtask 12: TAG Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/integration-wizards/TagWizard.tsx`

**Purpose:** Add tags to device nodes

**Steps:**

1. Trigger
2. **Select Device** (Interactive)
   - Click device node on canvas
   - Highlight selectable devices
3. Configure tags (reuse DevicePropertyDrawer form)
4. Update node (add tag markers)

**Visual Feedback:**

- Tag count badge on device node
- Temporary highlight during wizard

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Device selection (skipped)
- ⏭️ Tag addition (skipped)

---

### Subtask 13: TOPIC FILTER Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/integration-wizards/TopicFilterWizard.tsx`

**Purpose:** Add topic filters to Edge Broker

**Steps:**

1. Trigger
2. **Select Edge Broker** (Interactive)
   - Auto-select if only one
3. Configure topic filters (reuse TopicFilterManager form)
4. Update node (add filter markers)

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Broker selection (skipped)

---

### Subtask 14: DATA MAPPING Wizards (North/South)

**Files:**

- `src/modules/Workspace/components/wizard/integration-wizards/DataMappingNorthWizard.tsx`
- `src/modules/Workspace/components/wizard/integration-wizards/DataMappingSouthWizard.tsx`

**Purpose:** Add data mappings to adapters

**Steps:**

1. Trigger (specify direction)
2. **Select Adapter** (Interactive)
3. Configure mappings (reuse AdapterMappingManager form)
4. Update node (add mapping markers)

**Distinction:**

- Northbound: Device → Broker
- Southbound: Broker → Device
- Visual indication of direction

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Direction handling (skipped)

---

### Subtask 15: DATA COMBINING Wizard Implementation

**File:** `src/modules/Workspace/components/wizard/integration-wizards/DataCombiningWizard.tsx`

**Purpose:** Configure data combining for combiner nodes

**Steps:**

1. Trigger
2. **Select Combiner** (Interactive)
   - Or select sources (infer combiner)
3. Configure combining logic (reuse CombinerMappingManager form)
4. Update node (add combining markers)

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Combiner selection (skipped)

---

### Subtask 16: Wizard Orchestrator Component

**File:** `src/modules/Workspace/components/wizard/WizardOrchestrator.tsx`

**Purpose:** Top-level wizard coordinator

**Responsibilities:**

- Listen to wizard state
- Render appropriate wizard component
- Manage progress bar visibility
- Handle ghost node lifecycle
- Coordinate panel opening/closing

**Implementation:**

```typescript
const WizardOrchestrator: FC = () => {
  const { isActive, entityType, currentStep } = useWizardState()

  if (!isActive) return null

  return (
    <>
      <WizardProgressBar />
      {renderWizardComponent(entityType)}
      {renderGhostNodes()}
    </>
  )
}
```

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Wizard routing (skipped)

---

### Subtask 17: Interactive Selection System

**File:** `src/modules/Workspace/components/wizard/utils/selectionManager.ts`

**Purpose:** Handle interactive node selection during wizard

**Features:**

- Highlight selectable nodes
- Track selections
- Enforce constraints (min/max, type filters)
- Visual feedback (border, overlay)
- Keyboard support (tab, space, enter)

**Implementation:**

```typescript
interface SelectionConstraints {
  minNodes?: number
  maxNodes?: number
  nodeTypes?: NodeType[]
  excludeGrouped?: boolean
  requiredNodes?: string[]
}

export const useWizardSelection = (constraints: SelectionConstraints) => {
  const [selectedNodes, setSelectedNodes] = useState<string[]>([])
  const canProceed = validateSelection(selectedNodes, constraints)

  return {
    selectedNodes,
    canProceed,
    selectNode,
    deselectNode,
    clearSelection,
  }
}
```

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Selection tracking (skipped)
- ⏭️ Constraints validation (skipped)

---

### Subtask 18: Error Handling & Validation

**Files:**

- `src/modules/Workspace/components/wizard/utils/wizardValidation.ts`
- `src/modules/Workspace/components/wizard/components/WizardErrorBoundary.tsx`

**Purpose:** Robust error handling for wizard flows

**Coverage:**

- Form validation errors
- API errors
- Network failures
- Invalid selections
- State inconsistencies

**User Experience:**

- Toast notifications for errors
- Inline validation messages
- Error recovery options
- "Save draft" functionality
- Confirmation on cancel if data entered

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Error scenarios (skipped)

---

### Subtask 19: Keyboard Shortcuts & Accessibility

**File:** `src/modules/Workspace/components/wizard/hooks/useWizardKeyboard.ts`

**Purpose:** Full keyboard support for wizard

**Shortcuts:**

- `Ctrl+N` / `Cmd+N`: Open create menu
- `Esc`: Cancel wizard / close menu
- `Enter`: Confirm selection / proceed
- `Tab`: Navigate form fields
- `Arrow keys`: Navigate menu items
- `Space`: Select node in selection mode

**Accessibility Checklist:**

- [ ] All interactive elements keyboard accessible
- [ ] Focus management (trap focus in panels)
- [ ] ARIA labels on all controls
- [ ] Screen reader announcements for state changes
- [ ] Skip links for long wizards
- [ ] High contrast mode support
- [ ] Reduced motion support

**Tests:**

- ✅ Accessibility test (unskipped)
- ⏭️ Keyboard navigation (skipped)

---

### Subtask 20: Documentation & Examples

**Files:**

- `src/modules/Workspace/components/wizard/README.md`
- `.tasks/38111-workspace-operation-wizard/ARCHITECTURE.md`
- `.tasks/38111-workspace-operation-wizard/USER_GUIDE.md`

**Purpose:** Comprehensive documentation for developers and users

**Developer Documentation:**

- Architecture overview
- Adding new wizard types
- Customizing wizard steps
- Testing guidelines
- Troubleshooting

**User Documentation:**

- How to use the wizard
- Screenshots of each step
- Common workflows
- Keyboard shortcuts
- FAQ

---

## Testing Strategy

### Pragmatic Approach

Per your directive, we'll follow this testing pattern for all components:

**Every Component Must Have Tests, But:**

- ✅ **Accessibility test:** ALWAYS UNSKIPPED (mandatory, must pass)
- ⏭️ **All other tests:** SKIP during initial development

**Example Test File Structure:**

```typescript
describe('AdapterWizard', () => {
  // ✅ This test MUST pass and remain unskipped
  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<AdapterWizard />)
    cy.checkAccessibility()
  })

  // ⏭️ All other tests skipped for rapid development
  it.skip('should create ghost nodes on start', () => {
    // Test implementation...
  })

  it.skip('should show progress bar', () => {
    // Test implementation...
  })

  it.skip('should handle adapter type selection', () => {
    // Test implementation...
  })

  it.skip('should submit configuration', () => {
    // Test implementation...
  })

  it.skip('should handle cancellation', () => {
    // Test implementation...
  })
})
```

**Benefits:**

- ✅ Maintain test coverage structure
- ✅ Ensure accessibility from day one
- ✅ Rapid development without test maintenance burden
- ✅ Tests are documented and ready to unskip later
- ✅ CI/CD passes (skipped tests don't fail)

**When to Unskip:**

- During bug fixes related to that functionality
- When feature is stable and needs full coverage
- Before major releases
- When refactoring that area

---

## i18n Strategy

Following the **I18N_GUIDELINES.md** strictly:

### Rule 1: ALWAYS Use Plain String Keys

❌ **NEVER:**

```typescript
t(`workspace.wizard.${entityType}.name`) // NO TEMPLATE LITERALS!
```

✅ **ALWAYS:**

```typescript
t('workspace.wizard.entityType.name', { context: entityType })
```

### Translation Structure

**en.json:**

```json
{
  "workspace": {
    "wizard": {
      "trigger": {
        "buttonLabel": "Create New",
        "ariaLabel": "Create new entity or integration point",
        "menuTitle": "What would you like to create?"
      },
      "category": {
        "entities": "Entities",
        "integrationPoints": "Integration Points"
      },
      "entityType": {
        "name_ADAPTER": "Adapter",
        "name_BRIDGE": "Bridge",
        "name_COMBINER": "Combiner",
        "name_ASSET_MAPPER": "Asset Mapper",
        "name_GROUP": "Group",
        "name_TAG": "Tags",
        "name_TOPIC_FILTER": "Topic Filters",
        "name_DATA_MAPPING_NORTH": "Data Mapping (Northbound)",
        "name_DATA_MAPPING_SOUTH": "Data Mapping (Southbound)",
        "name_DATA_COMBINING": "Data Combining",

        "description_ADAPTER": "Connect to devices using specific protocols",
        "description_BRIDGE": "Connect to another MQTT broker",
        "description_COMBINER": "Merge data from multiple sources",
        "description_ASSET_MAPPER": "Map data to HiveMQ Pulse assets",
        "description_GROUP": "Group nodes logically",
        "description_TAG": "Add tags to a device",
        "description_TOPIC_FILTER": "Configure topic filters",
        "description_DATA_MAPPING_NORTH": "Map device data to MQTT topics",
        "description_DATA_MAPPING_SOUTH": "Map MQTT topics to device commands",
        "description_DATA_COMBINING": "Configure data combining logic"
      },
      "progress": {
        "stepCounter": "Step {{current}} of {{total}}",
        "cancel": "Cancel Wizard",
        "ariaLabel": "Wizard progress",

        "step_ADAPTER_0": "Review adapter preview",
        "step_ADAPTER_1": "Select protocol type",
        "step_ADAPTER_2": "Configure adapter settings",

        "step_BRIDGE_0": "Review bridge preview",
        "step_BRIDGE_1": "Configure bridge settings",

        "step_COMBINER_0": "Select data sources",
        "step_COMBINER_1": "Review combiner preview",
        "step_COMBINER_2": "Configure combining logic"
      },
      "selection": {
        "instruction": "Click to select {{nodeType}}",
        "instructionMulti": "Select {{min}} to {{max}} nodes",
        "selected": "{{count}} selected",
        "required": "{{nodeType}} is required",
        "cannotSelect": "This node cannot be selected"
      },
      "ghost": {
        "label": "Preview",
        "ariaLabel": "Preview of {{entityType}} being created"
      },
      "errors": {
        "apiError": "Failed to create {{entityType}}",
        "validationError": "Please fix the validation errors",
        "selectionRequired": "Please select at least {{count}} node",
        "pulseAgentRequired": "Pulse Agent node must be selected"
      },
      "confirmation": {
        "cancelTitle": "Cancel Wizard?",
        "cancelMessage": "You have unsaved changes. Are you sure you want to cancel?",
        "cancelConfirm": "Yes, Cancel",
        "cancelAbort": "Continue Editing"
      }
    }
  }
}
```

### Context Usage Pattern

**In Components:**

```typescript
const EntityTypeCard: FC<{ type: EntityType }> = ({ type }) => {
  const { t } = useTranslation()

  return (
    <Card>
      <Heading>
        {t('workspace.wizard.entityType.name', { context: type })}
      </Heading>
      <Text>
        {t('workspace.wizard.entityType.description', { context: type })}
      </Text>
    </Card>
  )
}
```

**Step Descriptions:**

```typescript
const getCurrentStepDescription = (entityType: EntityType, step: number) => {
  const stepKey = `${entityType}_${step}`
  return t('workspace.wizard.progress.step', { context: stepKey })
}
```

---

## Reporting Strategy

Following **REPORTING_STRATEGY.md**:

### Permanent Documentation (.tasks/ - IN GIT)

**Files to Create:**

- ✅ `TASK_BRIEF.md` (already created)
- ⏹️ `TASK_PLAN.md` (this document)
- 🔄 `TASK_SUMMARY.md` (updated after each subtask)
- 🔄 `CONVERSATION_SUBTASK_N.md` (detailed subtask discussions)
- 📝 `ARCHITECTURE.md` (technical decisions)
- 📝 `USER_GUIDE.md` (end-user documentation)

### Session Logs (.tasks-log/ - LOCAL ONLY)

**Naming Convention:**

```
38111_00_SESSION_INDEX.md
38111_01_Wizard_Foundation.md
38111_02_Adapter_Wizard_Complete.md
38111_03_Ghost_Node_System.md
38111_04_Selection_Manager.md
...
```

**When to Create Session Logs:**

- After completing each subtask
- When encountering and solving issues
- When making architectural decisions
- After user testing sessions

**Session Log Template:**

```markdown
# Session Log: [Descriptive Title]

**Task:** 38111-workspace-operation-wizard
**Date:** YYYY-MM-DD
**Subtasks:** [1, 2, 3]

## Summary

Brief description of work done

## Changes Made

- File changes
- New components
- Updated tests

## Issues Encountered

- Problem description
- Solution applied

## Test Results

- Commands run
- Pass/fail status

## Next Steps

- What remains
- Dependencies
```

---

## Component Architecture

### File Structure

```
src/modules/Workspace/components/wizard/
├── WizardOrchestrator.tsx              # Main coordinator
├── entity-wizards/
│   ├── AdapterWizard.tsx               # Subtask 7
│   ├── BridgeWizard.tsx                # Subtask 8
│   ├── CombinerWizard.tsx              # Subtask 9
│   ├── AssetMapperWizard.tsx           # Subtask 10
│   └── GroupWizard.tsx                 # Subtask 11
├── integration-wizards/
│   ├── TagWizard.tsx                   # Subtask 12
│   ├── TopicFilterWizard.tsx           # Subtask 13
│   ├── DataMappingNorthWizard.tsx      # Subtask 14
│   ├── DataMappingSouthWizard.tsx      # Subtask 14
│   └── DataCombiningWizard.tsx         # Subtask 15
├── steps/
│   ├── WizardProgressBar.tsx           # Subtask 4
│   ├── SelectionStep.tsx               # Node selection UI
│   └── ConfigurationStep.tsx           # Wraps config forms
├── preview/
│   ├── GhostNode.tsx                   # Subtask 5
│   └── GhostEdge.tsx                   # Ghost connections
├── hooks/
│   ├── useWizardState.ts               # Subtask 1
│   ├── useWizardSelection.ts           # Subtask 17
│   └── useWizardKeyboard.ts            # Subtask 19
└── utils/
    ├── wizardMetadata.ts               # Subtask 2
    ├── configurationPanelRouter.ts     # Subtask 6
    ├── selectionManager.ts             # Subtask 17
    └── wizardValidation.ts             # Subtask 18
```

### Integration Points

**CanvasToolbar:**

```typescript
// src/modules/Workspace/components/controls/CanvasToolbar.tsx
import CreateEntityButton from '../wizard/CreateEntityButton'

const CanvasToolbar: FC = () => {
  return (
    <Panel position="top-left">
      {/* Existing controls */}
      <SearchEntities />
      <DrawerFilterToolbox />
      <Divider />

      {/* NEW: Wizard trigger */}
      <CreateEntityButton />

      {/* Existing layout controls */}
      <LayoutSelector />
    </Panel>
  )
}
```

**ReactFlowWrapper:**

```typescript
// src/modules/Workspace/components/ReactFlowWrapper.tsx
import WizardOrchestrator from './wizard/WizardOrchestrator'

const ReactFlowWrapper: FC = () => {
  return (
    <ReactFlow>
      {/* Existing content */}
      <CanvasControls />
      <Background />
      <MiniMap />

      {/* NEW: Wizard system */}
      <WizardOrchestrator />
    </ReactFlow>
  )
}
```

---

## Design Decisions

### State Management: Zustand vs Context

**Decision: Use Zustand**

**Rationale:**

- Already used in workspace (`useWorkspaceStore`)
- Better performance for frequent updates
- Easy to access outside React components
- No prop drilling
- DevTools support

**Implementation:**

```typescript
// src/modules/Workspace/hooks/useWizardStore.ts
import { create } from 'zustand'
import { devtools } from 'zustand/middleware'

interface WizardStore extends WizardState {
  actions: WizardActions
}

export const useWizardStore = create<WizardStore>()(
  devtools(
    (set, get) => ({
      // Initial state
      isActive: false,
      entityType: null,
      currentStep: 0,
      // ...

      // Actions
      actions: {
        startWizard: (type) => set({ isActive: true, entityType: type }),
        cancelWizard: () => set({ isActive: false /* reset */ }),
        // ...
      },
    }),
    { name: 'WizardStore' }
  )
)
```

---

### Ghost Node Positioning

**Decision: Integrate with Layout Engine**

**Rationale:**

- Reuse existing layout algorithms
- Consistent with manual node placement
- Automatic collision avoidance
- Respects user's layout preferences

**Implementation:**

```typescript
import { useLayoutEngine } from '@/modules/Workspace/hooks/useLayoutEngine'

const createGhostNodes = (entityType: EntityType) => {
  const { calculateNodePosition } = useLayoutEngine()

  const ghostNodes = generateGhostNodesForType(entityType)
  const positionedGhosts = ghostNodes.map((node) => ({
    ...node,
    position: calculateNodePosition(node, existingNodes),
  }))

  return positionedGhosts
}
```

---

### Configuration Form Reusability

**Decision: Minimal Adaptation, Maximum Reuse**

**Approach:**

- Add optional `wizardContext` prop to existing forms
- Forms detect wizard mode and adapt behavior
- Validation logic unchanged
- Submission handling routes to wizard or direct API

**Example:**

```typescript
interface WizardContext {
  onComplete: (data: ConfigData) => void
  onCancel: () => void
  ghostNodeId?: string
}

interface FormProps {
  mode: 'create' | 'edit'
  initialData?: ConfigData
  wizardContext?: WizardContext // Optional
}

const AdapterForm: FC<FormProps> = ({ mode, initialData, wizardContext }) => {
  const handleSubmit = (data: ConfigData) => {
    if (wizardContext) {
      // Wizard mode: pass data to wizard
      wizardContext.onComplete(data)
    } else {
      // Normal mode: submit directly
      submitAdapterMutation(data)
    }
  }

  // Rest of form logic...
}
```

**Benefits:**

- No code duplication
- Single source of truth for validation
- Forms remain independently usable
- Easy to test both modes

---

## UX Design Principles

### Progressive Disclosure

**Principle:** Show only what's needed at each step

**Implementation:**

- Start with simple entity type selection
- Reveal complexity gradually
- Hide advanced options by default
- Provide "Learn more" links

**Example:**

```
Step 1: What would you like to create?
  → [Adapter]

Step 2: Which protocol?
  → [OPC UA] [MQTT] [Modbus] [...]

Step 3: Basic Settings
  → Name: ___
  → Host: ___
  → [Advanced Settings ▼]  ← Collapsed by default

Step 4: Review & Create
  → Summary of configuration
  → [Create Adapter]
```

---

### Visual Feedback

**Principle:** Always show what's happening

**Techniques:**

1. **Ghost Nodes:** Show result before committing
2. **Progress Bar:** Show current step and total
3. **Selection Highlights:** Show what's selectable
4. **Animations:** Smooth transitions between steps
5. **Loading States:** Indicate processing
6. **Success/Error Feedback:** Clear outcome indication

---

### Error Recovery

**Principle:** Errors should be fixable without starting over

**Strategies:**

1. **Inline Validation:** Catch errors early
2. **Clear Error Messages:** Explain what's wrong and how to fix
3. **Non-blocking Errors:** Allow fixing without cancelling wizard
4. **Draft Saving:** Preserve data on cancel (optional)
5. **Back Button:** Allow returning to previous steps

---

## Accessibility Commitments

### WCAG 2.1 Level AA Compliance

**Keyboard Navigation:**

- ✅ All controls reachable via Tab
- ✅ Modal/panel focus trapping
- ✅ Escape to cancel/close
- ✅ Enter to confirm
- ✅ Arrow keys for menu navigation

**Screen Readers:**

- ✅ All interactive elements labeled
- ✅ State changes announced
- ✅ Progress updates communicated
- ✅ Error messages read aloud
- ✅ Instructions provided

**Visual Design:**

- ✅ 4.5:1 contrast ratio minimum
- ✅ Focus indicators visible
- ✅ No color-only information
- ✅ Sufficient text size
- ✅ Icons paired with text labels

**Motion:**

- ✅ Respect `prefers-reduced-motion`
- ✅ No auto-playing animations
- ✅ Skippable animations

---

## Performance Considerations

### Ghost Node Rendering

**Concern:** Adding multiple ghost nodes could impact performance

**Solutions:**

- Limit max ghost nodes to 5 per wizard
- Use React.memo for ghost node components
- Debounce position calculations
- Remove ghost nodes immediately on cancel

**Monitoring:**

- Track render times
- Monitor React DevTools profiler
- User feedback on responsiveness

---

### Wizard State Size

**Concern:** Large configuration data in state

**Solutions:**

- Store minimal state (IDs, not full objects)
- Lazy load configuration forms
- Clear state immediately after wizard completion
- Use refs for form data instead of state

---

## Migration Strategy

### Existing Create Flows

**Current State:**

- Adapters created via `/protocol-adapters/new`
- Bridges created via `/bridges/new`
- Combiners only via workspace (drag-drop)
- Asset mappers only via workspace

**Phase 1:** Wizard introduction

- Keep existing flows intact
- Add wizard as alternative
- User testing to gather feedback

**Phase 2:** Gradual adoption

- Promote wizard in UI
- Add "Try the new wizard" hints
- Collect usage metrics

**Phase 3:** Consolidation

- Make wizard default
- Deprecate old flows (optional)
- Remove duplicate code paths

**No Breaking Changes:**

- Old routes remain functional
- Forms reused, not replaced
- API calls unchanged

---

## Success Metrics

### Quantitative

1. **Completion Rate:** % of started wizards that complete
2. **Time to Create:** Average time from trigger to entity creation
3. **Error Rate:** % of wizard sessions with errors
4. **Abandonment Points:** Where users cancel most often
5. **Accessibility Compliance:** 100% of tests passing

### Qualitative

1. **User Feedback:** Post-wizard surveys
2. **Usability Testing:** Observed user sessions
3. **Developer Feedback:** Ease of adding new wizard types
4. **Support Tickets:** Reduction in creation-related issues

---

## Risk Assessment

### High Risk

**1. Complexity Creep**

- **Risk:** Wizard becomes too complex
- **Mitigation:** Strict step limits, progressive disclosure, user testing

**2. Form Integration Issues**

- **Risk:** Existing forms don't adapt well to wizard context
- **Mitigation:** Minimal modifications, thorough testing, fallback to direct forms

**3. Ghost Node Confusion**

- **Risk:** Users confused by preview vs real nodes
- **Mitigation:** Clear visual distinction, labels, tooltips

### Medium Risk

**4. Performance Impact**

- **Risk:** Ghost nodes slow down canvas
- **Mitigation:** Limit ghost nodes, optimize rendering, profiling

**5. Accessibility Gaps**

- **Risk:** Wizard not fully accessible
- **Mitigation:** Accessibility-first design, mandatory tests, expert review

**6. i18n Complexity**

- **Risk:** Too many translation keys, maintenance burden
- **Mitigation:** Use context feature, consistent naming, documentation

### Low Risk

**7. Browser Compatibility**

- **Risk:** Wizard doesn't work in all browsers
- **Mitigation:** React Flow is well-supported, test in target browsers

**8. State Management Bugs**

- **Risk:** Wizard state inconsistencies
- **Mitigation:** Zustand devtools, thorough testing, state validation

---

## Timeline Estimate

### Phase 1: Foundation (Subtasks 1-7)

**Duration:** 2-3 weeks

- Week 1: Subtasks 1-4 (Foundation)
- Week 2: Subtasks 5-6 (Ghost system, integration)
- Week 3: Subtask 7 (Adapter wizard complete)

### Phase 2: Entities (Subtasks 8-11)

**Duration:** 1.5-2 weeks

- Week 4: Subtasks 8-9 (Bridge, Combiner)
- Week 5: Subtasks 10-11 (Asset Mapper, Group)

### Phase 3: Integration Points (Subtasks 12-15)

**Duration:** 1.5-2 weeks

- Week 6: Subtasks 12-13 (Tags, Topic Filters)
- Week 7: Subtasks 14-15 (Data Mappings, Combining)

### Phase 4: Polish (Subtasks 16-20)

**Duration:** 1-1.5 weeks

- Week 8: Subtasks 16-19 (Orchestrator, Selection, Errors, Keyboard)
- Week 9: Subtask 20 (Documentation)

**Total Estimate:** 6-9 weeks

**Variables:**

- Complexity of form adaptations
- Number of edge cases discovered
- User feedback iterations
- Accessibility audit results

---

## Next Steps

### Immediate Actions

1. **Review and approve this plan**
2. **Create TASK_SUMMARY.md**
3. **Start Subtask 1: Wizard State Management**
4. **Set up session logging structure**

### Before Development

- [ ] Review all referenced guidelines
- [ ] Set up i18n translation structure
- [ ] Create test file templates
- [ ] Prepare Zustand store skeleton
- [ ] Document current form structures

### During Development

- [ ] Update TASK_SUMMARY.md after each subtask
- [ ] Create session logs for major work
- [ ] Run accessibility tests before marking complete
- [ ] Keep CONVERSATION_SUBTASK_N.md files updated
- [ ] Screenshot wizard in action for documentation

---

## Questions for Stakeholders

1. **Priority:** Is the suggested order (Adapter → other entities → integration points) acceptable?

2. **Scope:** Should Phase 1 be fully complete and tested before moving to Phase 2?

3. **Old Flows:** Do we want to eventually deprecate old creation routes, or keep both?

4. **Shortcuts:** Are there any additional keyboard shortcuts desired?

5. **Analytics:** Do we want to track wizard usage for product analytics?

6. **Customization:** Should admin users be able to customize wizard steps/options?

---

## Appendix A: Glossary

- **Entity:** Top-level nodes (Adapter, Bridge, Combiner, Asset Mapper, Group)
- **Integration Point:** Configuration on entities (Tags, Topic Filters, Data Mappings)
- **Ghost Node:** Temporary preview node shown during wizard
- **Wizard Step:** Single stage in multi-step creation process
- **Selection Mode:** Interactive state where users click to select nodes
- **Configuration Panel:** Side drawer containing entity configuration form
- **Progress Bar:** Visual indicator of wizard progress
- **Trigger:** Button that initiates wizard

---

## Appendix B: Visual Mockups

### Wizard Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                   WORKSPACE CANVAS                           │
│                                                              │
│  ┌──────────────────────────────────────────────────┐      │
│  │  Search  Filter  [+] Create New ▼  Layout ⚙️      │      │
│  └──────────────────────────────────────────────────┘      │
│                           │                                  │
│                           ▼                                  │
│  ┌────────────────────────────────────────────┐            │
│  │  What would you like to create?            │            │
│  ├────────────────────────────────────────────┤            │
│  │  Entities                                  │            │
│  │    📊 Adapter                              │            │
│  │    🌉 Bridge                               │            │
│  │    🔀 Combiner                             │            │
│  │  ────────────────────────────────────────  │            │
│  │  Integration Points                        │            │
│  │    🏷️  Tags                                │            │
│  │    🔍 Topic Filters                        │            │
│  └────────────────────────────────────────────┘            │
│                                                              │
│  [After selection...]                                       │
│                                                              │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Step 2 of 3: Select protocol      [Cancel Wizard]   │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│        ┌─ ─ ─ ─ ─ ┐       ┌─ ─ ─ ─ ─ ┐                    │
│        │  DEVICE  │   →   │ ADAPTER  │   →   [EDGE]       │
│        └─ ─ ─ ─ ─ ┘       └─ ─ ─ ─ ─ ┘                    │
│        (Ghost nodes - dashed outline)                       │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                                      │
                                      ├───────────────────┐
                                      │ Configuration     │
                                      │ Panel (Drawer)    │
                                      │                   │
                                      │ Select Protocol:  │
                                      │ ○ OPC UA          │
                                      │ ○ MQTT            │
                                      │ ○ Modbus          │
                                      │                   │
                                      │ [Back]  [Next]    │
                                      └───────────────────┘
```

---

## Appendix C: Component Props Reference

### CreateEntityButton

```typescript
interface CreateEntityButtonProps {
  disabled?: boolean
  onSelectType: (type: EntityType | IntegrationPointType) => void
}
```

### WizardProgressBar

```typescript
interface WizardProgressBarProps {
  currentStep: number
  totalSteps: number
  stepDescription: string
  onCancel: () => void
}
```

### GhostNode

```typescript
interface GhostNodeProps {
  id: string
  type: NodeType
  position: { x: number; y: number }
  data: Partial<NodeData>
  connections?: Array<{ target: string; type: 'source' | 'target' }>
}
```

### WizardOrchestrator

```typescript
interface WizardOrchestratorProps {
  // No props - reads from store
}
```

---

## Document Revision History

| Date       | Version | Author | Changes               |
| ---------- | ------- | ------ | --------------------- |
| 2025-11-10 | 1.0     | AI     | Initial plan creation |

---

**END OF TASK PLAN**
