# Task: 25337-workspace-auto-layout

## Objective

Add automatic and dynamic layout to the workspace

## Context

The workspace has a rudimentary - and static - routine for computing a layout of the nodes, and it's not very good.

Using professional graph layout libraries to add auto-layout support to the workspace would be a good idea.

You can consult the WORKSPACE_TOPOLOGY document to have an idea of the topology of the workspace, with its nodes and connection.

## Goal

- we want to use dagrejs/dagre for a simple tree layout
- we also want to use tgdwyer/WebCola to experiment mre complex and dynamic layouts (e.g. constraint-based or force-directed)
- use an internal feature flag restrict the features until they are not experimental anymore
- Different layout types and options must be added to a configuration widget, located in the search toolbar in the workspace.
- One of the layout option MUST be the possibility to "save" the current positions of nodes in a user-specific and reusable configuration
- Applying layout could be done "statically" (i.e. on user request) or "dynamically" (i.e. every time a node or edge is added/removed). Both modes must be supported.
- From the description of the topology, propose a series of layout algorithms to try out

### Implementation

- The DAGRE_LAYOUT document shows an example of integration of the `dagrejs/dagre` library in React Flow, using a vertical or horizontal tree. There are other examples online
- The UI is developed in React with ChakraUI. A custom theme is available in `src/modules/Theme`
- The graph in the workspace is implemented with React Flow. The main canvas is `src/modules/Workspace/components/ReactFlowWrapper.tsx`
- The search/filter/layout tollbar is `src/modules/Workspace/components/controls/CanvasToolbar.tsx`
- All workspace-related code is located in the `src/modules/Workspace` directory
- Node-related components are in `src/modules/Workspace/components/nodes`
- Edge-related components are in `src/modules/Workspace/components/edges`
- workspace-related hooks are in `src/modules/Workspace/hooks`
- utility functions are in `src/modules/Workspace/utils`
- data-related hooks are in `src/api/hooks`
- a placeholder for applying a layout to the nodes after their creation is `src/modules/Workspace/utils/layout-utils.ts`
