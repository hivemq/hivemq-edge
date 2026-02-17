---
title: "Workspace Testing Guide"
author: "Edge Frontend Team"
last_updated: "2026-02-17"
purpose: "Complete testing guide for the Workspace topology visualization"
audience: "Developers writing tests for Workspace features"
maintained_at: "docs/guides/WORKSPACE_TESTING_GUIDE.md"
---

# Workspace Testing Guide

---

## Table of Contents

- [Overview](#overview)
- [Component Testing](#component-testing)
- [E2E Testing](#e2e-testing)
- [Mock Data Reference](#mock-data-reference)
- [Status Testing](#status-testing)
- [Common Pitfalls](#common-pitfalls)
- [Quick Reference](#quick-reference)

---

## Overview

The Workspace is a React Flow-based topology visualization requiring special testing considerations:

**Key Requirements:**
- React Flow context wrapper for component tests
- Complete API intercept setup for E2E tests
- Mock data for adapters, bridges, combiners, pulse
- Status fixtures for runtime and operational testing

**Related Documentation:**
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md) - Architecture overview
- [Testing Guide](./TESTING_GUIDE.md) - General testing patterns
- [Cypress Guide](./CYPRESS_GUIDE.md) - Cypress-specific patterns

---

## Component Testing

### ReactFlowTesting Wrapper

**ALL Workspace node components require the ReactFlowTesting wrapper.**

```typescript
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'

describe('NodeAdapter', () => {
  const mockNode = {
    id: 'adapter-1',
    type: NodeTypes.ADAPTER_NODE,
    position: { x: 0, y: 0 },
    data: {
      id: 'my-adapter',
      type: 'opcua',
      statusModel: {
        runtime: RuntimeStatus.ACTIVE,
        operational: OperationalStatus.ACTIVE,
        source: 'ADAPTER'
      }
    }
  }

  it('should render adapter node', () => {
    cy.mountWithProviders(<NodeAdapter {...mockNode} />, {
      wrapper: ({ children }) => (
        <ReactFlowTesting config={{ initialState: { nodes: [mockNode], edges: [] } }}>
          {children}
        </ReactFlowTesting>
      )
    })

    cy.contains('my-adapter').should('be.visible')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<NodeAdapter {...mockNode} />, {
      wrapper: ({ children }) => (
        <ReactFlowTesting config={{ initialState: { nodes: [mockNode], edges: [] } }}>
          {children}
        </ReactFlowTesting>
      )
    })
    cy.checkAccessibility()
  })
})
```

**Why?** Workspace components use React Flow hooks (`useReactFlow()`, `useNodes()`, etc.) which require React Flow context.

**Location:** `@/__test-utils__/react-flow/ReactFlowTesting.tsx`

### Testing Node Status

```typescript
it('should show error status', () => {
  const errorNode = {
    ...mockNode,
    data: {
      ...mockNode.data,
      statusModel: {
        runtime: RuntimeStatus.ERROR,
        operational: OperationalStatus.INACTIVE,
        source: 'ADAPTER'
      }
    }
  }

  cy.mountWithProviders(<NodeAdapter {...errorNode} />, {
    wrapper: ({ children }) => (
      <ReactFlowTesting config={{ initialState: { nodes: [errorNode], edges: [] } }}>
        {children}
      </ReactFlowTesting>
    )
  })

  // Check for error indicator (implementation-specific)
  cy.get('[data-nodeid="adapter-1"]').should('have.attr', 'data-status', 'ERROR')
})
```

---

## E2E Testing

### Essential Intercepts

**Every workspace E2E test needs these intercepts:**

```typescript
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils'
import { MOCK_PROTOCOL_OPC_UA, MOCK_ADAPTER_OPC_UA } from '@/__test-utils__/adapters'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

beforeEach(() => {
  // Core intercepts (auth, config, etc.)
  cy_interceptCoreE2E()

  // Protocol adapter types
  cy.intercept('/api/v1/management/protocol-adapters/types', {
    items: [MOCK_PROTOCOL_OPC_UA]
  }).as('getProtocols')

  // Adapter instances
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [MOCK_ADAPTER_OPC_UA]
  }).as('getAdapters')

  // Bridges
  cy.intercept('/api/v1/management/bridges', {
    items: [mockBridge]
  }).as('getBridges')

  // Combiners
  cy.intercept('/api/v1/data-combining/combiners', {
    items: []
  }).as('getCombiners')

  // Topic filters
  cy.intercept('/api/v1/management/topic-filters', {
    items: []
  })

  // Status polling (return 202 to disable)
  cy.intercept('/api/v1/management/protocol-adapters/status', { statusCode: 202, log: false })
  cy.intercept('/api/v1/management/bridges/status', { statusCode: 202, log: false })

  // Navigate to workspace
  loginPage.visit('/app/workspace')
  loginPage.loginButton.click()
  workspacePage.navLink.click()
})
```

### Operational Status Testing

**To test edge animation (operational status), add per-adapter mapping intercepts:**

```typescript
it('should animate edges for operational adapter', () => {
  // Adapter is ACTIVE (runtime)
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [{
      ...MOCK_ADAPTER_OPC_UA,
      status: {
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED
      }
    }]
  }).as('getAdapters')

  // Adapter has mappings (operational)
  cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
    items: [{ tagName: 'sensor1', topic: 'test/topic' }]
  }).as('getNorthboundMappings')

  cy.wait('@getAdapters')
  cy.wait('@getNorthboundMappings')

  workspacePage.toolbox.fit.click()

  // Edges should be animated
  cy.get('.react-flow__edge.animated').should('exist')
})
```

**Without mappings intercept, edges won't animate even if adapter is ACTIVE.**

### Testing Pulse/Asset Mappers

**Pulse requires capabilities to be enabled BEFORE navigation:**

```typescript
import { cy_interceptPulseWithMockDB, getPulseFactory } from 'cypress/utils/intercept-pulse.utils'
import { drop } from '@mswjs/data'

describe('Workspace with Pulse', () => {
  const mswDB = getPulseFactory()

  beforeEach(() => {
    drop(mswDB)
    cy_interceptCoreE2E()

    // This helper sets up:
    // - Capabilities with MOCK_CAPABILITY_PULSE_ASSETS
    // - Pulse status, managed assets, asset mappers, combiners
    cy_interceptPulseWithMockDB(mswDB, true, true)

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  it('should show Pulse Agent node', () => {
    cy.get('[data-nodetype="PULSE_NODE"]').should('exist')
  })
})
```

**Helper location:** `cypress/utils/intercept-pulse.utils.ts`

### Page Object Usage

```typescript
import { workspacePage, wizardPage } from 'cypress/pages'
```

**`workspacePage`** — `cypress/pages/Workspace/WorkspacePage.ts`

| Selector | Type | Description |
|----------|------|-------------|
| `navLink` | getter | Sidebar navigation link to Workspace |
| `canvas` | getter | React Flow wrapper element (`rf__wrapper`) |
| `edgeNode` | getter | The HiveMQ Edge hub node |
| `adapterNode(id)` | method | Adapter node by adapter ID |
| `bridgeNode(id)` | method | Bridge node by bridge ID |
| `bridgeNodes()` | method | All bridge nodes |
| `deviceNode(id)` | method | Device node by adapter ID |
| `combinerNode(id)` | method | Combiner node by ID |
| `combinerNodeContent(id)` | method | Returns `{ title, topic }` getters for combiner content |
| `nodeToolbar` | getter | Floating toolbar appearing on node selection |
| `toolbox.fit` | nested | "Fit to canvas" control button |
| `toolbox.zoomIn` | nested | Zoom in control |
| `canvasToolbar.expandButton` | nested | Expand the layout controls toolbar |
| `canvasToolbar.collapseButton` | nested | Collapse the layout controls toolbar |
| `layoutControls.panel` | nested | Layout controls panel container |
| `layoutControls.algorithmSelector` | nested | Layout algorithm `<select>` |
| `layoutControls.applyButton` | nested | Apply selected layout |
| `layoutControls.presetsButton` | nested | Open presets menu |
| `layoutControls.optionsButton` | nested | Open options drawer |
| `layoutControls.presetsMenu.saveOption` | nested | Save current layout as preset |
| `layoutControls.presetsMenu.presets` | nested | All saved preset items |
| `layoutControls.presetsMenu.presetItem(name)` | nested method | Open specific preset by name |
| `layoutControls.presetsMenu.presetItemDelete(name)` | nested method | Delete specific preset |
| `layoutControls.presetsMenu.emptyMessage` | nested | Empty state when no presets exist |
| `layoutControls.optionsDrawer.drawer` | nested | Layout options drawer dialog |
| `layoutControls.optionsDrawer.cancelButton` | nested | Cancel changes |
| `layoutControls.optionsDrawer.applyButton` | nested | Apply options |
| `layoutControls.savePresetModal.nameInput` | nested | Preset name input |
| `layoutControls.savePresetModal.saveButton` | nested | Save preset button |
| `toolbar.title` | nested | Node toolbar title text |
| `toolbar.topicFilter` | nested | "Manage topic filters" button |
| `toolbar.combine` | nested | Add to combiner button |
| `toolbar.group` | nested | Group selected nodes button |
| `toolbar.overview` | nested | Open node overview panel |
| `duplicateCombinerModal.modal` | nested | Duplicate detection modal |
| `duplicateCombinerModal.title` | nested | Modal title |
| `duplicateCombinerModal.buttons.cancel` | nested | Cancel without action |
| `duplicateCombinerModal.buttons.createNew` | nested | Create duplicate anyway |
| `duplicateCombinerModal.buttons.useExisting` | nested | Navigate to existing combiner |
| `act.selectReactFlowNodes(ids[])` | action method | Multi-select nodes with meta-click |

**Usage example:**

```typescript
it('should navigate workspace', () => {
  // Canvas controls
  workspacePage.toolbox.fit.click()

  // Select node
  workspacePage.adapterNode('opcua-1').click()

  // Toolbar appears on selection
  workspacePage.toolbar.title.should('have.text', 'opcua-1')
  workspacePage.toolbar.overview.click()

  // Layout controls
  workspacePage.canvasToolbar.expandButton.click()
  workspacePage.layoutControls.algorithmSelector.select('DAGRE_TB')
  workspacePage.layoutControls.applyButton.click()
  workspacePage.edgeNode.should('be.visible')
})
```

**`wizardPage`** — `cypress/pages/Workspace/WizardPage.ts`

| Selector | Type | Description |
|----------|------|-------------|
| `createEntityButton` | getter | The "+" create entity button |
| `wizardMenu.selectOption(type)` | method | Click wizard type option (`ADAPTER`\|`BRIDGE`\|`COMBINER`\|`GROUP`) |
| `progressBar.nextButton` | nested | Wizard "Next" button |
| `progressBar.backButton` | nested | Wizard "Back" button |
| `progressBar.completeButton` | nested | Wizard "Complete" button |
| `progressBar.cancelButton` | nested | Cancel wizard |
| `canvas.node(nodeId)` | nested method | Get canvas node by ID (selection state) |
| `canvas.nodeIsSelectable(id)` | nested method | Assert node is selectable |
| `canvas.ghostNode` | nested | Ghost node preview |
| `canvas.ghostEdges` | nested | Ghost edge previews |
| `adapterConfig.panel` | nested | Adapter configuration panel |
| `adapterConfig.protocolSelectors` | nested | Protocol selection list |
| `adapterConfig.protocolSelector(type)` | nested method | Specific protocol item |
| `adapterConfig.selectProtocol(type)` | nested method | Create button for protocol |
| `adapterConfig.adapterNameInput` | nested | Adapter ID input |
| `adapterConfig.setAdapterName(name)` | nested method | Clear and type adapter name |
| `bridgeConfig.bridgeNameInput` | nested | Bridge ID input |
| `bridgeConfig.setBridgeId(id)` | nested method | Set bridge ID |
| `bridgeConfig.setHost(host)` | nested method | Set broker host |
| `bridgeConfig.setPort(port)` | nested method | Set broker port |
| `selectionPanel.panel` | nested | Selection panel container |
| `selectionPanel.selectedCount` | nested | Count of selected nodes |
| `selectionPanel.selectedNodes` | nested | `<li>` items for selected nodes |
| `selectionPanel.selectedNode(id)` | nested method | Specific selected node |
| `selectionPanel.nextButton` | nested | "Next" in selection panel |
| `combinerConfig.combinerNameInput` | nested | Combiner name input |
| `groupConfig.titleInput` | nested | Group title input |
| `groupConfig.submitButton` | nested | Create group submit |
| `completion.successMessage` | nested | Wizard success state |
| `completion.closeWizardButton` | nested | Close after completion |
| `completion.entityAppearsOnCanvas(id)` | nested method | Assert entity visible on canvas |

### Stable Selectors

**When page objects don't have the selector you need:**

```typescript
// Node types
cy.get('[data-nodetype="EDGE_NODE"]')
cy.get('[data-nodetype="ADAPTER_NODE"]')
cy.get('[data-nodetype="BRIDGE_NODE"]')
cy.get('[data-nodetype="COMBINER_NODE"]')
cy.get('[data-nodetype="PULSE_NODE"]')

// Specific node by ID
cy.get('[data-nodeid="adapter-id"]')

// Edges
cy.get('.react-flow__edge')
cy.get('.react-flow__edge.animated')
cy.get('.react-flow__edge path') // For stroke checks

// Canvas
cy.get('.react-flow__viewport')
cy.get('[role="application"][data-testid="rf__wrapper"]')
```

---

## Mock Data Reference

### Mock Data Locations

```
src/__test-utils__/adapters/
├── index.ts              # Exports all adapter mocks
├── opc-ua.ts            # MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA
├── modbus.ts            # MOCK_ADAPTER_MODBUS, MOCK_PROTOCOL_MODBUS
├── simulation.ts        # MOCK_ADAPTER_SIMULATION
├── s7.ts                # MOCK_ADAPTER_S7
└── http.ts              # MOCK_ADAPTER_HTTP

src/api/hooks/
├── useProtocolAdapters/__handlers__/   # mockAdapter_OPCUA
├── useGetBridges/__handlers__/         # mockBridge
└── useConnection/__handlers__/         # Status fixtures
```

### Adapter Mock Structure

```typescript
import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters'

const adapter: Adapter = {
  id: 'adapter-id',
  type: 'opcua',
  config: {
    id: 'adapter-id',
    uri: 'opc.tcp://host:4840',
    // ... adapter-specific config
  },
  status: {
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED,
    id: 'adapter-id',
    type: 'adapter'
  }
}
```

### Bridge Mock Structure

```typescript
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'

const bridge: Bridge = {
  id: 'bridge-id',
  host: 'remote.broker.com',
  port: 1883,
  status: {
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED
  },
  remoteSubscriptions: [
    { filters: ['remote/#'], destination: 'local/topic' }
  ]
}
```

### Status Fixtures

```typescript
import { Status } from '@/api/__generated__'

// ERROR status
{
  connection: Status.connection.ERROR,
  runtime: Status.runtime.STOPPED,
  message: 'Connection timeout'
}

// ACTIVE status
{
  connection: Status.connection.CONNECTED,
  runtime: Status.runtime.STARTED
}

// INACTIVE status
{
  connection: Status.connection.DISCONNECTED,
  runtime: Status.runtime.STOPPED
}
```

---

## Status Testing

### Dual-Status System

**Runtime Status** (edge color):
- ERROR → Red
- ACTIVE → Green
- INACTIVE → Gray

**Operational Status** (edge animation):
- ACTIVE → Animated (flowing dots)
- INACTIVE → Static
- ERROR → Static

### Testing Status Combinations

#### ERROR Adapter

```typescript
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [{
    ...mockAdapter,
    status: {
      connection: Status.connection.ERROR,
      runtime: Status.runtime.STOPPED,
      message: 'Connection failed'
    }
  }]
})

// Check for red edges
cy.get('.react-flow__edge path').should('have.attr', 'stroke')
// Check no animation
cy.get('.react-flow__edge.animated').should('not.exist')
```

#### ACTIVE Operational

```typescript
// Runtime: ACTIVE
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [{
    ...mockAdapter,
    status: {
      connection: Status.connection.CONNECTED,
      runtime: Status.runtime.STARTED
    }
  }]
})

// Operational: HAS mappings
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
  items: [{ tagName: 'sensor', topic: 'data/topic' }]
})

// Green animated edges
cy.get('.react-flow__edge.animated').should('exist')
```

#### ACTIVE Non-Operational

```typescript
// Runtime: ACTIVE
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [{
    ...mockAdapter,
    status: {
      connection: Status.connection.CONNECTED,
      runtime: Status.runtime.STARTED
    }
  }]
})

// Operational: NO mappings
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
  items: []
})

// Green static edges (no animation)
cy.get('.react-flow__edge').should('exist')
cy.get('.react-flow__edge.animated').should('not.exist')
```

### Status Reference Table

| Connection | Runtime | Mappings | Color | Animated | Status Name |
|------------|---------|----------|-------|----------|-------------|
| ERROR | STOPPED | N/A | Red | No | ERROR |
| CONNECTED | STARTED | Yes | Green | Yes | ACTIVE Operational |
| CONNECTED | STARTED | No | Green | No | ACTIVE Non-operational |
| DISCONNECTED | STOPPED | N/A | Gray | No | INACTIVE |

---

## Common Pitfalls

### 1. Missing ReactFlowTesting Wrapper

**Problem:** `TypeError: Cannot read property 'useStore' of null`

**Solution:** Wrap component with ReactFlowTesting in component tests

```typescript
// ❌ Wrong
cy.mountWithProviders(<NodeAdapter {...mockNode} />)

// ✅ Correct
cy.mountWithProviders(<NodeAdapter {...mockNode} />, {
  wrapper: ({ children }) => (
    <ReactFlowTesting config={{ initialState: { nodes: [mockNode], edges: [] } }}>
      {children}
    </ReactFlowTesting>
  )
})
```

### 2. Missing Mapping Intercepts

**Problem:** Edges don't animate even though adapter is ACTIVE

**Solution:** Add northbound/southbound mapping intercepts

```typescript
// ❌ Wrong - No mappings
cy.intercept('/api/v1/management/protocol-adapters/adapters', { items: [adapter] })

// ✅ Correct - Add mappings
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
  items: [{ tagName: 'tag', topic: 'topic' }]
})
```

### 3. Not Waiting for Layout

**Problem:** Screenshots taken before React Flow layout completes

**Solution:** Wait for layout to settle

```typescript
// ❌ Wrong
workspacePage.toolbox.fit.click()
cy.screenshot('workspace')

// ✅ Correct
workspacePage.toolbox.fit.click()
cy.wait(1000) // Allow layout to settle
cy.screenshot('workspace')
```

### 4. Testing Animation with Static Checks

**Problem:** Checking for animation class instead of animated edges

**Solution:** Check for animated edges directly

```typescript
// ❌ Wrong
cy.get('.react-flow__edge').should('have.class', 'animated')

// ✅ Correct
cy.get('.react-flow__edge.animated').should('exist')
```

### 5. Missing Status Endpoints

**Problem:** Nodes show INACTIVE when they should be ACTIVE

**Solution:** Mock status endpoints correctly

```typescript
// For tests, either:
// Option 1: Disable status polling
cy.intercept('/api/v1/management/protocol-adapters/status', { statusCode: 202, log: false })

// Option 2: Return status data
cy.intercept('/api/v1/management/protocol-adapters/status', {
  items: [{
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED,
    id: 'adapter-id'
  }]
})
```

### 6. Pulse Without Capabilities

**Problem:** Pulse node doesn't render

**Solution:** Enable capabilities BEFORE navigation

```typescript
// ❌ Wrong - No capabilities
cy.intercept('/api/v1/frontend/capabilities', { capabilities: [] })

// ✅ Correct - Include Pulse capability
import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'

cy.intercept('/api/v1/frontend/capabilities', {
  capabilities: [MOCK_CAPABILITY_PULSE_ASSETS]
})
```

---

## Quick Reference

### Test Setup Checklist

- [ ] Import mocks from `@/__test-utils__/adapters`
- [ ] Call `cy_interceptCoreE2E()` for core intercepts
- [ ] Add workspace-specific intercepts (adapters, bridges, combiners)
- [ ] Add per-adapter mapping intercepts if testing animation
- [ ] For Pulse: Use `cy_interceptPulseWithMockDB()` or enable capabilities manually
- [ ] Navigate via `loginPage` → `workspacePage`
- [ ] Call `workspacePage.toolbox.fit.click()` to fit canvas
- [ ] Wait for layout (`cy.wait(1000)`) before screenshots
- [ ] Check accessibility with `cy.injectAxe()` and `cy.checkAccessibility()`

### Component Test Template

```typescript
import { ReactFlowTesting } from '@/__test-utils__/react-flow/ReactFlowTesting'

describe('ComponentName', () => {
  const mockNode = { /* ... */ }

  it('should render', () => {
    cy.mountWithProviders(<Component {...mockNode} />, {
      wrapper: ({ children }) => (
        <ReactFlowTesting config={{ initialState: { nodes: [mockNode], edges: [] } }}>
          {children}
        </ReactFlowTesting>
      )
    })
    cy.contains('expected content')
  })

  it('should be accessible', () => {
    cy.injectAxe()
    cy.mountWithProviders(<Component {...mockNode} />, {
      wrapper: ({ children }) => (
        <ReactFlowTesting config={{ initialState: { nodes: [mockNode], edges: [] } }}>
          {children}
        </ReactFlowTesting>
      )
    })
    cy.checkAccessibility()
  })
})
```

### E2E Test Template

```typescript
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils'
import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters'
import { loginPage, workspacePage } from 'cypress/pages'

describe('Feature', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA]
    }).as('getProtocols')

    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [MOCK_ADAPTER_OPC_UA]
    }).as('getAdapters')

    cy.intercept('/api/v1/management/bridges', { items: [] })
    cy.intercept('/api/v1/data-combining/combiners', { items: [] })

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  it('should test feature', () => {
    cy.wait('@getAdapters')
    workspacePage.toolbox.fit.click()

    // Your test assertions
  })
})
```

---

## Related Documentation

**Architecture:**
- [Workspace Architecture](../architecture/WORKSPACE_ARCHITECTURE.md)

**Guides:**
- [Testing Guide](./TESTING_GUIDE.md)
- [Cypress Guide](./CYPRESS_GUIDE.md)
- [Design Guide](./DESIGN_GUIDE.md)

**Technical:**
- [Technical Stack](../technical/TECHNICAL_STACK.md)
