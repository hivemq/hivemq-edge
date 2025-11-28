## Workspace Auto-Layout: Organize Your MQTT Architecture Effortlessly

### What It Is

HiveMQ Edge now includes **automatic layout algorithms** that intelligently organize your workspace nodes. Instead of manually positioning each element, select a layout and let the workspace arrange your MQTT infrastructure in seconds.

The feature offers five professional algorithms, each optimized for different topology patterns:

- **Dagre Vertical** - Clean top-to-bottom flow, perfect for sequential architectures
- **Dagre Horizontal** - Left-to-right organization, ideal for wide screens
- **Radial Hub** - EDGE node centered with connections radiating outward
- **Force-Directed** - Physics-based organic clustering that reveals natural relationships
- **Hierarchical Constraint** - Strict layer-based organization for formal structures

### How It Works

1. **Open your workspace** and locate the Layout Controls in the toolbar
2. **Select an algorithm** from the dropdown menu
3. **Click Apply Layout** to instantly reorganize your nodes
4. **Save as preset** (optional) to reuse the same arrangement across workspaces

All layouts execute instantly—even complex calculations complete in milliseconds, so you can iterate freely and compare different arrangements.

![Workspace Layout Controls - Showing algorithm selector and layout options](./screenshot-workspace-layout-controls.png)

### How It Helps

#### Better Visualization

See your MQTT architecture's structure clearly without manual node positioning. Different layouts reveal different aspects of your topology—from linear flows to interconnected relationships.

#### Faster Setup

Stop spending time on layout tweaking. Apply a layout in one click, then save it as a reusable preset for consistent workspace organization.

#### Explore Your Architecture

Try different layouts to understand your topology better. A force-directed layout might reveal unexpected clusters, while a hierarchical view clarifies data flow patterns.

### Looking Ahead

The layout algorithms available today represent our **initial implementation**. **We're actively collecting feedback from real-world MQTT topologies to continuously improve these layouts.** As users deploy HiveMQ Edge with diverse network architectures, we'll refine these algorithms to better match common patterns.

Consider these layouts as **starting points that will evolve** based on your feedback. If you notice improvement opportunities with your specific topology, please share your insights!

---

**Try the new layouts in your next workspace and discover which arrangement works best for your architecture.**
