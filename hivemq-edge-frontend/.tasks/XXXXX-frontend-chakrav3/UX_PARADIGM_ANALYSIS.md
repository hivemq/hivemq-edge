# UX Paradigm Migration Analysis: Visual Flow → Resource Listings

**Document Purpose:** Analysis of migrating from React Flow-based visual UX to traditional resource listing \+ editing paradigm for Workspace and DataHub features.

**Date:** December 12, 2025

---

## Executive Summary

This report analyzes the engineering, design, and product implications of introducing (or switching to) a "resource listing \+ RJSF-based editing" UX paradigm for the visually intensive Workspace and DataHub features. Both currently rely heavily on React Flow for their core interactive experiences.

---

## 1\. Current Architecture Analysis

### 1.1 Codebase Metrics

| Feature       | TSX Components  | TSX Test Files   | Percentage of App  |
| :------------ | :-------------- | :--------------- | :----------------- |
| **DataHub**   | 172 files       | \~86 spec files  | \~34% of total     |
| **Workspace** | 126 files       | \~63 spec files  | \~25% of total     |
| **Combined**  | 298 files       | \~149 spec files | **\~59% of total** |
| **Total App** | \~500 TSX files | \-               | 100%               |

### 1.2 React Flow Integration Depth

#### Workspace Feature

- **Core Components:** `ReactFlowWrapper.tsx` (main canvas), `EdgeFlowProvider.tsx` (state management)
- **Custom Node Types:** 10 (Adapter, Bridge, Edge, Group, Listener, Host, Device, Combiner, Pulse, Cluster)
- **Custom Edge Types:** 2 (MonitoringEdge, DynamicEdge)
- **Related UI Components:**
  - Canvas controls (zoom, pan, layout)
  - Filtering/search system
  - Property drawers (triggered by node selection)
  - Wizard system (multi-step entity creation with ghost nodes)
  - Layout engine (auto-layout with Elkjs)

#### DataHub Feature

- **Core Components:** `PolicyEditor.tsx` (main canvas), `useDataHubDraftStore.ts` (state management)
- **Custom Node Types:** 11 (DataPolicy, BehaviorPolicy, Validator, Schema, Function, Operation, TopicFilter, ClientFilter, Transition, StateNode, TransitionNode)
- **Custom Edge Types:** 1 (DataHubPolicyEdge)
- **Related UI Components:**
  - Designer toolbox (drag-drop node creation)
  - Property panels (node configuration)
  - Copy/paste/delete listeners
  - Dry-run validation system
  - FSM visualization (behavior policies)

### 1.3 Existing Resource Listing Patterns

The application **already implements resource listing patterns** for several features:

| Feature               | Pattern         | Components                                                                     |
| :-------------------- | :-------------- | :----------------------------------------------------------------------------- |
| **DataHub Listings**  | Tabbed tables   | `DataHubListings.tsx`, `PolicyTable.tsx`, `SchemaTable.tsx`, `ScriptTable.tsx` |
| **Bridges**           | Table \+ Drawer | `BridgeTable.tsx`, `BridgeEditorDrawer.tsx`                                    |
| **Protocol Adapters** | Table \+ Drawer | `ProtocolAdapters.tsx`, `AdapterInstanceDrawer.tsx`                            |
| **Event Log**         | Table \+ Drawer | `EventLogTable.tsx`, `EventDrawer.tsx`                                         |
| **Assets (Pulse)**    | Table \+ Drawer | `AssetsTable.tsx`, `ManagedAssetDrawer.tsx`                                    |

---

## 2\. Engineering Impact Analysis

### 2.1 EXTENDING with Resource Listings (Dual UX)

#### New Components Required

**For Workspace:**

```
├── WorkspaceListing.tsx           # Main page with tabs
├── tables/
│   ├── AdaptersTable.tsx          # Protocol adapter listing
│   ├── BridgesTable.tsx           # Bridge listing (exists)
│   ├── CombinersTable.tsx         # Combiner listing
│   ├── GroupsTable.tsx            # Group listing
│   └── DevicesTable.tsx           # Device listing
├── editors/
│   ├── AdapterEditorDrawer.tsx    # (exists as AdapterInstanceDrawer)
│   ├── CombinerEditorDrawer.tsx   # New
│   ├── GroupEditorDrawer.tsx      # New
│   └── DeviceEditorDrawer.tsx     # New
```

**For DataHub:**

```
├── DataHubResourceManager.tsx     # Main resource-centric page
├── tables/
│   ├── PolicyTable.tsx            # (exists)
│   ├── SchemaTable.tsx            # (exists)
│   ├── ScriptTable.tsx            # (exists)
│   ├── ValidatorTable.tsx         # New
│   └── OperationTable.tsx         # New
├── editors/
│   ├── PolicyEditorDrawer.tsx     # RJSF-based policy editor
│   ├── SchemaEditorDrawer.tsx     # New
│   ├── ScriptEditorDrawer.tsx     # New
│   ├── ValidatorEditorDrawer.tsx  # New
│   └── OperationEditorDrawer.tsx  # New
```

#### Estimated Engineering Effort (Extension)

| Category                     | Workspace | DataHub | Total          |
| :--------------------------- | :-------- | :------ | :------------- |
| New Table Components         | 4-5       | 2-3     | 6-8            |
| New Drawer/Editor Components | 3-4       | 4-5     | 7-9            |
| RJSF Schema Definitions      | 4-5       | 5-6     | 9-11           |
| Routing/Navigation Updates   | 1-2       | 1-2     | 2-4            |
| Tests (per component)        | \~15-20   | \~15-20 | 30-40          |
| **Total Estimated Days**     | 20-30     | 25-35   | **45-65 days** |

### 2.2 SWITCHING to Resource Listings (Replace Flow UX)

#### Components to Deprecate

**Workspace (126 files):**

- ReactFlowWrapper.tsx and all node/edge components (\~35 files)
- Layout engine and related utilities (\~10 files)
- Wizard system components (\~15 files)
- Canvas controls and filters (\~20 files)

**DataHub (172 files):**

- PolicyEditor.tsx and all designer components (\~40 files)
- All node panels and editors (\~30 files)
- Toolbox, canvas controls, and utilities (\~20 files)
- Copy/paste/delete listeners (\~5 files)

#### Estimated Engineering Effort (Full Switch)

| Category                  | Workspace | DataHub | Total            |
| :------------------------ | :-------- | :------ | :--------------- |
| Deprecation/Removal       | 10-15     | 15-20   | 25-35            |
| New CRUD Components       | 15-20     | 20-25   | 35-45            |
| RJSF Schema Definitions   | 8-10      | 10-15   | 18-25            |
| State Management Refactor | 5-8       | 8-12    | 13-20            |
| E2E Test Migration        | 15-25     | 20-30   | 35-55            |
| **Total Estimated Days**  | 55-80     | 75-100  | **130-180 days** |

### 2.3 Technical Debt Implications

#### React Flow Dependencies (Extension approach)

- Maintain two UX paradigms \= doubled maintenance cost
- React Flow updates affect only flow-based views
- Potential state synchronization issues between views

#### React Flow Dependencies (Switch approach)

- Complete removal of @xyflow/react dependency
- Remove related packages: elkjs (layout), dagre (layout)
- Simplifies bundle size (\~150-200KB reduction estimated)

---

## 3\. Design Implications

### 3.1 UX Paradigm Comparison

| Aspect                      | Visual Flow (Current)            | Resource Listings (Proposed)  |
| :-------------------------- | :------------------------------- | :---------------------------- |
| **Mental Model**            | Spatial/graph-based              | Hierarchical/list-based       |
| **Discovery**               | Exploratory, organic             | Structured, predictable       |
| **Learning Curve**          | Steeper for complex scenarios    | Lower for CRUD operations     |
| **Relationship Visibility** | Immediate, visual                | Requires navigation/inference |
| **Bulk Operations**         | Difficult (multi-select limited) | Native (table selection)      |
| **Mobile/Responsive**       | Poor                             | Good                          |
| **Accessibility**           | Challenging                      | Better support                |

### 3.2 Feature-Specific Design Considerations

#### Workspace

The Workspace visualizes a **topology** of interconnected entities (Edge, Adapters, Bridges, Devices, etc.). Key design challenges for resource listing:

1. **Relationship Context Loss:**

- Current: User sees adapter connected to bridge in context
- Proposed: User navigates between separate tables

2. **Spatial Awareness:**

- Current: Group memberships visible via containment
- Proposed: Requires nested/hierarchical table or explicit linking

3. **Monitoring Context:**

- Current: Status visible on nodes in real-time
- Proposed: Status column in table (loses spatial correlation)

**Mitigation Strategies:**

- Implement "relationship preview" cards in drawers
- Add navigation shortcuts between related entities
- Consider hybrid: list view with optional "mini-map" preview

#### DataHub

DataHub models **policies as directed graphs** (data flows through validators, operations, functions). Key design challenges:

1. **Pipeline Visualization:**

- Current: Visual representation of data transformation flow
- Proposed: Serialized step-by-step configuration

2. **Connection Logic:**

- Current: Drag handles to connect nodes
- Proposed: Dropdown/select for linking resources

3. **Validation Context:**

- Current: Errors shown on specific nodes
- Proposed: Error lists with links to problematic resources

**Mitigation Strategies:**

- Implement "policy preview" as read-only flow visualization
- Use numbered steps or breadcrumbs for pipeline awareness
- Consider wizard-style policy builder for guided creation

### 3.3 Accessibility Analysis (Revised)

#### React Flow v12 Accessibility Features

React Flow v12 (`@xyflow/react: 12.8.4`) provides built-in accessibility support documented at [reactflow.dev/learn/advanced-use/accessibility](https://reactflow.dev/learn/advanced-use/accessibility):

**Built-in Features:**

- **ARIA roles:** `role="application"` on wrapper with proper labeling
- **Keyboard Navigation:** Arrow keys for node traversal, Enter/Space for selection
- **Focus Management:** Nodes and edges can be focusable (`nodesFocusable`, `edgesFocusable` props)
- **Screen Reader:** Live announcements for state changes

#### Current Implementation in HiveMQ Edge

**Positive Observations:**

| Feature                  | Implementation                                                                  | Files                                               |
| :----------------------- | :------------------------------------------------------------------------------ | :-------------------------------------------------- |
| **ARIA Labeling**        | Canvas has `role="region"` and `aria-label`                                     | `ReactFlowWrapper.tsx`, `PolicyEditor.tsx`          |
| **Toolbar ARIA**         | Toolbar containers have `role="toolbar"` or `role="group"` with `aria-controls` | Both Workspace and DataHub                          |
| **Node ARIA Labels**     | Custom nodes have `aria-label` for status/type                                  | `NodePulse.tsx`, `ToolItem.tsx`, `NodeIcon.tsx`     |
| **Keyboard Shortcuts**   | Comprehensive shortcuts via `react-hotkeys-hook`                                | Copy/Paste/Delete listeners                         |
| **Cheat Sheet**          | Discoverable keyboard shortcuts modal                                           | `DesignerCheatSheet.tsx`                            |
| **Accessible Draggable** | Custom keyboard-alternative for drag-drop                                       | `useAccessibleDraggable` hook                       |
| **A11y Testing**         | Deque axe-core testing on nodes, panels, toolbars                               | \~30+ checkAccessibility calls in Workspace/DataHub |

**Current Canvas ARIA Configuration:**

```
// Workspace: ReactFlowWrapper.tsx
<ReactFlow
  id="edge-workspace-canvas"
  role="region"
  aria-label={t('workspace.canvas.aria-label')}
  // ...
>
  <Box role="group" aria-label={t('workspace.canvas.toolbar.container')} aria-controls="edge-workspace-canvas">

// DataHub: PolicyEditor.tsx
<ReactFlow
  id="edge-datahub-canvas"
  role="region"
  aria-label={t('workspace.canvas.aria-label')}
  // ...
>
  <Box role="toolbar" aria-label={t('workspace.toolbars.aria-label')} aria-controls="edge-datahub-canvas">
```

#### Accessibility Testing Coverage

The codebase has extensive a11y testing using `cypress-axe` (Deque):

| Area                   | Test Files with `checkAccessibility` | Coverage              |
| :--------------------- | :----------------------------------- | :-------------------- |
| **DataHub Nodes**      | 12 node/panel spec files             | All node types tested |
| **DataHub Controls**   | Editor, toolbox, toolbar specs       | Good coverage         |
| **Workspace Nodes**    | Edges, drawers, parts specs          | Partial coverage      |
| **Workspace Controls** | Toolbar, layout, wizard specs        | Good coverage         |

**Notable:** The `useAccessibleDraggable` hook provides a keyboard-driven alternative to mouse drag-and-drop for the mapping transformation UI, demonstrating intentional a11y investment.

#### Gaps and Limitations

| Gap                         | Current State                       | React Flow Support      |
| :-------------------------- | :---------------------------------- | :---------------------- |
| **Node Focus Traversal**    | `nodesFocusable` not explicitly set | Available in v12        |
| **Edge Focus Traversal**    | `edgesFocusable` not explicitly set | Available in v12        |
| **Selection Announcements** | Custom implementation needed        | Partial support         |
| **Complex Interactions**    | Drag-to-connect requires mouse      | No keyboard alternative |
| **Screen Reader Testing**   | Not automated                       | Manual testing required |

#### Revised Comparison

| Aspect                  | Visual Flow (Current)                                                        | Resource Listings (Proposed) |
| :---------------------- | :--------------------------------------------------------------------------- | :--------------------------- |
| **Keyboard Navigation** | Partial \- hotkeys for actions, needs node focus traversal                   | Native table/form support    |
| **Screen Reader**       | Basic ARIA, needs testing/refinement                                         | Full semantic support        |
| **Focus Management**    | Manual implementation required                                               | Standard browser behavior    |
| **Drag-and-Drop**       | Custom `useAccessibleDraggable` for mappings; connect edges still mouse-only | Not required                 |
| **A11y Testing**        | \~30+ axe tests on components                                                | Similar effort               |
| **WCAG Compliance**     | Likely AA with improvements; AAA challenging                                 | Easier AA/AAA path           |

#### Recommendations for Improving Flow A11y

1. **Enable node/edge focus:** Set `nodesFocusable={true}` and `edgesFocusable={true}`
2. **Test with screen readers:** Add manual testing with VoiceOver/NVDA
3. **Keyboard connect alternative:** Consider keyboard-driven node connection mode
4. **Focus indicators:** Ensure visible focus rings on nodes/edges
5. **Live regions:** Add ARIA live regions for state change announcements

---

## 4\. Product Management Considerations

### 4.1 User Segment Analysis

| User Type               | Current UX Preference        | Proposed UX Benefit         |
| :---------------------- | :--------------------------- | :-------------------------- |
| **First-time users**    | Overwhelmed by canvas        | Familiar CRUD patterns      |
| **Power users**         | Prefer visual manipulation   | May resist change           |
| **API-first users**     | Ignore UI                    | Neutral (API unchanged)     |
| **Mobile/tablet users** | Poor experience              | Improved usability          |
| **Accessibility needs** | Partial support (improvable) | Easier full compliance path |

### 4.2 Feature Parity Matrix

Resources in DataHub that would need RJSF schemas:

| Resource        | Current Editing      | RJSF Schema Exists | Complexity |
| :-------------- | :------------------- | :----------------- | :--------- |
| Data Policy     | Panel \+ connections | Partial            | High       |
| Behavior Policy | Panel \+ FSM         | No                 | Very High  |
| Schema          | Panel                | Partial            | Medium     |
| Script          | Panel \+ Monaco      | Partial            | Medium     |
| Validator       | Panel                | No                 | Medium     |
| Operation       | Panel                | No                 | Medium     |
| Topic Filter    | Inline               | No                 | Low        |
| Client Filter   | Inline               | No                 | Low        |

### 4.3 Migration Strategy Options

#### Option A: Progressive Extension (Recommended for Risk Mitigation)

1. **Phase 1:** Add resource listings as _alternative_ views
2. **Phase 2:** Gather user feedback and usage metrics
3. **Phase 3:** Make resource listings the default for new users
4. **Phase 4:** Optionally deprecate flow views based on data

**Pros:** Low risk, data-driven decisions, maintains power user satisfaction **Cons:** Maintenance overhead, potential confusion with two UX modes

#### Option B: Full Switch (For Resource Constraints)

1. **Phase 1:** Build complete resource listing infrastructure
2. **Phase 2:** Migrate all functionality with feature flags
3. **Phase 3:** Remove React Flow-based views entirely

**Pros:** Single UX to maintain, cleaner architecture **Cons:** High risk of user backlash, significant dev investment upfront

#### Option C: Feature-Specific Approach

- **Workspace:** Keep flow view (topology is core value)
- **DataHub:** Switch to resource listings (policies as resources)

**Pros:** Optimizes for each feature's nature **Cons:** Inconsistent UX across product

---

## 5\. Potential Gains Analysis: Switch vs. Extend

### 5.1 Quantitative Gains (Full Switch)

| Metric                 | Current                 | After Switch  | Improvement                  |
| :--------------------- | :---------------------- | :------------ | :--------------------------- |
| Bundle Size            | \~2.5MB                 | \~2.2MB       | \~12% reduction              |
| Components to Maintain | \~500                   | \~250         | \~50% reduction              |
| Test Files             | \~250                   | \~180         | \~28% reduction              |
| External Dependencies  | React Flow, Elkjs, etc. | Standard only | \~3-4 fewer libs             |
| Time to First Render   | Variable                | Faster        | Better perceived performance |

### 5.2 Qualitative Gains

| Aspect                   | Extension Approach      | Full Switch                   |
| :----------------------- | :---------------------- | :---------------------------- |
| **Developer Onboarding** | Complex (learn both)    | Simpler (standard patterns)   |
| **Bug Surface Area**     | Large (dual paths)      | Reduced (single path)         |
| **Feature Consistency**  | Low (UX divergence)     | High (unified patterns)       |
| **Chakra v3 Migration**  | Complex (dual styling)  | Simpler (standard components) |
| **RJSF Upgrade**         | Complex (flow \+ forms) | Simpler (forms only)          |
| **OpenAPI Migration**    | Neutral                 | Neutral                       |

### 5.3 Risk Analysis

| Risk                           | Extension | Full Switch |
| :----------------------------- | :-------- | :---------- |
| **User Backlash**              | Low       | High        |
| **Data Loss During Migration** | Low       | Medium      |
| **Feature Regression**         | Low       | High        |
| **Timeline Overrun**           | Medium    | High        |
| **Technical Debt**             | Increases | Decreases   |

---

## 6\. Recommendations

### 6.1 Short-term (0-3 months)

1. **Document all node types and their RJSF schema requirements**
2. **Audit existing resource listing components** for reuse
3. **Create proof-of-concept** for DataHub policy as RJSF form
4. **User research** on first-time user experience pain points

### 6.2 Medium-term (3-6 months)

1. **Implement resource listings as alternative views** (Phase 1 of Option A)
2. **Add analytics** to track UX preference and task completion rates
3. **Prepare RJSF schemas** for all DataHub resources

### 6.3 Long-term (6-12 months)

1. **Data-driven decision** on full switch vs. permanent dual-UX
2. **If switching:** Complete migration with feature flags
3. **If extending:** Optimize both UX paths for their target users

---

## 7\. RJSF Integration Considerations

### 7.1 Current RJSF Custom Components

The application has extensive RJSF customization that must work in any new resource editors:

**Custom Widgets:**

- `AdapterTagSelect.tsx` \- Tag selection widget
- `EntitySelectWidget.tsx` \- Entity reference selection
- `SchemaWidget.tsx` \- Schema editor widget
- `ToggleWidget.tsx` \- Boolean toggle
- `UpDownWidget.tsx` \- Number input with buttons

**Custom Fields:**

- `CompactArrayField.tsx` \- Compact array editor
- `MqttTransformationField.tsx` \- MQTT transformation config

**Custom Templates:**

- 8 custom templates for form layout
- Compact variants for dense forms

### 7.2 DataHub-Specific RJSF Components

Located in `datahub/designer/datahubRJSFWidgets.tsx`:

- Custom widgets for DataHub-specific form fields
- Integration with Monaco editor for code editing
- Schema validation widgets

### 7.3 Migration Considerations for RJSF Upgrade

The current RJSF v5 → v6 upgrade will affect:

- All custom widget signatures
- Template prop types
- Chakra UI theme integration

**Impact on this decision:**

- Resource listing UX simplifies RJSF upgrade (fewer integration points)
- Flow-based panels have complex RJSF integrations (more migration work)

---

## 8\. Appendix: Existing Resource Patterns to Leverage

### Example: Bridge CRUD Pattern

```
// BridgePage.tsx - Listing container
<PageContainer title="Bridges">
  <BridgeTable />        // Table component
  <SuspenseOutlet />     // Drawer routing
</PageContainer>

// BridgeTable.tsx - Resource listing
<PaginatedTable
  data={bridges}
  columns={columns}
  onRowClick={handleEdit}
/>

// BridgeEditorDrawer.tsx - RJSF-based editing
<Drawer>
  <ChakraRJSForm
    schema={bridgeSchema}
    uiSchema={bridgeUISchema}
    formData={bridge}
    onSubmit={handleSubmit}
  />
</Drawer>
```

This pattern can be replicated for all Workspace and DataHub resources.

---

**End of Analysis**
