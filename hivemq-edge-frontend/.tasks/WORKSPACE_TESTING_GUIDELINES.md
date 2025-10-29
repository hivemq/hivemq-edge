# Workspace Testing Guidelines

**Last Updated:** October 25, 2025  
**Purpose:** Comprehensive guide for testing the HiveMQ Edge Workspace visualization

This document provides a complete reference for writing E2E and component tests for the Workspace module, including mock data structures, intercept patterns, and best practices learned from the codebase.

---

## Table of Contents

- [Workspace Architecture Overview](#workspace-architecture-overview)
- [Mock Data & Handlers](#mock-data--handlers)
- [API Intercept Patterns](#api-intercept-patterns)
- [Page Object Model](#page-object-model)
- [Entity Types & Relationships](#entity-types--relationships)
- [Status System](#status-system)
- [Testing Patterns](#testing-patterns)
- [Common Pitfalls](#common-pitfalls)

---

## Workspace Architecture Overview

### Core Components

The Workspace is a React Flow-based visualization that displays:

1. **Edge Node** (Central hub) - `[data-nodetype="EDGE_NODE"]`
2. **Adapter Nodes** - Protocol adapters (OPC-UA, Modbus, S7, Simulation, HTTP, etc.)
3. **Device Nodes** - Physical/logical devices represented by adapters
4. **Bridge Nodes** - MQTT bridges to remote brokers
5. **Combiner Nodes** - Data transformation/aggregation entities
6. **Group Nodes** - Clustered nodes `[data-nodetype="CLUSTER_NODE"]`
7. **Topic Filter Nodes** - MQTT topic filters
8. **Dynamic Edges** - Connections showing data flow and status

### File Structure

```
src/modules/Workspace/
├── components/
│   ├── nodes/            # Node components (AdapterNode, BridgeNode, etc.)
│   ├── edges/            # Edge components (DynamicEdge)
│   └── toolbars/         # Node toolbars and controls
├── hooks/                # Workspace-specific hooks
└── utils/                # Layout, status, and helper utilities

cypress/e2e/workspace/
├── workspace.spec.cy.ts            # General workspace tests
├── workspace-status.spec.cy.ts     # Status visualization tests
└── workspace-*.spec.cy.ts          # Other workspace feature tests
```

---

## Mock Data & Handlers

### Location of Mock Data

All mock data is centralized in handler files:

```
src/api/hooks/
├── useProtocolAdapters/__handlers__/index.ts
├── useGetBridges/__handlers__/index.ts
├── useConnection/__handlers__/index.ts
└── useTopicFilters/__handlers__/index.ts

src/__test-utils__/
└── adapters/
    ├── index.ts              # Exports all adapter mocks
    ├── opc-ua.ts            # MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA
    ├── modbus.ts            # MOCK_ADAPTER_MODBUS, MOCK_PROTOCOL_MODBUS
    ├── simulation.ts        # MOCK_ADAPTER_SIMULATION, MOCK_PROTOCOL_SIMULATION
    ├── s7.ts                # MOCK_ADAPTER_S7, MOCK_PROTOCOL_S7
    ├── http.ts              # MOCK_ADAPTER_HTTP, MOCK_PROTOCOL_HTTP
    ├── eip.ts               # MOCK_ADAPTER_EIP, MOCK_PROTOCOL_EIP
    ├── ads.ts               # MOCK_ADAPTER_ADS, MOCK_PROTOCOL_ADS
    └── file.ts              # MOCK_ADAPTER_FILE, MOCK_PROTOCOL_FILE
```

### Key Mock Objects

#### Adapter Mock Structure

```typescript
import { mockAdapter_OPCUA } from '@/api/hooks/useProtocolAdapters/__handlers__'
import { MOCK_ADAPTER_OPC_UA, MOCK_PROTOCOL_OPC_UA } from '@/__test-utils__/adapters'

// Basic adapter structure
const adapter: Adapter = {
  id: 'adapter-id',
  type: 'opcua', // or 'modbus', 's7', 'simulation', 'http', etc.
  config: {
    id: 'adapter-id',
    uri: 'opc.tcp://host:port',
    // ... adapter-specific config
  },
  status: {
    connection: Status.connection.CONNECTED, // or ERROR, DISCONNECTED
    runtime: Status.runtime.STARTED, // or STOPPED
    id: 'adapter-id',
    type: 'adapter',
    startedAt: '2024-10-08T10:34:21.692+01',
    message: 'Optional error or status message',
  },
}
```

#### Bridge Mock Structure

```typescript
import { mockBridge, mockBridgeId } from '@/api/hooks/useGetBridges/__handlers__'

const bridge: Bridge = {
  id: 'bridge-id',
  host: 'remote.broker.com',
  port: 1883,
  clientId: 'my-client-id',
  keepAlive: 60,
  sessionExpiry: 3600,
  cleanStart: true,
  status: {
    connection: Status.connection.CONNECTED,
    runtime: Status.runtime.STARTED,
    startedAt: '2023-08-21T11:51:24.234+01',
  },
  localSubscriptions: [
    {
      filters: ['local/topic/#'],
      destination: 'remote/topic',
      maxQoS: 1,
    },
  ],
  remoteSubscriptions: [
    {
      filters: ['remote/commands/#'],
      destination: 'local/commands',
      maxQoS: 0,
    },
  ],
}
```

#### Topic Filter Mock Structure

```typescript
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'

const topicFilter = {
  topicFilter: 'my/topic/filter',
  description: 'Description of the topic filter',
}
```

#### Mapping Mock Structure (Operational Status)

```typescript
// Northbound mappings (device → MQTT)
const northboundMappings = {
  items: [
    {
      tagName: 'sensor-1',
      topic: 'factory/production/line1',
      mqttQos: 0,
      messageExpiryInterval: 4294967295,
    },
  ],
}

// Southbound mappings (MQTT → device)
const southboundMappings = {
  items: [
    {
      tagName: 'actuator-1',
      topicFilter: 'factory/commands/+',
    },
  ],
}
```

---

## API Intercept Patterns

### Essential Workspace Intercepts

Every workspace test needs these core intercepts:

```typescript
// Protocol adapter types (available protocols)
cy.intercept('/api/v1/management/protocol-adapters/types', {
  items: [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_MODBUS, MOCK_PROTOCOL_SIMULATION],
}).as('getProtocols')

// Adapter instances
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [mockAdapter_OPCUA, MOCK_ADAPTER_MODBUS],
}).as('getAdapters')

// Bridges
cy.intercept('/api/v1/management/bridges', {
  items: [mockBridge],
}).as('getBridges')

// Topic filters
cy.intercept('/api/v1/management/topic-filters', {
  items: [MOCK_TOPIC_FILTER],
}).as('getTopicFilters')
```

### Optional/Background Intercepts

These endpoints are polled or called conditionally:

```typescript
// Status endpoints (polled in background - can return 202 to disable)
cy.intercept('/api/v1/management/protocol-adapters/status', { statusCode: 202, log: false })
cy.intercept('/api/v1/management/bridges/status', { statusCode: 202, log: false })

// Frontend notifications
cy.intercept('/api/v1/frontend/notifications', { statusCode: 202, log: false })

// Capabilities check
cy.intercept('/api/v1/frontend/capabilities', { statusCode: 202, log: false })

// Gateway listeners
cy.intercept('/api/v1/gateway/listeners', { statusCode: 202, log: false })

// Combiners
cy.intercept('/api/v1/management/combiners', { statusCode: 202, log: false })

// Pulse/Asset mappers
cy.intercept('/api/v1/management/pulse/asset-mappers', { statusCode: 202, log: false })

// DataHub policies
cy.intercept('/api/v1/data-hub/data-validation/policies', { items: [] })

// GitHub releases check
cy.intercept('https://api.github.com/repos/hivemq/hivemq-edge/releases', { statusCode: 202, log: false })
```

### Pulse Agent & Capabilities Setup

**⚠️ IMPORTANT: Pulse Agent, Asset Mapper, and Combiner nodes require capabilities to be enabled.**

The capabilities intercept **MUST be set up in `beforeEach()` BEFORE navigation**.

#### Option 1: Using the Helper Utility (Recommended)

```typescript
import { cy_interceptPulseWithMockDB, getPulseFactory } from 'cypress/utils/intercept-pulse.utils.ts'
import { drop } from '@mswjs/data'

describe('Workspace with Pulse', () => {
  const mswDB = getPulseFactory()

  beforeEach(() => {
    drop(mswDB)

    cy_interceptCoreE2E()

    // This helper handles ALL Pulse setup:
    // - Capabilities with MOCK_CAPABILITY_PULSE_ASSETS
    // - Pulse status
    // - Managed assets
    // - Asset mappers
    // - Combiners
    cy_interceptPulseWithMockDB(mswDB, true, true)

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  it('should show Pulse Agent node', () => {
    cy.get('[data-nodetype="PULSE_AGENT_NODE"]').should('exist')
  })
})
```

#### Option 2: Manual Capabilities Setup

If you need more control:

```typescript
import { MOCK_CAPABILITY_PULSE_ASSETS } from '@/api/hooks/useFrontendServices/__handlers__'

beforeEach(() => {
  cy_interceptCoreE2E()

  // Enable Pulse capabilities BEFORE navigation
  cy.intercept('/api/v1/frontend/capabilities', (req) => {
    req.reply({
      capabilities: [
        MOCK_CAPABILITY_PULSE_ASSETS,
        // ... other capabilities
      ],
    })
  }).as('getCapabilities')

  // Then set up Pulse endpoints
  cy.intercept('/api/v1/management/pulse/status', {
    status: 'CONNECTED',
    connectedSince: '2025-10-25T10:00:00Z',
  })

  cy.intercept('/api/v1/management/pulse/managed-assets', {
    items: [
      {
        id: 'asset-1',
        name: 'Industrial Pump',
        topic: 'assets/pump-01/telemetry',
        mapping: {
          status: 'STREAMING',
          mappingId: 'mapper-1',
        },
      },
    ],
  })

  cy.intercept('/api/v1/management/pulse/asset-mappers', {
    items: [
      {
        id: 'mapper-1',
        name: 'Asset Mapper',
        sources: {
          items: [
            { type: 'ADAPTER', id: 'my-adapter' },
            { type: 'PULSE_AGENT', id: 'the Pulse Agent' },
          ],
        },
        mappings: { items: [] },
      },
    ],
  })

  cy.intercept('/api/v1/management/combiners', {
    items: [
      {
        id: 'combiner-1',
        name: 'Data Combiner',
        sources: {
          items: [
            { type: 'ADAPTER', id: 'adapter-1' },
            { type: 'ADAPTER', id: 'adapter-2' },
          ],
        },
        mappings: { items: [] },
      },
    ],
  })

  loginPage.visit('/app/workspace')
  loginPage.loginButton.click()
  workspacePage.navLink.click()
})
```

**Key Points:**

- ✅ Capabilities intercept MUST come before `loginPage.visit()`
- ✅ Use `cy_interceptPulseWithMockDB` for complete Pulse setup
- ✅ Pulse Agent node will only render if capabilities include `MOCK_CAPABILITY_PULSE_ASSETS`
- ✅ Asset Mappers appear as special Combiner nodes in the workspace
- ✅ Pulse managed assets don't appear as separate nodes - they're data sources

### Per-Adapter Intercepts (For Operational Status)

To control whether edges are animated (operational ACTIVE):

```typescript
// Northbound mappings - presence determines if adapter is operationally ACTIVE
cy.intercept('/api/v1/management/protocol-adapters/adapters/adapter-id/northboundMappings', {
  items: [
    { tagName: 'tag1', topic: 'topic/path' }, // Has mappings = operational
  ],
}).as('getNorthboundMappings')

// Or empty for non-operational
cy.intercept('/api/v1/management/protocol-adapters/adapters/adapter-id/northboundMappings', {
  items: [], // No mappings = not operational
})

// Southbound mappings
cy.intercept('/api/v1/management/protocol-adapters/adapters/adapter-id/southboundMappings', {
  items: [], // Can return 404 if not applicable
})

// Device tags
cy.intercept('/api/v1/management/protocol-adapters/adapters/adapter-id/tags', {
  items: [], // Can return 404 if not applicable
})
```

### Wildcard Intercepts

For multiple adapters with same response:

```typescript
// All adapters return empty mappings
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
  statusCode: 202,
  log: false,
})

// Dynamic response based on adapter ID
cy.intercept('GET', '/api/v1/management/protocol-adapters/adapters/**/tags', (req) => {
  const pathname = new URL(req.url).pathname
  const id = pathname.split('/')[6]

  req.reply(200, { items: MOCK_DEVICE_TAGS(id, MockAdapterType.OPC_UA) })
})
```

---

## Page Object Model

### WorkspacePage Selectors

The workspace uses data attributes for stable test selectors:

```typescript
// Example usage patterns from existing tests
import { workspacePage } from 'cypress/pages'

// Toolbox controls
workspacePage.toolbox.fit.click() // Fit canvas to viewport
workspacePage.toolbox.zoomIn.click() // Zoom in
workspacePage.toolbox.zoomOut.click() // Zoom out

// Canvas
workspacePage.canvas.should('be.visible') // Main React Flow canvas

// Nodes
workspacePage.edgeNode.click() // Central Edge node
workspacePage.adapterNode('adapter-id').click() // Specific adapter
workspacePage.bridgeNode('bridge-id').click() // Specific bridge
workspacePage.deviceNode('adapter-id').click() // Device node
workspacePage.combinerNode('combiner-id').click() // Combiner node

// Toolbar (appears when node selected)
workspacePage.toolbar.title.should('have.text', 'Title')
workspacePage.toolbar.overview.click() // Overview tab
workspacePage.nodeToolbar.should('be.visible')

// Navigation
workspacePage.navLink.click() // Navigate to workspace
```

### Direct Selectors

When page objects don't exist, use these stable selectors:

```typescript
// Node types
cy.get('[data-nodetype="EDGE_NODE"]') // Central edge node
cy.get('[data-nodetype="ADAPTER_NODE"]') // Adapter nodes
cy.get('[data-nodetype="BRIDGE_NODE"]') // Bridge nodes
cy.get('[data-nodetype="DEVICE_NODE"]') // Device nodes
cy.get('[data-nodetype="CLUSTER_NODE"]') // Group nodes
cy.get('[data-nodetype="TOPIC_FILTER_NODE"]') // Topic filter nodes

// Specific node by ID
cy.get('[data-nodeid="adapter-id"]')

// Edges
cy.get('.react-flow__edge') // All edges
cy.get('.react-flow__edge.animated') // Animated edges only
cy.get('.react-flow__edge path') // Edge paths (for stroke checks)
cy.get('#connect-edge-adapter-id') // Specific edge

// React Flow canvas
cy.get('.react-flow__viewport') // Viewport container
cy.get('.react-flow__renderer') // Canvas renderer
```

---

## Entity Types & Relationships

### Workspace Entity Graph

```
                    ┌─────────────┐
                    │  Edge Node  │ (Central hub)
                    └──────┬──────┘
                           │
          ┌────────────────┼────────────────┬──────────────┐
          │                │                │              │
    ┌─────▼─────┐    ┌────▼────┐    ┌─────▼─────┐  ┌────▼────┐
    │  Adapter  │    │ Bridge  │    │   Topic   │  │  Pulse  │
    │   Node    │    │  Node   │    │  Filter   │  │  Agent  │
    └─────┬─────┘    └────┬────┘    └───────────┘  └────┬────┘
          │               │                              │
    ┌─────▼─────┐    ┌────▼────┐                   ┌────▼────┐
    │  Device   │    │ Remote  │                   │  Asset  │
    │   Node    │    │ Topics  │                   │ Mapper  │
    └───────────┘    └─────────┘                   └────┬────┘
                                                         │
                                                    ┌────▼────┐
                                                    │Combiner │
                                                    └─────────┘
```

### Entity Relationships

1. **Edge → Adapters**: Each adapter connects to the central Edge node
2. **Adapters → Devices**: Each adapter can have device nodes (representing physical devices)
3. **Adapters → Edge**: Data flows from adapter through Edge to MQTT broker
4. **Bridge → Edge**: Bridge connects Edge to remote MQTT broker
5. **Topic Filters → Edge**: Topic filters apply to Edge broker subscriptions
6. **Combiners → Adapters**: Combiners can aggregate data from multiple adapters

### Node Clustering/Grouping

Nodes can be automatically grouped if they're close together:

```typescript
// Check if nodes are grouped
cy.get('[data-nodetype="CLUSTER_NODE"]').should('exist')

// Conditional check (grouping depends on layout)
cy.get('body').then(($body) => {
  if ($body.find('[data-nodetype="CLUSTER_NODE"]').length > 0) {
    // Group exists, test group behavior
    cy.get('[data-nodetype="CLUSTER_NODE"]').should('have.attr', 'data-status')
  }
})
```

---

## Status System

### Dual Status Model

HiveMQ Edge uses a **dual-status system** for adapters and bridges:

#### 1. Runtime Status (Connection + Runtime State)

Determines the **color** of edges:

| Status       | Visual   | Connection                    | Runtime   | Meaning                     |
| ------------ | -------- | ----------------------------- | --------- | --------------------------- |
| **ERROR**    | 🔴 Red   | `ERROR`                       | Any       | Connection or runtime error |
| **ACTIVE**   | 🟢 Green | `CONNECTED`                   | `STARTED` | Connected and running       |
| **INACTIVE** | ⚪ Gray  | `DISCONNECTED` or `STATELESS` | `STOPPED` | Stopped or not connected    |

```typescript
// Status enum values
import { Status } from '@/api/__generated__'

// Connection status
Status.connection.CONNECTED // Connected to device/broker
Status.connection.DISCONNECTED // Not connected
Status.connection.ERROR // Connection error
Status.connection.STATELESS // No connection state (e.g., simulation)

// Runtime status
Status.runtime.STARTED // Adapter/bridge is running
Status.runtime.STOPPED // Adapter/bridge is stopped
```

#### 2. Operational Status (Data Flow)

Determines if edges are **animated**:

| Operational State   | Visual      | Condition                     | Meaning                         |
| ------------------- | ----------- | ----------------------------- | ------------------------------- |
| **Operational**     | ✨ Animated | Has mappings or subscriptions | Actively processing data        |
| **Non-operational** | Static      | No mappings/subscriptions     | Running but not processing data |

**For Adapters:**

- Operational = Has northbound or southbound mappings
- Non-operational = No mappings configured

**For Bridges:**

- Operational = Has local or remote subscriptions
- Non-operational = No subscriptions configured

### Testing Different Status Combinations

#### ERROR Adapter (Red, No Animation)

```typescript
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [
    {
      ...mockAdapter,
      status: {
        connection: Status.connection.ERROR,
        runtime: Status.runtime.STOPPED,
        message: 'Connection timeout: Unable to reach device',
      },
    },
  ],
})
```

#### ACTIVE Operational Adapter (Green, Animated)

```typescript
// Adapter with CONNECTED + STARTED status
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [
    {
      ...mockAdapter,
      status: {
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    },
  ],
})

// With mappings for operational status
cy.intercept('/api/v1/management/protocol-adapters/adapters/adapter-id/northboundMappings', {
  items: [{ tagName: 'sensor1', topic: 'data/topic' }],
})
```

#### ACTIVE Non-operational Adapter (Green, Not Animated)

```typescript
// Adapter with CONNECTED + STARTED status
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [
    {
      ...mockAdapter,
      status: {
        connection: Status.connection.CONNECTED,
        runtime: Status.runtime.STARTED,
      },
    },
  ],
})

// No mappings = not operational
cy.intercept('/api/v1/management/protocol-adapters/adapters/adapter-id/northboundMappings', {
  items: [], // Empty = no data flow
})
```

#### INACTIVE Adapter (Gray, No Animation)

```typescript
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [
    {
      ...mockAdapter,
      status: {
        connection: Status.connection.DISCONNECTED,
        runtime: Status.runtime.STOPPED,
      },
    },
  ],
})
```

### Status Propagation

Status propagates through the graph:

1. **Adapter ERROR** → Device nodes also show ERROR → Edges are red
2. **Group/Cluster** → Shows worst status of contained nodes
3. **Bridge ERROR** → Remote topic nodes may show ERROR

---

## Testing Patterns

### Basic Workspace Test Structure

```typescript
import { MOCK_PROTOCOL_OPC_UA, MOCK_ADAPTER_OPC_UA } from '@/__test-utils__/adapters'
import { mockBridge } from '@/api/hooks/useGetBridges/__handlers__'
import { MOCK_TOPIC_FILTER } from '@/api/hooks/useTopicFilters/__handlers__'
import { loginPage, workspacePage } from 'cypress/pages'
import { cy_interceptCoreE2E } from 'cypress/utils/intercept.utils.ts'

describe('Workspace Feature', () => {
  beforeEach(() => {
    // Core E2E intercepts (auth, config, etc.)
    cy_interceptCoreE2E()

    // Workspace-specific intercepts
    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA],
    }).as('getProtocols')

    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [MOCK_ADAPTER_OPC_UA],
    }).as('getAdapters')

    cy.intercept('/api/v1/management/bridges', {
      items: [mockBridge],
    }).as('getBridges')

    cy.intercept('/api/v1/management/topic-filters', {
      items: [MOCK_TOPIC_FILTER],
    }).as('getTopicFilters')

    // Navigate to workspace
    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  it('should display workspace correctly', () => {
    // Wait for data to load
    cy.wait('@getAdapters')
    cy.wait('@getBridges')

    // Fit canvas
    workspacePage.toolbox.fit.click()

    // Assertions
    cy.get('[data-nodetype="EDGE_NODE"]').should('exist')
    cy.get('[data-nodetype="ADAPTER_NODE"]').should('have.length', 1)
    cy.get('.react-flow__edge').should('exist')
  })
})
```

### Testing Edge Status Colors

```typescript
it('should display correct edge colors for different statuses', () => {
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [
      // ERROR adapter
      {
        ...mockAdapter_OPCUA,
        id: 'error-adapter',
        status: {
          connection: Status.connection.ERROR,
          runtime: Status.runtime.STOPPED,
        },
      },
      // ACTIVE adapter
      {
        ...mockAdapter_OPCUA,
        id: 'active-adapter',
        status: {
          connection: Status.connection.CONNECTED,
          runtime: Status.runtime.STARTED,
        },
      },
      // INACTIVE adapter
      {
        ...mockAdapter_OPCUA,
        id: 'inactive-adapter',
        status: {
          connection: Status.connection.DISCONNECTED,
          runtime: Status.runtime.STOPPED,
        },
      },
    ],
  }).as('getAdapters')

  cy.wait('@getAdapters')

  // Check edges exist
  cy.get('.react-flow__edge').should('have.length.greaterThan', 0)

  // Check edge stroke attributes (status-based styling)
  cy.get('.react-flow__edge path').should('have.attr', 'stroke')
})
```

### Testing Edge Animation (Operational Status)

```typescript
it('should animate edges for operational adapters', () => {
  // Adapter is ACTIVE
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [
      {
        ...mockAdapter_OPCUA,
        status: {
          connection: Status.connection.CONNECTED,
          runtime: Status.runtime.STARTED,
        },
      },
    ],
  }).as('getAdapters')

  // Adapter has mappings = operational
  cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
    items: [{ tagName: 'sensor1', topic: 'test/topic' }],
  }).as('getNorthboundMappings')

  cy.wait('@getAdapters')
  cy.wait('@getNorthboundMappings')

  // Edges should be animated
  cy.get('.react-flow__edge.animated').should('exist')
})
```

### Creating Complex Workspace Scenarios

```typescript
it('should handle complex workspace with multiple entities', () => {
  // Multiple adapters of different types
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [
      { ...MOCK_ADAPTER_OPC_UA, id: 'opcua-1' },
      { ...MOCK_ADAPTER_MODBUS, id: 'modbus-1' },
      { ...MOCK_ADAPTER_SIMULATION, id: 'sim-1' },
    ],
  }).as('getAdapters')

  // Multiple bridges
  cy.intercept('/api/v1/management/bridges', {
    items: [
      { ...mockBridge, id: 'bridge-1' },
      { ...mockBridge, id: 'bridge-2' },
    ],
  }).as('getBridges')

  // Multiple topic filters
  cy.intercept('/api/v1/management/topic-filters', {
    items: [
      { topicFilter: 'sensor/+/data', description: 'Sensor data' },
      { topicFilter: 'commands/#', description: 'All commands' },
    ],
  }).as('getTopicFilters')

  cy.wait('@getAdapters')
  cy.wait('@getBridges')
  cy.wait('@getTopicFilters')

  workspacePage.toolbox.fit.click()

  // Verify all entities rendered
  cy.get('[data-nodetype="ADAPTER_NODE"]').should('have.length', 3)
  cy.get('[data-nodetype="BRIDGE_NODE"]').should('have.length', 2)
  cy.get('[data-nodetype="TOPIC_FILTER_NODE"]').should('have.length', 2)
})
```

### Screenshot Tests for PR Documentation

```typescript
it('should capture workspace screenshot for PR', { tags: ['@percy'] }, () => {
  // Set up desired workspace state
  cy.intercept('/api/v1/management/protocol-adapters/adapters', {
    items: [
      /* ... */
    ],
  })

  // Wait for all data
  cy.wait('@getAdapters')

  // Fit and wait for layout
  workspacePage.toolbox.fit.click()
  cy.wait(1000)

  // Accessibility check
  cy.injectAxe()
  cy.checkAccessibility(undefined, {
    rules: {
      region: { enabled: false },
      'color-contrast': { enabled: false },
    },
  })

  // Capture REAL screenshot (PNG file)
  cy.screenshot('PR-Screenshot-Feature-Name', {
    overwrite: true,
    capture: 'viewport',
  })

  // Optional: Percy snapshot for visual regression
  cy.percySnapshot('Feature Description', {
    widths: [1280, 1920],
    minHeight: 800,
  })
})
```

---

## Common Pitfalls

### 1. ❌ Forgetting Per-Adapter Mappings Intercepts

Edges won't animate without mapping intercepts:

```typescript
// ❌ Wrong - No mappings intercepted
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [mockAdapter],
})

// ✅ Correct - Add mapping intercepts
cy.intercept('/api/v1/management/protocol-adapters/adapters', {
  items: [mockAdapter],
})
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/northboundMappings', {
  items: [{ tagName: 'tag1', topic: 'topic' }],
})
```

### 2. ❌ Using Invalid Cypress Chainables

```typescript
// ❌ Wrong - .or() doesn't exist
cy.get('[data-nodetype="CLUSTER_NODE"]').should('exist').or('not.exist')

// ✅ Correct - Use conditional check
cy.get('body').then(($body) => {
  if ($body.find('[data-nodetype="CLUSTER_NODE"]').length > 0) {
    cy.get('[data-nodetype="CLUSTER_NODE"]').should('exist')
  }
})
```

### 3. ❌ Not Waiting for Layout to Settle

React Flow needs time to compute layout:

```typescript
// ❌ Wrong - Screenshot before layout complete
workspacePage.toolbox.fit.click()
cy.screenshot('workspace')

// ✅ Correct - Wait for layout
workspacePage.toolbox.fit.click()
cy.wait(1000) // Allow layout to settle
cy.screenshot('workspace')
```

### 4. ❌ Testing Animation with Static Checks

```typescript
// ❌ Wrong - Checking static class
cy.get('.react-flow__edge').should('have.class', 'animated')

// ✅ Correct - Check animated edges exist
cy.get('.react-flow__edge.animated').should('exist')
```

### 5. ❌ Not Handling 404s for Optional Endpoints

```typescript
// ❌ Wrong - Test fails on 404
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings')

// ✅ Correct - Return empty or 404
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
  statusCode: 404,
  body: { title: 'Not Found' },
})

// Or return empty array
cy.intercept('/api/v1/management/protocol-adapters/adapters/**/southboundMappings', {
  items: [],
})
```

### 6. ❌ Assuming Fixed Node Positions

React Flow uses dynamic layout - don't rely on coordinates:

```typescript
// ❌ Wrong - Position-based assertions
cy.get('[data-nodetype="ADAPTER_NODE"]').should('have.css', 'top', '100px')

// ✅ Correct - Test existence and relationships
cy.get('[data-nodetype="ADAPTER_NODE"]').should('exist')
cy.get('.react-flow__edge[source="edge"][target="adapter-id"]').should('exist')
```

### 7. ❌ Not Using Proper Selectors

```typescript
// ❌ Wrong - Fragile selectors
cy.get('.css-abc123')
cy.contains('Adapter')

// ✅ Correct - Data attributes
cy.get('[data-nodetype="ADAPTER_NODE"]')
cy.get('[data-nodeid="adapter-id"]')
cy.getByTestId('workspace-toolbar')
```

---

## Quick Reference: Status Combinations

| Connection     | Runtime   | Mappings | Edge Color | Animated | Status Name            |
| -------------- | --------- | -------- | ---------- | -------- | ---------------------- |
| `ERROR`        | `STOPPED` | N/A      | 🔴 Red     | ❌ No    | ERROR                  |
| `CONNECTED`    | `STARTED` | ✅ Yes   | 🟢 Green   | ✅ Yes   | ACTIVE Operational     |
| `CONNECTED`    | `STARTED` | ❌ No    | 🟢 Green   | ❌ No    | ACTIVE Non-operational |
| `DISCONNECTED` | `STOPPED` | N/A      | ⚪ Gray    | ❌ No    | INACTIVE               |
| `STATELESS`    | `STARTED` | N/A      | 🟢 Green   | Depends  | ACTIVE (Simulation)    |

---

## Example: Complete Status Test

```typescript
describe('Workspace Status Visualization', () => {
  beforeEach(() => {
    cy_interceptCoreE2E()

    cy.intercept('/api/v1/management/protocol-adapters/types', {
      items: [MOCK_PROTOCOL_OPC_UA, MOCK_PROTOCOL_MODBUS, MOCK_PROTOCOL_S7],
    }).as('getProtocols')

    loginPage.visit('/app/workspace')
    loginPage.loginButton.click()
    workspacePage.navLink.click()
  })

  it('should show all status types correctly', () => {
    // Mock adapters with all status types
    cy.intercept('/api/v1/management/protocol-adapters/adapters', {
      items: [
        // ERROR
        {
          id: 'error-adapter',
          type: 'opcua',
          status: {
            connection: Status.connection.ERROR,
            runtime: Status.runtime.STOPPED,
            message: 'Connection failed',
          },
        },
        // ACTIVE Operational
        {
          id: 'active-operational',
          type: 'modbus',
          status: {
            connection: Status.connection.CONNECTED,
            runtime: Status.runtime.STARTED,
          },
        },
        // ACTIVE Non-operational
        {
          id: 'active-idle',
          type: 's7',
          status: {
            connection: Status.connection.CONNECTED,
            runtime: Status.runtime.STARTED,
          },
        },
        // INACTIVE
        {
          id: 'inactive-adapter',
          type: 'http',
          status: {
            connection: Status.connection.DISCONNECTED,
            runtime: Status.runtime.STOPPED,
          },
        },
      ],
    }).as('getAdapters')

    // Mappings for operational adapter only
    cy.intercept('/api/v1/management/protocol-adapters/adapters/error-adapter/northboundMappings', {
      items: [],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/active-operational/northboundMappings', {
      items: [{ tagName: 'sensor', topic: 'data/topic' }],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/active-idle/northboundMappings', {
      items: [],
    })
    cy.intercept('/api/v1/management/protocol-adapters/adapters/inactive-adapter/northboundMappings', {
      items: [],
    })

    cy.wait('@getAdapters')
    workspacePage.toolbox.fit.click()

    // Verify nodes exist
    cy.get('[data-nodetype="ADAPTER_NODE"]').should('have.length', 4)

    // Verify edges exist and have styles
    cy.get('.react-flow__edge').should('have.length.greaterThan', 0)
    cy.get('.react-flow__edge path').should('have.attr', 'stroke')

    // Verify animation exists for operational adapter
    cy.get('.react-flow__edge.animated').should('exist')
  })
})
```

---

## Related Documentation

- [CYPRESS_BEST_PRACTICES.md](.tasks/CYPRESS_BEST_PRACTICES.md) - General Cypress testing guidelines
- [TESTING_GUIDELINES.md](.tasks/TESTING_GUIDELINES.md) - Overall testing strategy
- [PULL_REQUEST_SCREENSHOTS_GUIDE.md](.tasks/PULL_REQUEST_SCREENSHOTS_GUIDE.md) - PR screenshot guidelines
- [REACT_FLOW_BEST_PRACTICES.md](.tasks/REACT_FLOW_BEST_PRACTICES.md) - React Flow specific patterns

---

## Summary

### Key Takeaways

1. **Mock Data is Centralized** - Use `__handlers__` and `__test-utils__` for consistent mocks
2. **Dual Status System** - Runtime status (color) + Operational status (animation)
3. **Intercepts Matter** - Missing mapping intercepts = no animation
4. **Use Data Attributes** - `[data-nodetype]` and `[data-nodeid]` for stable selectors
5. **Wait for Layout** - React Flow needs time to settle before screenshots
6. **Status Propagates** - Errors bubble up through device nodes and groups
7. **cy.screenshot() for PRs** - Use real screenshot command, not just Percy
8. **Accessibility Always** - Check a11y before capturing screenshots

### Quick Start Checklist

- [ ] Import mocks from `@/__test-utils__/adapters` and handlers
- [ ] Add core intercepts (adapters, bridges, topic-filters)
- [ ] Add per-adapter mapping intercepts for animation
- [ ] Use `cy_interceptCoreE2E()` for auth/config
- [ ] Navigate via `loginPage` and `workspacePage`
- [ ] Call `workspacePage.toolbox.fit.click()` to fit canvas
- [ ] Wait for layout with `cy.wait(1000)` before screenshots
- [ ] Check accessibility with `cy.injectAxe()` and `cy.checkAccessibility()`
- [ ] Use `cy.screenshot()` for actual PNG files
- [ ] Tag Percy tests with `{ tags: ['@percy'] }`

---

**Last Updated:** October 25, 2025  
**Maintainer:** Development Team  
**Questions?** See related documentation or ask in team chat
