## Workspace Status Visualization: See What's Running and What's Configured

### What It Is

The workspace now provides clear, real-time status indicators for every node and connection in your topology. You can instantly see which parts of your MQTT infrastructure are running, which have configuration issues, and which are partially set up.

The status display uses two complementary indicators:

- **Runtime Status** (shown as color) - Whether the node is actively running, inactive, or experiencing errors
- **Operational Status** (shown as animation) - Whether the node is fully configured, partially configured (draft), or not configured

Each edge (connection) also shows its own operational status so you can see the complete picture of which data flows are ready to operate.

### How It Works

1. **Look at node colors** to see runtime status:

   - **Green** - Active and running
   - **Yellow** - Inactive but available
   - **Red** - Error or stopped

2. **Watch for animated edges** that indicate operational configuration:

   - **Animated edge** - Fully configured and ready to operate
   - **Static edge** - Partially or not configured

3. **Access the observability panel** to see detailed status information and configuration state for any node or edge

4. **Check passive nodes** (devices, hosts, combiners) which automatically show status based on their upstream connections:

   - A device shows green if its adapter is active
   - A combiner shows error if any input adapter has an error
   - The Edge broker reflects the overall health of your topology

5. **Status updates in real-time** as your adapters, bridges, and agents start or stop

The status propagation follows the natural flow of your topology, so understanding what's happening in one part of your infrastructure helps you understand the impact downstream.

![Workspace Status Visualization - Showing runtime status colors and operational animation on edges](./screenshot-workspace-status.png)

### How It Helps

#### Troubleshoot Quickly

Red nodes and static edges immediately show where problems exist. No more guessing—the color coding and animation tell you exactly which parts of your topology need attention.

#### Understand Data Flow Health

See not just which nodes are running, but which data flows are ready to operate. A green adapter connected by an animated edge to a combiner means data is both flowing and configured. A green adapter with a static edge means data is flowing but the destination isn't ready.

#### Trust Your Configuration

Operational status animation confirms that your entire flow is configured end-to-end. All animated edges mean you're ready to go. Any static edges indicate where configuration is incomplete.

#### Monitor at a Glance

Don't need to click through status panels—everything is visible on the canvas. The color and animation tell the story of your topology's health and readiness in seconds.

### Looking Ahead

The status visualization is designed to evolve with more sophisticated insights. We're considering adding performance indicators, warning states for degraded conditions, and predictive alerts for potential issues. The current runtime and operational status framework provides the foundation for these future enhancements.

If you discover scenarios where the status visualization could be clearer or more helpful, your feedback will help us improve it.

---

**Glance at your workspace and instantly understand what's running, what's configured, and what needs attention.**
