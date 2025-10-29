# Task: 32118-workspace-status

## Objective

Improve the visibility of the nodes' status in the Workspace and their propagation as induced status through the connections.

## Context

The workspace is a graph-based representation of the topology of the Edge broker, connecting different entities
such as `Devices`, `Adapters`, `Bridges`, `Pulse Agent`, etc., to each other and to the reification of the `Edge`
broker itself. The entities represents the sources or destinations of a data point in the topology, while the
connections materialise the configuration of a data transformation flow from one entity to the other (but not
necessarily the flow of data itself)

The following entities are defined in the current implementation of the workspace

- The **Edge Broker** (`EDGE_NODE`) is the central hub of the topology, a gateway to the MQTT broker and messaging
- A **Device** (`DEVICE_NODE`) is a physical or digital piece of hardware that can be connected to the **Edge** broker,
  through some specific protocol
- An **Adapter** (`ADAPTER_NODE`) is a piece of software in charge of communicating with a particular type of Device, through
  a specific protocol
- A **Bridge** (`BRIDGE_NODE`) is a piece of software that offers bidirectional MQTT bridge functionality to connect it to enterprise MQTT brokers and forward messages or receive messages from upstream.
- A **Host** (`HOST_NODE`) is a physical or digital piece of hardware that the bridge connects to
- A **Pulse Agent** (`PULSE_NODE`) is a distributed execution engine that integrates the topology with HiveMQ Pulse.
- A **Data Combiner** (`COMBINER_NODE`) is a piece of software that can combine data from multiple sources into a single output stream
- An **Asset Mapper** (`ASSET_MAPPER_NODE`) is a special case of a **Combiner** that expect a Pulse asset as the destination of the data combining process
- A **Group** (`CLUSTER_NODE`) is a logical grouping of entities such as adapters (and their devices) and bridges, to simplify the management of the topology

Using graph theory terminology:

- entities are nodes
- connections are edges
- groups are nested graphs and subflows
- the graph is a network, ensured to be a Directed Acyclic Graph (DAG)

In the current version (V1) of the `Workspace`, the possible (directed) connections are:

- `ADAPTER_NODE` -> `DEVICE_NODE`
- `ADAPTER_NODE` -> `EDGE_NODE`
- `ADAPTER_NODE` -> `COMBINER_NODE`
- `ADAPTER_NODE` -> `ASSET_MAPPER_NODE`
- `BRIDGE_NODE` -> `EDGE_NODE`
- `BRIDGE_NODE` -> `HOST_NODE`
- `BRIDGE_NODE` -> `COMBINER_NODE`
- `BRIDGE_NODE` -> `ASSET_MAPPER_NODE`
- `PULSE_NODE` -> `EDGE_NODE`
- `PULSE_NODE` -> `MAPPER_NODE`

Two more nodes have no existence on the workspace yet but will be mentioned for consistency and evolution (v2):

- `ADAPTER_NODE` -> `NORTHBOUND_MAPPER_NODE` for a north-bound data mapper from the device to the Edge broker
- `NORTHBOUND_MAPPER_NODE` -> `EDGE_NODE`
- `ADAPTER_NODE` -> `SOUTHBOUND_MAPPER_NODE` for a south-bound data mapper from the Edge broker to the device
- `SOUTHBOUND_MAPPER_NODE` -> `EDGE_NODE`

In the current version of the `Workspace`, the runtime status of nodes are available for three entities:

- ADAPTER_NODE
- BRIDGE_NODE
- PULSE_NODE

The status is periodically listened to and changes are immediately propagated to their visualisation in the workspace.

The status is visible on these nodes as a string, but also on the outbound edges of these nodes by a colour (green for active, red for error, yellow for inactive).
The other nodes and edges have no explicit status and are neither consistently represented nor updated in the `Workspace`. And this is what we want to change

## Goal

- refactor the handling of status for the active nodes
- refactor the propagation of status to the passive nodes through the inbound and outbound edges

## For development

- The UI is developed in React with ChakraUI. A custom theme is available in `src/modules/Theme`
- The graph in the workspace is implemented with React Flow. The main canvas is `src/modules/Workspace/components/ReactFlowWrapper.tsx`
- All workspace-related code is located in the `src/modules/Workspace` directory
- Node-related components are in `src/modules/Workspace/components/nodes`
- Edge-related components are in `src/modules/Workspace/components/edges`
- workspace-related hooks are in `src/modules/Workspace/hooks`
- utility functions are in `src/modules/Workspace/utils`
- data-related hooks are in `src/api/hooks`

## Acceptance Criteria

- The status of nodes in the workspace MUST be defined in two parts: RUNTIME status and OPERATIONAL status
- RUNTIME Status of active nodes is different from each others, but a general "node status" MUST be implemented and mapped to the original ones
  - ACTIVE
  - INACTIVE
  - ERROR
- RUNTIME Status of passive nodes MUST be defined as follows:
  - ACTIVE: the node is fully operational, at least one upstream active node is ACTIVE, and no upstream node is in ERROR
  - INACTIVE: no upstream active node is ACTIVE, and no upstream node is in ERROR
  - ERROR: at least one upstream active node is in ERROR
- OPERATIONAL status of a node describes whether configuration for data transformation have been create effectively
  - ACTIVE: the node is fully configured for data transformation
  - INACTIVE: the node is partially configured for data transformation (e.g. DRAFT mode) but not yet activated
  - ERROR: the node is not configured for data transformation
- The type of the React Flow nodes MUST be extended to use the new "node status", the status being stored in the data part of the node
- The status of passive nodes MUST be computed internally from their connections and the status of the active nodes, using the "computing flow" functionalities provided by react flow
- RUNTIME status MUST be rendered by colours on the nodes and the edges
- OPERATIONAL status MUST be rendered by a directed animation on the edges, e.g. the ReactFlow built-in `animated` attribute or a custom SVG rendering. THis animation MUST be light and unobtrusive
