# Feature Distribution Analysis: Workspace vs DataHub vs Core App

## Executive Summary

The HiveMQ Edge frontend is structured into three main areas with distinct characteristics:

| Feature       | Files   | % of Total | Tests  | Complexity                  |
| :------------ | :------ | :--------- | :----- | :-------------------------- |
| **Workspace** | \~200   | 12%        | \~112  | High (React Flow \+ Layout) |
| **DataHub**   | \~321   | 20%        | \~123  | High (Policy Designer)      |
| **Core App**  | \~1,095 | 68%        | \~350+ | Medium                      |

**Total**: 1,616 source files (excluding generated code)

---

## Workspace Module

### Overview

The Workspace provides a visual canvas for managing IoT infrastructure including adapters, bridges, groups, and combiners.

**Location**: `src/modules/Workspace/`

### File Breakdown

| Category       | Count | Subcategories                                                           |
| :------------- | :---- | :---------------------------------------------------------------------- |
| **Components** | \~95  | nodes, edges, drawers, controls, wizard, filters, layout, parts, modals |
| **Hooks**      | \~21  | stores, context, layout engine                                          |
| **Utils**      | \~34  | status, layout algorithms, topics, groups                               |
| **Types**      | \~5   | core types, status types, layout types                                  |
| **Schemas**    | \~3   | layout options                                                          |

### Component Categories

#### Nodes (`components/nodes/`) \- 15 files

- `NodeAdapter.tsx` \- Protocol adapter visualization
- `NodeBridge.tsx` \- MQTT bridge node
- `NodeCombiner.tsx` \- Data combiner node
- `NodeDevice.tsx` \- Device node
- `NodeEdge.tsx` \- HiveMQ Edge node (central)
- `NodeGroup.tsx` \- Grouping container
- `NodeHost.tsx` \- Host node
- `NodeListener.tsx` \- MQTT listener
- `NodePulse.tsx` \- Pulse agent node
- `NodeAssets.tsx` \- Managed assets
- `ContextualToolbar.tsx` \- Node toolbar
- `index.ts` \- Exports

#### Edges (`components/edges/`) \- 6 files

- `DynamicEdge.tsx` \- Main edge component
- `MonitoringEdge.tsx` \- Monitoring connection
- `DataPolicyEdgeCTA.tsx` \- Data policy call-to-action
- `ObservabilityEdgeCTA.tsx` \- Observability CTA

#### Controls (`components/controls/`) \- 6 files

- `CanvasControls.tsx` \- Zoom, pan controls
- `CanvasToolbar.tsx` \- Main toolbar
- `ConfigurationPanelController.tsx` \- Property panel controller
- `SelectionListener.tsx` \- Selection handling
- `StatusListener.tsx` \- Status updates

#### Drawers (`components/drawers/`) \- 9 files

- `DevicePropertyDrawer.tsx`
- `EdgePropertyDrawer.tsx`
- `GroupPropertyDrawer.tsx`
- `LinkPropertyDrawer.tsx`
- `NodePropertyDrawer.tsx`
- `PulsePropertyDrawer.tsx`
- `WorkspaceOptionsDrawer.tsx`

#### Wizard (`components/wizard/`) \- 20+ files

Entity creation wizard with multiple steps:

- `CreateEntityButton.tsx`
- `WizardAdapterConfiguration.tsx`
- `WizardBridgeConfiguration.tsx`
- `WizardCombinerConfiguration.tsx`
- `WizardConfigurationPanel.tsx`
- `WizardGroupConfiguration.tsx`
- `WizardProgressBar.tsx`
- `WizardSelectionPanel.tsx`
- `WizardSelectionRestrictions.tsx`
- `GhostNodeRenderer.tsx`
- `AutoIncludedNodesList.tsx`
- `steps/WizardAdapterForm.tsx`
- `steps/WizardBridgeForm.tsx`
- `steps/WizardGroupForm.tsx`
- `steps/WizardProtocolSelector.tsx`
- `hooks/useComplete*Wizard.ts` (4 files)
- `utils/` (5 files)

#### Filters (`components/filters/`) \- 14 files

- `FilterEntities.tsx`
- `FilterProtocol.tsx`
- `FilterStatus.tsx`
- `FilterTopics.tsx`
- `FilterSelection.tsx`
- `ConfigurationSave.tsx`
- `QuickFilters.tsx`
- `SearchEntities.tsx`
- `DrawerFilterToolbox.tsx`
- `OptionsFilter.tsx`
- `WrapperCriteria.tsx`
- `ApplyFilter.tsx`

#### Layout (`components/layout/`) \- 6 files

- `ApplyLayoutButton.tsx`
- `LayoutOptionsDrawer.tsx`
- `LayoutPresetsManager.tsx`
- `LayoutSelector.tsx`

### Hooks

| Hook                        | Purpose                           |
| :-------------------------- | :-------------------------------- |
| `useWorkspaceStore.ts`      | Zustand store for workspace state |
| `useWizardStore.ts`         | Wizard state management           |
| `useContextMenu.ts`         | Context menu handling             |
| `useEdgeFlowContext.ts`     | React Flow context                |
| `useGetFlowElements.ts`     | Flow element retrieval            |
| `useGetPoliciesMatching.ts` | Policy matching                   |
| `useLayoutEngine.ts`        | Layout algorithm execution        |
| `EdgeFlowProvider.tsx`      | Context provider                  |
| `FlowContext.tsx`           | Context definition                |

### Layout Utilities (`utils/layout/`)

| Algorithm        | File                         |
| :--------------- | :--------------------------- |
| Dagre            | `dagre-layout.ts`            |
| Cola Force       | `cola-force-layout.ts`       |
| Cola Constrained | `cola-constrained-layout.ts` |
| Radial Hub       | `radial-hub-layout.ts`       |
| Manual           | `manual-layout.ts`           |
| Layout Registry  | `layout-registry.ts`         |
| Constraint Utils | `constraint-utils.ts`        |
| Cola Utils       | `cola-utils.ts`              |

### Test Coverage

| Test Type                 | Count                     |
| :------------------------ | :------------------------ |
| Unit Tests (Vitest)       | \~45 `.spec.ts` files     |
| Component Tests (Cypress) | \~50 `.spec.cy.tsx` files |
| E2E Tests (Cypress)       | 12 specs                  |

**E2E Test Files**:

- `workspace.spec.cy.ts`
- `workspace-group-nodes.spec.cy.ts`
- `workspace-layout-accessibility.spec.cy.ts`
- `workspace-layout-basic.spec.cy.ts`
- `workspace-layout-options.spec.cy.ts`
- `workspace-layout-presets.spec.cy.ts`
- `workspace-layout-shortcuts.spec.cy.ts`
- `workspace-pr-screenshots.spec.cy.ts`
- `workspace-status.spec.cy.ts`
- `duplicate-combiner.spec.cy.ts`
- `wizard/wizard-create-adapter.spec.cy.ts`
- `wizard/wizard-create-bridge.spec.cy.ts`
- `wizard/wizard-create-combiner.spec.cy.ts`
- `wizard/wizard-create-group.spec.cy.ts`

---

## DataHub Extension

### Overview

DataHub provides a visual policy designer for creating data policies, behavior policies, and associated resources like schemas and scripts.

**Location**: `src/extensions/datahub/`

### File Breakdown

| Category           | Count | Subcategories                                                                                               |
| :----------------- | :---- | :---------------------------------------------------------------------------------------------------------- |
| **Designer Nodes** | \~50  | behavior_policy, client_filter, data_policy, operation, schema, script, topic_filter, transition, validator |
| **Components**     | \~120 | controls, edges, editors, forms, fsm, helpers, interpolation, nodes, pages, toolbar                         |
| **API Hooks**      | \~45  | Per-service hooks                                                                                           |
| **Hooks**          | \~10  | Draft store, policy checks                                                                                  |
| **Utils**          | \~25  | Policy, node, theme, store utilities                                                                        |
| **Config**         | \~3   | Nodes, schemas, editors config                                                                              |

### Designer Nodes (`designer/`)

Each policy element has its own folder with:

- Node component (`.tsx`)
- Panel component (`.tsx`)
- Utils (`.ts`)
- Types/Schema (`.ts`)
- Tests (`.spec.ts`, `.spec.cy.tsx`)

| Node Type          | Files |
| :----------------- | :---- |
| `behavior_policy/` | 6     |
| `client_filter/`   | 6     |
| `data_policy/`     | 6     |
| `operation/`       | 8     |
| `schema/`          | 6     |
| `script/`          | 6     |
| `topic_filter/`    | 5     |
| `transition/`      | 5     |
| `validator/`       | 5     |

### Components

#### Controls (`components/controls/`) \- 15+ files

- `DesignerToolbox.tsx` \- Node palette
- `DesignerMiniMap.tsx` \- Mini navigation
- `DesignerCheatSheet.tsx` \- Keyboard shortcuts
- `PropertyPanelController.tsx` \- Property editing
- `DryRunPanelController.tsx` \- Policy testing
- `CanvasControls.tsx` \- Zoom/pan
- `CopyPasteListener.tsx` \- Copy/paste handling
- `CopyPasteStatus.tsx` \- Status indicator
- `DeleteListener.tsx` \- Delete handling
- `Minimap.tsx` \- Alternative minimap
- `ToolboxNodes.tsx` \- Node list
- `ToolboxSelectionListener.tsx` \- Selection
- `ToolGroup.tsx` \- Tool grouping
- `ToolItem.tsx` \- Single tool

#### Forms (`components/forms/`) \- 20 files

Custom form widgets for policy configuration:

- `AdapterSelect.tsx`
- `CodeEditor.tsx` (Monaco integration)
- `FunctionCreatableSelect.tsx`
- `MessageInterpolationTextArea.tsx`
- `MessageTypeSelect.tsx`
- `MetricCounterInput.tsx`
- `ReactFlowSchemaForm.tsx`
- `ResourceNameCreatableSelect.tsx`
- `TransitionSelect.tsx`
- `VersionManagerSelect.tsx`
- `monaco/` (6+ files for Monaco configuration)

#### Pages (`components/pages/`) \- 10 files

- `DataHubListings.tsx`
- `PolicyEditor.tsx`
- `PolicyEditorLoader.tsx`
- `PolicyTable.tsx`
- `SchemaTable.tsx`
- `ScriptTable.tsx`

#### Toolbar (`components/toolbar/`) \- 8 files

- `PolicyToolbar.tsx`
- `NodeDatahubToolbar.tsx`
- `ToolbarClear.tsx`
- `ToolbarDryRun.tsx`
- `ToolbarPublish.tsx`
- `ToolbarShowReport.tsx`

#### Helpers (`components/helpers/`) \- 18 files

- `CopyButton.tsx`
- `DataHubListAction.tsx`
- `DraftCTA.tsx`
- `DraftStatus.tsx`
- `ExpandVersionButton.tsx`
- `LicenseWarning.tsx`
- `NodeIcon.tsx`
- `NodeParams.tsx`
- `PolicyErrorReport.tsx`
- `PolicyJsonView.tsx`
- `PolicyOverview.tsx`
- `PolicySummaryReport.tsx`
- `ResourcesBreakdown.tsx`
- `ApiErrorToastDevMode.tsx`

### API Hooks (`api/hooks/`)

| Service                        | Hooks |
| :----------------------------- | :---- |
| DataHubBehaviorPoliciesService | 5     |
| DataHubDataPoliciesService     | 5     |
| DataHubFsmService              | 1     |
| DataHubFunctionsService        | 2     |
| DataHubInterpolationService    | 1     |
| DataHubSchemasService          | 4     |
| DataHubScriptsService          | 4     |
| DataHubStateService            | 1     |

### Hooks

| Hook                             | Purpose            |
| :------------------------------- | :----------------- |
| `useDataHubDraftStore.ts`        | Draft policy state |
| `usePolicyChecksStore.ts`        | Validation state   |
| `usePolicyDryRun.ts`             | Dry run execution  |
| `usePolicyGuards.ts`             | Route guards       |
| `useGetFilteredFunctions.ts`     | Function filtering |
| `useFilteredFunctionsFetcher.ts` | Function loading   |

### Test Coverage

| Test Type                 | Count                     |
| :------------------------ | :------------------------ |
| Unit Tests (Vitest)       | \~40 `.spec.ts` files     |
| Component Tests (Cypress) | \~80 `.spec.cy.tsx` files |
| E2E Tests (Cypress)       | 3 specs                   |

**E2E Test Files**:

- `datahub.spec.cy.ts`
- `policy-report.spec.cy.ts`
- `resource-edit-flow.spec.cy.ts`

---

## Core Application

### Overview

The remaining codebase supporting general application functionality.

### Module Distribution

| Module              | Files | Purpose                 |
| :------------------ | :---- | :---------------------- |
| `App/`              | \~5   | Main app, routes        |
| `Auth/`             | \~10  | Authentication          |
| `Bridges/`          | \~50  | Bridge management       |
| `Dashboard/`        | \~15  | Navigation, layout      |
| `Device/`           | \~20  | Device/tag management   |
| `DomainOntology/`   | \~35  | Domain visualization    |
| `EventLog/`         | \~15  | Event display           |
| `Login/`            | \~10  | Login page              |
| `Mappings/`         | \~40  | Data mappings           |
| `Metrics/`          | \~25  | Metrics display         |
| `Notifications/`    | \~5   | Notification badges     |
| `ProtocolAdapters/` | \~60  | Adapter management      |
| `Pulse/`            | \~45  | Asset monitoring        |
| `Theme/`            | \~15  | Chakra theme            |
| `TopicFilters/`     | \~15  | Topic filter management |
| `Trackers/`         | \~3   | Analytics               |
| `UnifiedNamespace/` | \~15  | UNS configuration       |
| `Welcome/`          | \~10  | Onboarding              |

### Shared Components (`src/components/`)

| Category              | Files  |
| :-------------------- | :----- |
| Chakra wrappers       | \~15   |
| Connection components | \~10   |
| DateTime              | \~15   |
| Icons                 | \~5    |
| Modal                 | \~2    |
| MQTT                  | \~5    |
| PaginatedTable        | \~15   |
| React Flow            | \~5    |
| RJSF                  | \~100+ |

### API Layer

| Category       | Files                       |
| :------------- | :-------------------------- |
| Custom hooks   | \~100+                      |
| Schemas        | \~20                        |
| Types          | \~5                         |
| Generated code | \~210 (excluded from count) |

---

## Migration Implications

### Workspace

- **High Impact**: Heavy React Flow \+ Chakra integration
- **Focus Areas**: Node styling, drawer components, toolbar
- **Risk**: Layout algorithms use theme tokens

### DataHub

- **High Impact**: Complex form components, Monaco editor
- **Focus Areas**: Policy designer, property panels
- **Risk**: Custom RJSF widgets, interpolation editor

### Core App

- **Medium Impact**: Standard Chakra components
- **Focus Areas**: Theme, shared components, RJSF
- **Risk**: Theme customizations, component variants

---

## Metrics Summary

### Lines of Code (Estimated)

| Feature   | Est. Lines |
| :-------- | :--------- |
| Workspace | \~15,000   |
| DataHub   | \~33,000   |
| Core App  | \~60,000   |
| Generated | \~20,000   |
| **Total** | \~128,000  |

### Component Tests

| Feature   | Tests |
| :-------- | :---- |
| Workspace | \~50  |
| DataHub   | \~80  |
| Core App  | \~150 |
| **Total** | \~280 |

### E2E Tests

| Feature   | Tests |
| :-------- | :---- |
| Workspace | 12    |
| DataHub   | 3     |
| Other     | 12    |
| **Total** | 27    |
