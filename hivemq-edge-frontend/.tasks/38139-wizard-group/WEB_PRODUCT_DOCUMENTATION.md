# Workspace Operation Wizard

**Product:** HiveMQ Edge  
**Feature Version:** 2024.11  
**Last Updated:** November 21, 2025

> **Note for Developers**: This document includes screenshot placeholders marked with `![...](./screenshots/...)`. Each placeholder includes a descriptive caption explaining what should be captured. Screenshots should show the actual HiveMQ Edge interface in use, demonstrating the workflow described in the surrounding text.

---

## Overview

The Workspace Operation Wizard is an interactive creation system that allows you to create and configure MQTT infrastructure entities directly within the workspace canvas. Instead of navigating between different screens or using inconsistent creation patterns, the wizard provides a unified, guided workflow for all entity types.

### Key Benefits

- **Visual Feedback**: See exactly where entities will appear before committing changes
- **In-Context Creation**: Configure resources without leaving your workspace view
- **Interactive Selection**: Select data sources directly on the canvas with real-time validation
- **Consistent Experience**: Same workflow pattern for all entity types
- **Reduced Errors**: Built-in validation and visual previews prevent configuration mistakes

### Supported Operations

The wizard currently supports creating the following entity types:

| Entity Type           | Description                                                   | Creation Pattern                               |
| --------------------- | ------------------------------------------------------------- | ---------------------------------------------- |
| **Protocol Adapters** | Connect to external data sources (OPC-UA, HTTP, Modbus, etc.) | Direct configuration with ghost preview        |
| **MQTT Bridges**      | Connect to remote MQTT brokers                                | Direct configuration with ghost preview        |
| **Data Combiners**    | Merge multiple data sources into unified topics               | Interactive source selection                   |
| **Asset Mappers**     | Transform and enrich industrial data with ontology            | Interactive source selection with Pulse Agent  |
| **Groups**            | Organize related nodes into visual containers                 | Interactive node selection with ghost boundary |

---

## Getting Started

### Prerequisites

- HiveMQ Edge instance running (version 2024.11 or later)
- Access to the workspace canvas
- Appropriate permissions to create entities

### Accessing the Wizard

1. Navigate to the **Workspace** section in HiveMQ Edge
2. Locate the **"Create New"** button in the workspace toolbar (positioned near search and filter controls)
3. Click the button to open the dropdown menu showing all available entity types

The wizard button is organized into two sections:

- **Entities**: Core infrastructure components (Adapters, Bridges, Combiners, Asset Mappers, Groups)
- **Integration Points**: Configuration attachments for existing entities (coming in future releases)

---

## Wizard Workflow

### Understanding the Four Components

Every wizard operation involves four coordinated components:

#### 1. Trigger Button

The **"Create New"** dropdown button in the workspace toolbar serves as the entry point. When clicked, it displays a categorized list of entity types you can create.

![Create New button dropdown](./screenshots/trigger-button-dropdown.png)  
_**Screenshot**: "Create New" button in workspace toolbar with open dropdown menu showing Entities (Adapters, Bridges, Combiners, Asset Mappers) and Integration Points sections with icons_

#### 2. Progress Bar

A bottom-center panel that appears when a wizard is active, showing your current step, a description of what to do, and navigation buttons.

![Progress bar during wizard](./screenshots/progress-bar-active.png)  
_**Screenshot**: Progress bar at bottom-center of workspace showing "Step 1 of 3: Select protocol type" with Back, Next, and Cancel buttons_

#### 3. Ghost Nodes

Transparent preview nodes that show where your new entity will appear in the workspace topology before you commit to creating it. These nodes are visually distinct from real nodes (semi-transparent with dashed borders) and cannot be moved or edited.

**Behavior**:

- Appear immediately after selecting entity type
- Persist throughout configuration steps
- Transform into real nodes upon successful creation
- Removed if wizard is cancelled

![Ghost nodes preview](./screenshots/ghost-nodes-preview.png)  
_**Screenshot**: Ghost nodes showing adapter topology (Device → Adapter → Edge Broker) with dashed borders and semi-transparent styling_

#### 4. Configuration Panel

A side drawer containing the configuration form for your entity. The workspace remains visible behind the panel so you can see the context of what you're creating.

![Configuration panel with form](./screenshots/configuration-panel.png)  
_**Screenshot**: Side drawer showing adapter configuration form with workspace visible in background_

---

## Creating Entities

### Creating a Protocol Adapter

Protocol Adapters connect HiveMQ Edge to external data sources using industry-standard protocols.

**Wizard Steps**: 3 steps

#### Step 0: Preview

**What Happens**:

- Ghost nodes appear showing the future topology: `DEVICE → ADAPTER → EDGE BROKER`
- Workspace locks to prevent accidental changes
- Progress bar shows "Review topology preview"

**Action Required**: Click "Next" when ready to proceed

#### Step 1: Select Protocol Type

The configuration drawer opens showing available protocol adapters as cards with their names, descriptions, and icons.

![Protocol selector](./screenshots/protocol-selector.png)  
_**Screenshot**: Protocol browser showing cards for OPC-UA, Modbus TCP, HTTP, and other adapters with search functionality_

**Action Required**:

1. Browse or search for your desired protocol (e.g., "OPC-UA", "Modbus TCP", "HTTP")
2. Click on the protocol card to select it
3. Click "Next" to proceed

**Tip**: If you don't see your protocol, ensure the corresponding adapter module is installed.

#### Step 2: Configure Adapter

**What Happens**:

- Drawer displays the protocol-specific configuration form
- Form fields include connection details, sampling intervals, tags, etc.
- Validation runs as you type
- Ghost nodes remain visible for context

**Action Required**:

1. Fill in all required fields (marked with asterisks)
2. Configure protocol-specific settings (host, port, credentials, etc.)
3. Define data points or tags to collect
4. Click "Complete" when form is valid

#### Completion

When you click "Complete", the wizard creates your adapter in HiveMQ Edge. Ghost nodes fade out and are replaced by real, active nodes with a brief highlight animation. A success message confirms the adapter is created.

![Wizard completion with new nodes](./screenshots/wizard-completion.png)  
_**Screenshot**: Workspace showing newly created adapter nodes (Device → Adapter → Edge Broker) with green highlight effect and success toast notification_

**Result**: Your adapter is now active and visible in the workspace, ready to collect data.

---

### Creating an MQTT Bridge

MQTT Bridges connect your HiveMQ Edge instance to remote MQTT brokers, enabling data flow between different MQTT networks.

**Wizard Steps**: 2 steps

#### Step 0: Preview

**What Happens**:

- Ghost nodes show: `REMOTE BROKER → BRIDGE → EDGE BROKER`
- Workspace locks

**Action Required**: Click "Next"

#### Step 1: Configure Bridge

**What Happens**:

- Configuration drawer opens with bridge settings form
- Fields for remote broker connection (host, port, protocol)
- Subscription and publication topic mappings
- Authentication settings (username/password, TLS certificates)

**Action Required**:

1. Enter remote broker connection details
2. Configure topic filters for bidirectional data flow
3. Set up authentication if required
4. Click "Complete"

#### Completion

Same animation and feedback as adapter creation. Your bridge is now routing messages between brokers.

---

### Creating a Data Combiner

Data Combiners merge multiple data sources into unified MQTT topics, allowing you to aggregate data streams.

**Wizard Steps**: 3 steps  
**Special Feature**: Interactive source selection on canvas

#### Step 0: Select Data Sources

The workspace enters selection mode where you can click nodes to choose data sources. Selectable nodes show blue highlights, while incompatible nodes are dimmed. A ghost combiner appears showing where your new combiner will be created, with a floating panel tracking your selections.

![Interactive selection mode](./screenshots/combiner-selection.png)  
_**Screenshot**: Workspace in selection mode showing highlighted adapter nodes, ghost combiner with dashed edges, and selection panel in bottom-left corner displaying "2 nodes selected"_

**Action Required**:

1. Click on adapter or bridge nodes to select them as data sources
2. Minimum 2 sources required, no maximum limit
3. Watch ghost edges appear connecting selected nodes to the combiner ghost
4. Selection panel shows current count and validation status
5. Click "Next" in the selection panel when minimum sources selected

**Visual Feedback**:

- Selected nodes: Blue border + checkmark badge
- Ghost edges: Dashed lines from sources → combiner
- Selection panel: Live count display (e.g., "2 nodes selected")
- Validation: Next button disabled until minimum sources met

**Tips**:

- Click a node again to deselect it
- Ghost combiner is clickable to highlight all connected edges
- Press `Escape` to cancel the entire wizard

#### Step 1: Configure Combiner

**What Happens**:

- Configuration drawer opens
- Form pre-populates with selected source nodes
- Define output topic structure and data transformation rules

**Action Required**:

1. Review selected sources (can return to previous step if needed)
2. Name your combiner
3. Configure output topic pattern
4. Define any data transformations or filtering
5. Click "Complete"

#### Completion

Ghost combiner and edges transform into real nodes. The combiner immediately begins merging data from selected sources.

---

### Creating an Asset Mapper

Asset Mappers transform industrial data using semantic ontologies, applying asset models to enrich raw telemetry with business context.

**Wizard Steps**: 3 steps  
**Special Feature**: Interactive source selection with Pulse Agent requirement

#### Step 0: Select Data Sources and Pulse Agent

**What Happens**:

- Selection mode activates (similar to Combiner)
- **Mandatory requirement**: Must select Pulse Agent node
- Optional: Select additional data sources (adapters, bridges)
- Ghost asset mapper appears connected to selected sources and Pulse Agent
- Selection panel shows validation: "Pulse Agent required"

**Action Required**:

1. Click on the Pulse Agent node (mandatory - wizard won't proceed without it)
2. Optionally select additional data sources
3. Selection panel updates: "1 node selected (Pulse Agent ✓)"
4. Click "Next" when ready

**Key Difference from Combiner**:

- Pulse Agent is always required (validation enforces this)
- Selection panel highlights Pulse Agent status separately
- Ghost connections show data flow through Asset Mapper to Edge Broker

#### Step 1: Configure Asset Mapper

**What Happens**:

- Configuration drawer displays Asset Mapper form
- Pulse Agent integration pre-configured
- Define asset ontology mappings and transformations

**Action Required**:

1. Name your asset mapper
2. Configure asset model and ontology mappings
3. Define transformation rules
4. Set up data enrichment logic
5. Click "Complete"

#### Completion

Asset Mapper becomes active, applying semantic transformations to industrial data flowing from selected sources.

---

### Creating a Group

Groups are visual containers that organize related nodes in your workspace, helping you manage large MQTT topologies by creating logical hierarchies that reflect your real-world infrastructure.

**Wizard Steps**: 2 steps  
**Special Feature**: Interactive node selection with ghost preview boundary

#### Step 0: Select Nodes for Group

The workspace enters selection mode where you can click nodes directly on the canvas to add them to your group. A **ghost group** appears as a semi-transparent container with a dashed border, showing the exact boundary of your future group.

![Interactive group selection with ghost preview](./screenshots/group-selection-mode.png)  
_**Screenshot**: Workspace in selection mode showing 2 selected adapter nodes with blue highlights, ghost group boundary (dashed border) surrounding them, and selection panel displaying "2 nodes selected (min: 2)"_

**What Happens**:

- Workspace enters selection mode (similar to Combiner/Asset Mapper)
- Ghost group boundary appears immediately after first node selection
- Ghost group expands/shrinks as you select/deselect nodes
- Selection panel tracks node count and validates constraints
- **Minimum requirement**: 2 nodes (enforced by validation)
- **Maximum nesting depth**: 3 levels (groups within groups within groups)

**Action Required**:

1. Click on adapter, bridge, or other group nodes to select them
2. Minimum 2 nodes required - selection panel shows "2 nodes selected (min: 2)"
3. Watch ghost group boundary update in real-time as you select nodes
4. Ghost edges show data connections that will be preserved
5. Click "Next" in the selection panel when minimum nodes selected

**Visual Feedback**:

- **Selected nodes**: Blue border + checkmark badge overlay
- **Ghost group**: Semi-transparent container with dashed border surrounding selected nodes
- **Auto-included nodes**: Device/Host nodes automatically included when parent adapter selected
- **Restricted nodes**: Nodes already in groups appear dimmed and cannot be selected
- **Selection panel**: Live count display with validation status
- **Next button**: Disabled until minimum 2 nodes selected

**Selection Rules**:

- Can select adapters, bridges, and other groups
- Device and Host nodes auto-included with their parent adapters (no manual selection needed)
- Cannot select nodes already in a group (appear dimmed)
- Cannot create groups that would exceed 3-level nesting depth
- Click a node again to deselect it (ghost group shrinks accordingly)

**Tips**:

- Pan and zoom to see your entire topology while selecting
- Ghost group shows exact positioning before committing
- Press `Escape` to cancel and return to normal workspace mode
- Selection state preserved if you navigate back from configuration step

#### Step 1: Configure Group

**What Happens**:

- Configuration drawer slides in from the right
- Workspace and ghost group remain visible in background for context
- Form displays group metadata fields with tabs for future expansion
- Selected nodes list shown (readonly at this step)

![Group configuration drawer](./screenshots/group-configuration.png)  
_**Screenshot**: Configuration drawer on right showing group form with "Title" field, color scheme selector, and tabs (Config, Events, Metrics). Workspace with ghost group visible in background._

**Action Required**:

1. **Enter group title** (required) - Descriptive name like "Production Servers" or "Building A Floor 3"
2. **Select color scheme** (optional) - Choose a visual theme to distinguish this group
3. Review selected nodes in the content preview section (read-only)
4. Click "Back" to return to selection if you need to adjust node membership
5. Click "Submit" when ready to create the group

**Form Fields**:

| Field          | Type     | Required | Description                                            |
| -------------- | -------- | -------- | ------------------------------------------------------ |
| Title          | Text     | Yes      | Display name for the group (e.g., "Production Line A") |
| Color Scheme   | Dropdown | No       | Visual theme (blue, red, green, purple, orange, gray)  |
| Selected Nodes | Preview  | Readonly | List of nodes that will be grouped                     |

**Form Tabs**:

- **Config**: Metadata and visual properties (active during creation)
- **Events**: Event log preview (placeholder for future functionality)
- **Metrics**: Metrics visualization (placeholder for future functionality)

**Validation**:

- Title field required - form cannot submit with empty title
- Title must be unique across workspace groups
- Real-time validation feedback as you type
- Submit button disabled until all validation passes

**Back Button Behavior**:

- Click "Back" in drawer footer to return to Step 0 (selection mode)
- All selected nodes preserved when navigating back
- Modify selection as needed, then return to configuration
- Configuration form state preserved (title, color scheme inputs retained)

#### Completion

When you click "Submit", the wizard creates your group in the workspace. The ghost group transforms into a real group node with a smooth transition animation.

![Group creation success](./screenshots/group-completion.png)  
_**Screenshot**: Workspace showing newly created real group node (solid border) containing the selected adapters, with green highlight animation and success toast notification "Group created successfully"_

**What Happens**:

1. **Ghost transformation**: Ghost group fades out, real group node fades in with highlight animation
2. **Topology update**: Selected nodes now visually contained within group boundary
3. **Edge rerouting**: Data flow edges automatically rerouted to show connections through group
4. **Success notification**: Green toast message confirms "Group created successfully"
5. **Workspace unlock**: Returns to normal editing mode

**Result**:

- Your group is now a first-class workspace entity
- Selected nodes are children of the group (hierarchical relationship)
- Group can be collapsed to hide children and save visual space
- Group can be expanded to show children and edit contents
- Group appears in workspace hierarchy and can be renamed/deleted later
- Data flow through grouped nodes continues uninterrupted

**Post-Creation Actions**:

- **Collapse group**: Click collapse icon to hide child nodes (reduces visual clutter)
- **Expand group**: Click expand icon to show child nodes and edit contents
- **Edit group**: Right-click → Edit to change title or color scheme
- **Delete group**: Removes grouping but preserves child nodes
- **Nest further**: Create parent groups to build deeper hierarchies (max 3 levels)

#### Advanced: Nested Groups

Groups can contain other groups, allowing you to build complex hierarchies. For example:

```
North America (Group)
├── California (Group)
│   ├── San Jose Data Center (Group)
│   │   ├── Adapter 1
│   │   ├── Adapter 2
│   │   └── Bridge A
│   └── Adapter 3
└── Adapter 4
```

**Nesting Rules**:

- Maximum depth: 3 levels (group → group → group)
- Attempting to create a 4th level triggers validation error
- Selection panel shows "Maximum nesting depth reached" warning
- Cannot select groups that would exceed depth limit (appear dimmed)

**Visual Hierarchy**:

- Nested groups show indentation in workspace hierarchy panel
- Parent groups display child count badge (e.g., "3 nodes")
- Collapsing parent also collapses all children
- Expanding parent shows immediate children only (expand children separately)

#### Use Cases

**By Infrastructure Type**:

- **Geographic**: "North America" → "California" → "San Jose Data Center"
- **Facility**: "Factory Floor" → "Production Line A" → "Assembly Station 1"
- **Environment**: "Production" vs "Staging" vs "Development"
- **Business Unit**: "Manufacturing" → "Quality Control" → "Inspection Sensors"

**By Data Flow**:

- Group all sensors feeding a specific data pipeline
- Group edge gateways by network segment
- Group adapters by protocol type (all OPC-UA in one group)

**By Lifecycle**:

- "Active Production" vs "Decommissioning" vs "Maintenance"
- Temporary groups for migration or testing projects
- Seasonal or time-based groupings

**Operational Benefits**:

1. **Navigation**: Collapse irrelevant groups to focus on specific subsystems
2. **Clarity**: New team members understand topology structure instantly
3. **Operations**: Bulk actions on groups (e.g., pause all adapters in "Maintenance" group)
4. **Documentation**: Topology visually documents organizational structure

---

## Understanding the Wizard Behavior

### Ghost Nodes and Preview

The wizard shows you a preview of what will be created using ghost nodes - semi-transparent nodes with dashed borders that indicate future entities. For adapters and bridges, you'll see the complete data flow including the device or remote broker connections.

The workspace automatically positions ghost nodes logically within your topology and pans to show them clearly. Ghost nodes transform into real, functional nodes when you complete the wizard, or disappear if you cancel.

### Interactive Selection (Combiners & Asset Mappers)

When creating combiners or asset mappers, you select existing nodes as data sources directly on the workspace. The wizard highlights which nodes you can select and dims others. As you click nodes, you'll see:

- Blue highlights on selected nodes
- Dashed connection lines to the ghost combiner/mapper
- A selection panel showing your current count and validation status

You must select at least 2 nodes for combiners. Asset mappers require the Pulse Agent plus any additional data sources.

### Workspace During Wizard

While a wizard is active, the workspace locks to prevent accidental changes:

- **Locked**: Dragging nodes, selecting nodes (except during selection steps), making other edits
- **Available**: Panning, zooming, viewing the minimap to navigate

The "Create New" button becomes disabled until you complete or cancel the current wizard. You can always cancel by clicking the Cancel button in the progress bar or pressing `Escape`.

---

## Accessibility

The wizard is fully accessible via keyboard and screen readers, meeting WCAG AA standards.

### Keyboard Shortcuts

| Shortcut            | Action                                   |
| ------------------- | ---------------------------------------- |
| `Escape`            | Cancel the wizard at any step            |
| `Tab` / `Shift+Tab` | Navigate through buttons and form fields |
| `Enter` or `Space`  | Activate buttons and select nodes        |
| Arrow keys          | Navigate dropdowns and radio buttons     |

### Screen Reader Support

The wizard announces all progress changes, validation status, and error messages to screen readers. All interactive elements have clear labels, and the step-by-step flow is clearly communicated.

### Visual Features

Ghost nodes use both color and dashed borders for distinction (not color alone). All text meets contrast standards, and animations respect your system's motion preferences.

---

## When Things Go Wrong

### Validation Messages

The wizard validates your input as you type and shows clear error messages when something needs correction:

- Required fields are marked with asterisks
- Invalid values show error messages below the field
- The "Complete" button stays disabled until all errors are fixed

Common validation messages:

- "Host URL is required"
- "Port must be between 1 and 65535"
- "At least 2 data sources must be selected"
- "Pulse Agent is required for Asset Mapper"

### Connection and Server Errors

If the wizard can't connect to HiveMQ Edge or encounters a server error, you'll see a clear message explaining what went wrong:

- "Unable to connect to HiveMQ Edge" - Check your network connection
- "An adapter with this ID already exists" - Choose a different ID
- "Authentication failed" - Verify your credentials
- "Cannot connect to host" - Check the host address and firewall settings

Your configuration is saved even if an error occurs, so you can correct the problem and try again without starting over.

---

## Best Practices

### When to Use the Wizard

**✅ Use the wizard when**:

- Creating new entities directly in workspace context
- You want to visualize topology changes before committing
- You need to select data sources from existing workspace nodes
- You prefer guided step-by-step creation

**⚠️ Consider standalone creation when**:

- Batch creating multiple entities (API or bulk import more efficient)
- Importing configurations from JSON files
- Automating entity creation via CI/CD pipelines
- Workspace not yet initialized or entity types not visually represented

### Workflow Tips

**Before Starting a Wizard**:

1. **Review your topology**: Understand where the new entity will fit
2. **Prepare configuration data**: Have connection details, credentials ready
3. **Check prerequisites**: Ensure required nodes exist (e.g., Pulse Agent for Asset Mapper)

**During the Wizard**:

1. **Use ghost preview**: Verify positioning and connections before configuring
2. **Click "Back" freely**: Navigate backward without fear of losing data
3. **Leverage search**: Use search in protocol selector for faster selection
4. **Check validation**: Watch for inline errors as you type

**After Creation**:

1. **Verify status**: Check that new node shows healthy status
2. **Test data flow**: Confirm data flowing through new entity
3. **Save workspace**: Consider saving a workspace snapshot after major changes

### Troubleshooting Common Issues

#### Ghost Nodes Not Appearing

**Symptoms**: Wizard progress bar appears but no ghost nodes visible

**Possible Causes**:

- Canvas zoomed too far in/out
- Ghost nodes positioned outside current viewport
- Browser rendering issue

**Solutions**:

1. Click minimap to see full workspace view
2. Cancel wizard and restart (sometimes fixes rendering)
3. Zoom to fit all nodes (use toolbar button)
4. Try different browser if issue persists

#### Configuration Form Not Loading

**Symptoms**: Drawer opens but form is blank or spinning

**Possible Causes**:

- API endpoint unreachable (protocol schemas not loaded)
- JavaScript error in form renderer
- Network latency

**Solutions**:

1. Check browser console for errors
2. Verify HiveMQ Edge API is accessible
3. Refresh page and restart wizard
4. Check network connectivity to HiveMQ Edge instance

#### Selection Not Working

**Symptoms**: Cannot click nodes during selection step

**Possible Causes**:

- Wrong step (selection only in Step 0 for Combiner/Asset Mapper)
- Nodes of wrong type (not adapters/bridges)
- Browser focus outside canvas

**Solutions**:

1. Verify you're on selection step (progress bar should indicate)
2. Look for blue highlight on valid nodes (if none visible, no valid targets exist)
3. Click on canvas area to ensure focus
4. Check that selectable nodes exist in workspace

#### Wizard Stuck After Clicking "Complete"

**Symptoms**: Spinner shows indefinitely after submitting

**Possible Causes**:

- API request timeout
- Backend processing error
- Network disconnection

**Solutions**:

1. Wait up to 30 seconds (timeout period)
2. Check browser network tab for failed requests
3. Review HiveMQ Edge logs for backend errors
4. Cancel wizard, verify backend health, and retry

---

## Reference

### Wizard Metadata

Complete reference for all wizard types and their step configurations.

#### Protocol Adapter Wizard

```
Type: EntityType.ADAPTER
Steps: 3
- Step 0: Preview topology (ghost nodes: Device, Adapter, Edge Broker)
- Step 1: Select protocol type (ProtocolsBrowser)
- Step 2: Configure adapter (ChakraRJSForm with protocol-specific schema)

Ghost Topology:
  DEVICE → ADAPTER → EDGE_BROKER

Configuration Data Stored:
  - protocolType: string (Step 1)
  - config: object (Step 2)
```

#### MQTT Bridge Wizard

```
Type: EntityType.BRIDGE
Steps: 2
- Step 0: Preview topology (ghost nodes: Remote Broker, Bridge, Edge Broker)
- Step 1: Configure bridge (Bridge configuration form)

Ghost Topology:
  REMOTE_BROKER ⇄ BRIDGE ⇄ EDGE_BROKER

Configuration Data Stored:
  - config: object (Step 1)
```

#### Data Combiner Wizard

```
Type: EntityType.COMBINER
Steps: 3
- Step 0: Select data sources (Interactive selection on canvas)
- Step 1: Configure combiner (Combiner configuration form)
- Step 2: (Reserved for future mapping configuration)

Ghost Topology:
  [Selected Sources] → COMBINER → EDGE_BROKER

Selection Constraints:
  - minNodes: 2
  - maxNodes: Infinity
  - allowedNodeTypes: [ADAPTER_NODE, BRIDGE_NODE]

Configuration Data Stored:
  - selectedNodeIds: string[] (Step 0)
  - config: object (Step 1)
```

#### Asset Mapper Wizard

```
Type: EntityType.ASSET_MAPPER
Steps: 3
- Step 0: Select data sources + Pulse Agent (Interactive selection)
- Step 1: Configure asset mapper (Asset Mapper configuration form)
- Step 2: (Reserved for ontology mapping configuration)

Ghost Topology:
  [Selected Sources] + PULSE_AGENT → ASSET_MAPPER → EDGE_BROKER

Selection Constraints:
  - minNodes: 1
  - maxNodes: Infinity
  - allowedNodeTypes: [ADAPTER_NODE, BRIDGE_NODE, PULSE_NODE]
  - requiredNodeTypes: [PULSE_NODE]

Configuration Data Stored:
  - selectedNodeIds: string[] (Step 0)
  - config: object (Step 1)
```

### API Endpoints

Wizard operations interact with the following HiveMQ Edge REST API endpoints:

```
POST /api/v1/management/protocol-adapters/adapters
  - Create new protocol adapter
  - Body: AdapterConfiguration
  - Returns: 201 Created + adapter instance

POST /api/v1/management/bridges
  - Create new MQTT bridge
  - Body: BridgeConfiguration
  - Returns: 201 Created + bridge instance

POST /api/v1/data-hub/data-policies/combiners
  - Create new data combiner
  - Body: CombinerConfiguration
  - Returns: 201 Created + combiner instance

POST /api/v1/pulse/asset-mappers
  - Create new asset mapper
  - Body: AssetMapperConfiguration
  - Returns: 201 Created + asset mapper instance
```

---

## Support & Feedback

### Getting Help

If you encounter issues with the Workspace Operation Wizard:

1. **Check this documentation** for troubleshooting tips
2. **Review browser console** for JavaScript errors
3. **Check HiveMQ Edge logs** for backend issues
4. **Visit HiveMQ Community Forums** for community support
5. **Contact HiveMQ Support** for enterprise customers

### Reporting Issues

When reporting wizard-related issues, please include:

- HiveMQ Edge version
- Browser type and version
- Steps to reproduce the issue
- Expected vs. actual behavior
- Screenshots or screen recording (if applicable)
- Browser console errors (if any)
- Relevant HiveMQ Edge log excerpts

### Feature Requests

We actively collect feedback to improve the wizard experience. If you have suggestions for:

- New wizard types
- Improved workflows
- Better visual feedback
- Enhanced error messages
- Performance optimizations

Please submit your ideas through:

- HiveMQ Community Forums
- GitHub Issues (if using open-source version)
- Direct feedback to HiveMQ product team

---

## Appendix

### Glossary

**Adapter**: A protocol adapter that connects HiveMQ Edge to external data sources using industry protocols (OPC-UA, Modbus, HTTP, etc.)

**Asset Mapper**: A component that applies semantic ontology transformations to industrial data, enriching raw telemetry with business context

**Bridge**: An MQTT bridge that connects HiveMQ Edge to remote MQTT brokers for inter-broker message routing

**Combiner**: A data transformation component that merges multiple data sources into unified MQTT topics

**Edge Broker**: The central MQTT broker in HiveMQ Edge that routes messages between adapters, bridges, and clients

**Entity**: A first-class infrastructure component in the workspace (Adapter, Bridge, Combiner, Asset Mapper, Device, Group)

**Ghost Node**: A semi-transparent preview node shown during wizard operation, indicating where a real node will be created

**Group**: A visual container that organizes related nodes into hierarchical structures (up to 3 levels deep). Groups help manage large topologies by creating logical boundaries that reflect real-world infrastructure organization—such as geographic locations, production lines, or operational environments

**Integration Point**: Configuration attachments that extend entity behavior (Tags, Topic Filters, Mappings)

**Pulse Agent**: HiveMQ's industrial IoT agent required for Asset Mapper functionality, providing semantic data transformation

**Wizard**: A multi-step guided workflow for creating entities or configuring integration points within the workspace

**Workspace**: The visual canvas representing your MQTT infrastructure topology with nodes and connections

### Related Documentation

- [HiveMQ Edge User Guide](https://docs.hivemq.com/hivemq-edge/index.html)
- [Protocol Adapters Reference](https://docs.hivemq.com/hivemq-edge/protocol-adapters.html)
- [MQTT Bridges Configuration](https://docs.hivemq.com/hivemq-edge/bridges.html)
- [HiveMQ Pulse Documentation](https://docs.hivemq.com/hivemq-pulse/index.html)
- [DataHub Designer (workspace architecture)](../DATAHUB_ARCHITECTURE.md)

### Version History

| Version | Date              | Changes                                                        |
| ------- | ----------------- | -------------------------------------------------------------- |
| 1.0.0   | November 10, 2025 | Initial release with Phase 1 entity wizards                    |
| 1.1.0   | November 21, 2025 | Documentation created, interactive selection system documented |

---

**© 2025 HiveMQ GmbH. All rights reserved.**
