# User Documentation: Workspace Creation Wizard

**Feature Release:** HiveMQ Edge 2024.11  
**Last Updated:** November 14, 2025

---

## Workspace Creation Wizard: Build Your MQTT Architecture Without Leaving the Canvas

### What It Is

HiveMQ Edge now includes a **guided creation wizard** that allows you to create entities directly within the workspace. Instead of navigating away from the workspace or using different creation patterns for different entity types, you can now use the "Create New" button to start a step-by-step wizard that shows you exactly what you're creating before you commit.

The wizard supports four entity types:

- **Adapters** - Protocol adapters (HTTP, OPC-UA, Simulation) with ghost preview showing DEVICE → ADAPTER → EDGE BROKER topology
- **Bridges** - MQTT bridges with ghost preview showing HOST → BRIDGE → EDGE BROKER topology
- **Combiners** - Data combining nodes with interactive source selection from existing workspace nodes
- **Asset Mappers** - Asset mapping nodes with required Pulse Agent integration and source selection

Each wizard provides a visual preview using transparent "ghost nodes" that show exactly where your new entity will appear in the workspace, along with a progress bar that guides you through configuration steps. For combiners and asset mappers, you can select source nodes directly on the canvas with real-time validation feedback.

---

### How It Works

1. **Open your workspace** and locate the "Create New" button in the workspace toolbar (near the search and filter controls)
2. **Click "Create New"** and select the entity type you want to create from the dropdown menu
3. **Review the ghost preview** showing transparent nodes that represent where your entity will appear
4. **Click "Next"** in the progress bar at the bottom of the screen to begin configuration
5. **Configure your entity** using the familiar forms in the side panel (same forms as standalone creation)
6. **Click "Complete"** to create the entity and see ghost nodes transform into real workspace nodes

All wizard operations complete instantly—ghost nodes render in milliseconds, and the workspace remains responsive throughout the creation process. You can cancel at any step by clicking "Cancel" in the progress bar or pressing the `Escape` key.

![Workspace wizard showing ghost preview and progress bar](../../../cypress/screenshots/workspace/wizard/wizard-create-adapter.spec.cy.ts/Workspace%20Wizard%20/%20Adapter%20wizard%20progress.png)

_Ghost nodes on workspace canvas showing where an adapter will be created, with progress bar at bottom showing "Step 1 of 3"_

---

### How It Helps

#### Unified Creation Experience

All entity types now use the same creation pattern—no more remembering which entities are created in the workspace versus separate views. Whether you're creating an adapter, bridge, combiner, or asset mapper, the wizard follows the same flow: preview, select (if needed), configure, complete.

#### See Before You Create

The ghost preview system shows you exactly how new entities will fit into your workspace topology before you commit. You can verify placement, understand connections, and cancel if it doesn't match your expectations—all before spending time on configuration.

#### Stay in Context

The wizard keeps your workspace visible while you configure. The side panel doesn't hide your canvas, so you maintain spatial awareness of where entities are being added and how they relate to existing nodes.

#### Interactive Source Selection

For combiners and asset mappers, select source nodes directly on the canvas with real-time validation. The wizard tells you immediately if your selection satisfies requirements (e.g., "Combiner requires at least 2 sources"), and you can adjust without starting over.

---

### Looking Ahead

The creation wizard available today covers **core entity types (Phase 1)**. The next phase will extend the wizard pattern to integration points—configuration options that attach to existing entities rather than creating new ones.

**Upcoming wizard types in Phase 2** (based on user feedback and priority):

- **TAG Wizard** - Attach tags to devices directly from the workspace
- **Topic Filter Wizard** - Configure edge broker subscriptions without leaving the canvas
- **Data Mapping Wizards** - Add northbound and southbound mappings to adapters
- **Data Combining Wizard** - Configure combiner mappings interactively
- **Group Wizard** - Create groups by selecting multiple nodes

Consider this wizard as a **foundation that will expand** based on your feedback. If you find the wizard helpful or have suggestions for improvement, please share your experience with us!

---

**Try the workspace wizard in your next deployment and discover how in-context creation streamlines your MQTT architecture workflow.**
