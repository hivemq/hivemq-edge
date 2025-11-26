## Group Wizard: Organize Your MQTT Infrastructure Visually

HiveMQ Edge now includes an interactive **Group Wizard** that transforms how you organize large MQTT topologies. Instead of managing dozens of individual adapters and bridges in a flat workspace, create visual containers that reflect your real-world infrastructure—production lines, geographic locations, or any organizational structure that makes sense for your operations.

### What It Is

The Group Wizard guides you through creating visual node containers directly on the workspace canvas. Select nodes by clicking them, see a real-time preview of your group boundary, and complete the workflow in seconds.

The wizard provides:

- **Interactive Canvas Selection** - Click nodes directly to add them to the group
- **Ghost Preview** - Semi-transparent boundary shows exactly what you're creating before committing
- **Constraint Validation** - Live feedback ensures you've selected enough nodes (minimum 2)
- **Nested Hierarchies** - Create groups within groups up to 3 levels deep for complex organizations
- **Mixed Node Types** - Group adapters, bridges, and other groups in any combination

### How It Works

1. **Open the wizard** - Click "Create New" in the workspace toolbar and select "GROUP"
2. **Select your nodes** - Click adapters or bridges on the canvas (minimum 2 required)
3. **Watch the ghost preview** - A dashed-border container appears, updating as you select nodes
4. **Configure the group** - Enter a name like "Production Sensors" and optionally choose a color
5. **Submit** - Watch the ghost transform into a real group with a smooth animation

The selection panel tracks your progress, showing "2 nodes selected (min: 2)" and enabling the Next button once constraints are met. Ghost groups update instantly as you add or remove nodes.

![Group wizard showing interactive node selection with ghost preview](./screenshots/PR-ghost-nodes-multiple.png)  
_The Group Wizard in action: Two adapters selected with ghost preview (dashed border) showing the future group container. Selection panel provides live validation feedback._

### How It Helps

#### Navigate Large Topologies Faster

Find components instantly by collapsing irrelevant groups. In a workspace with 50 adapters, collapse "Production Line A" and "Testing Environment" to focus only on "Production Line B"—no more scanning through unrelated nodes.

#### Reflect Real-World Structure

Map your digital topology to physical infrastructure. Create groups for "San Jose Data Center," "Building A Floor 3," or "Production Line 2" so your workspace mirrors your actual facility layout—making it intuitive for new team members.

#### Simplify Operations Workflows

Separate production from staging environments visually. Operators can immediately distinguish critical live systems from test deployments, reducing the risk of accidentally modifying production configurations.

#### Scale Without Chaos

Start with 5 adapters today, grow to 100 tomorrow. Groups keep workspaces organized as infrastructure expands—preventing the "everything is everywhere" problem that makes large topologies unmanageable.

### Looking Ahead

The Group Wizard available today represents our **initial implementation**, focused on core grouping workflows. **We're actively collecting feedback from users managing diverse MQTT topologies** to understand which organizational patterns matter most. Future enhancements will include group templates, bulk selection tools, and advanced nesting visualizations based on real-world usage patterns. Try the wizard with your infrastructure and share what organizational structures would help you most—your feedback directly shapes our roadmap.

---

## Getting Started

Try the Group Wizard today in your HiveMQ Edge 2024.11 workspace. Start with a small group (2-3 adapters) to experience the workflow, then expand your organizational structure as needed.

**Learn More:**

- Product Documentation: Complete group workflows and API reference
- Community Forums: Share organizational patterns and ask questions at [community.hivemq.com](https://community.hivemq.com)
- HiveMQ Edge Docs: [docs.hivemq.com/hivemq-edge](https://docs.hivemq.com/hivemq-edge)

---

_Feature available in HiveMQ Edge 2024.11 and later._
