# Task: 38111-workspace-operation-wizard

## Objective

Add a custom "wizard" to allow end-users to create (or modify) entities directly in the workspace.

## Context

The workspace has an uneven approach to the direct creation of "entities" (nodes and edges) in the graph: some like
`Combiner` or `Asset Mapper` can only be created in the workspace, others like `Adapter` or `Bridge` can only be created
outside the workspace.

We want to provide a consistent way to create entities in the workspace, and to allow end-users to create entities,
modify them, delete them and create and modify the "integration points" (i.e. the data sources and operations sustained
by certain entities ) directly in the workspace.

Most of the creations are already supported by "side panels" (drawers) containing the form required to publish a new
entities. Hooks, form and utilities might be in another part of the app routing but should be easily reused in the
context of the workspace.

Some creation will be direct, e.g., adding a new `Adapter` will proceed after completion of the configuration.
Others will require interactions with the workspace, e,g, adding a new `Asset Mapper` will require the user to select
data sources and targets in the workspace.

The creation "wizard" MUST have at least the following "wizard step" (let's find a better name!):

> I want to create (select one of the following)
>
> - Entities
>   - Adapter (new)
>     1.  observe the addition on the graph (DEVICE → ADAPTER → EDGE BROKER)
>     2.  configure the new adapter (2 steps: select type + configure)
>     3.  create the adapter
>   - Bridge (new)
>     1.  observe the addition on the graph (HOST → BRIDGE → EDGE BROKER)
>     2.  configure the new bridge
>     3.  create the bridge
>   - Combiner (update)
>     1.  select the sources on the workspace
>     2.  observe the addition on the graph (sources → COMBINER → EDGE BROKER)
>     3.  configure the new Combiner
>     4.  create the Combiner
>   - Asset Mapper (update)
>     1.  select the sources on the workspace, including the Pulse Agent as mandatory
>     2.  observe the addition on the graph (sources + PULSE AGENT → COMBINER → EDGE BROKER)
>     3.  configure the new Asset Mapper
>     4.  create the Asset Mapper
>   - Group (update)
>     1.  select the sources on the workspace (not already in a group)
>     2.  observe the addition on the graph
>     3.  configure the new Group
>     4.  create the Group
> - Integration Points
>   - TAG (update)
>     1.  Select the Device
>     2.  configure the new tags
>     3.  illustrate the new tags on the node
>   - TOPIC FILTER (update)
>     1.  select the Edge Broker
>     2.  configure the new topic filters
>     3.  illustrate the new topic filters on the node
>   - DATA MAPPING (Northbound) (update)
>     1.  select the Adapter
>     2.  configure the new mappings
>     3.  illustrate the new mappings on the node
>   - DATA MAPPING (Southbound) (update)
>     1.  select the Adapter
>     2.  configure the new mappings
>     3.  illustrate the new mappings on the node
>   - DATA COMBINING (update)
>     1.  select the combiner or the sources
>     2.  configure the new mappings
>     3.  illustrate the new mappings on the node

For information:

- `new` means no existing flow in the workspace
- `update` means an update of an existing flow, usually through a CTA on the node's toolbar. But users are required to know the node to select first then the action, which is what we want to change
- `select` means we want users to be able to select a node on the graph (and see the selection feedback)
- `configure` means the configuration form is open in a side panel, waiting for completion to proceed
- `observe` means that one or several "ghost nodes" could be added to the graph during the "wizard"
- `create` means that the configuration has been validated and the ghost is removed, replaced by the real instance on the graph
- `illustrate` means that the "integration points" are usually visible as a marker on the node and should be updated temporarily to show the new additions

You can consult the WORKSPACE_TOPOLOGY document to have an idea of the topology of the workspace, with its nodes and connection.

## Important references

Refer to the following tasks for more details on the workspace:

- .tasks/25337-workspace-auto-layout
- .tasks/32118-workspace-status

You MUST also refer to the following guidelines and abide by them

- .tasks/REPORTING_STRATEGY.md
- .tasks/I18N_GUIDELINES.md

## Acceptance Criteria

- The **wizard** is in four parts:
  - a **trigger** (a CTA on the workspace canvas)
  - a **progress** or feedback bar
  - a **ghost** or transient node (or nodes), showing the result of the process when done (ONLY FOR ENTITIES)
  - the **configuration** form of the node (a side panel)
- The **trigger** MUST be a simple button with a dropdown containing the list of entities and integration points that can be created
  - Better situated in the CanvasToolbar, along search nd filter
- The **progress bar** MUST be a simple, one-liner progress bar
  - Should be in a React Flow panel, at the bottom-center, betwen canvas tools and minimap
  - It describes the current step and the total number of steps
  - It allows users to cancel the wizard
- The **ghost nodes** MUST be created on the React Flow canvas as soon as enough information is available
  - They MUST be visually distinct from real nodes (lighter, dashed border, etc.)
  - They MUST be removed if the wizard is cancelled
  - They MUST be replaced by the real node when the configuration is validated
- The configuration panel MUST be a side panel, opened by the user, and closed when the wizard is cancelled or completed

  - It MUST integrates the code that might exist somewhere else (like for Bridge or Adapter)
  - It MUST trigger the existing configuration route (combiner or asset mapper)

- The "wizard" MUST respect rules of responsive layout and accessibility

## Additional considerations

- Act as a senior designer to propose a good designer for the wizard. Promote simplicity, clarity, usability and accessibility.
- Ensure that the development is gradual, breaking down your plan into manageable and accountable steps
- The wizard must be future-proof, as this is a proof-of-concept that is likely to need revision. The list of wizard targets and steps is likely to be expanded in the future
- Any React component MUST be designed reusing as much as possible from the existing ChakraUI components or the custom ones in the code base

## Implementation

- The `workspace` is `src/modules/Workspace`
- The `Adapter` is `src/modules/ProtocolAdapters`
- The `Device` is `src/modules/Device`
- The `Bridge` is `src/modules/Bridges`
- The `Combiner` is `src/modules/Mappings`
- The `Asset Mapper` is `src/modules/Pulse`
- The `Group` is `src/modules/Group`
- The `TAG` is associated to `Device` and is `src/modules/Workspace/components/drawers/DevicePropertyDrawer.tsx`
- The `TOPIC FILTER` is associated to `Edge Broker` and is `src/modules/TopicFilters/TopicFilterManager.tsx`
- The `DATA MAPPING` is associated to `Adapter` and is `src/modules/Mappings/AdapterMappingManager.tsx`
- The `DATA COMBINING` is associated to `Combiner` and is `src/modules/Mappings/CombinerMappingManager.tsx`
