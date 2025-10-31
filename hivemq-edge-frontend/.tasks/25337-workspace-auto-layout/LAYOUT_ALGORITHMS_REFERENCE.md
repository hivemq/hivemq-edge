# Layout Algorithms - Complete Reference Guide

**Date:** October 27, 2025  
**Total Algorithms:** 5  
**Status:** Production Ready ✅

---

## Overview

The HiveMQ Edge workspace supports 5 professional graph layout algorithms, each optimized for different topology patterns and use cases. This document provides a complete reference for all algorithms, their parameters, and when to use each one.

---

## Quick Comparison Table

| Algorithm                   | Type         | Speed                | Best For                   | Node Spacing              |
| --------------------------- | ------------ | -------------------- | -------------------------- | ------------------------- |
| **Dagre Vertical Tree**     | Hierarchical | ⚡ Fast (5-50ms)     | Sequential flows, top-down | Auto (150px ranks)        |
| **Dagre Horizontal Tree**   | Hierarchical | ⚡ Fast (5-50ms)     | Wide screens, left-right   | Auto (200px ranks)        |
| **Radial Hub**              | Custom       | ⚡ Very Fast (<20ms) | Hub-spoke, EDGE centered   | 500px between layers      |
| **Force-Directed**          | Physics      | 🔥 Slower (50-500ms) | Complex relationships      | 350px link distance       |
| **Hierarchical Constraint** | Layer-based  | ⚡ Fast (50-200ms)   | Strict hierarchies         | 350px layers, 300px nodes |

---

## 1. Dagre Vertical Tree Layout (DAGRE_TB)

### Description

Top-to-bottom hierarchical tree layout using the Dagre graph layout library. Creates a clear parent-child flow with EDGE at the top.

### Visual Structure

```
         [EDGE]
           |
    ┌──────┼──────┐
    |      |      |
[COMBINER] [PULSE] [ADAPTER-1]
    |              |
[DEVICE-1]    [DEVICE-2]
```

### When to Use

✅ **Best for:**

- Sequential workflows
- Top-to-bottom information flow
- Traditional hierarchical data
- Process diagrams
- Standard workspace view

❌ **Not ideal for:**

- Wide graphs (many siblings)
- Complex interconnections
- Non-hierarchical structures

### Parameters (DagreOptions)

| Parameter             | Type                                                  | Default             | Range    | Description                                 |
| --------------------- | ----------------------------------------------------- | ------------------- | -------- | ------------------------------------------- |
| **rankdir**           | `'TB' \| 'LR'`                                        | `'TB'`              | -        | Rank direction: Top-Bottom or Left-Right    |
| **ranksep**           | `number`                                              | `150`               | 50-500px | Vertical space between ranks/layers         |
| **nodesep**           | `number`                                              | `80`                | 20-200px | Horizontal space between nodes in same rank |
| **edgesep**           | `number`                                              | `20`                | 10-100px | Space between edges                         |
| **ranker**            | `'network-simplex' \| 'tight-tree' \| 'longest-path'` | `'network-simplex'` | -        | Algorithm for rank assignment               |
| **animate**           | `boolean`                                             | `true`              | -        | Enable smooth transitions                   |
| **animationDuration** | `number`                                              | `300`               | 0-1000ms | Animation speed                             |
| **fitView**           | `boolean`                                             | `true`              | -        | Auto-zoom to fit all nodes                  |

### Key Features

- **Deterministic:** Same input always produces same output
- **Fast:** Optimized for large graphs
- **Clean:** No overlapping nodes or edges
- **Hierarchical:** Clear parent-child relationships

### Performance

- Small graphs (<10 nodes): 5-10ms
- Medium graphs (10-30 nodes): 10-30ms
- Large graphs (30-100 nodes): 30-50ms

### Tips

- Increase `ranksep` for taller layouts
- Increase `nodesep` for wider spacing
- Use `'tight-tree'` ranker for more compact layouts
- Use `'longest-path'` for maximum layer separation

---

## 2. Dagre Horizontal Tree Layout (DAGRE_LR)

### Description

Left-to-right hierarchical tree layout. Same as vertical but rotated 90°. EDGE appears on the left, information flows right.

### Visual Structure

```
[EDGE] ─── [COMBINER] ─── [DEVICE-1]
        │
        ├─ [PULSE]
        │
        └─ [ADAPTER-1] ─── [DEVICE-2]
```

### When to Use

✅ **Best for:**

- Wide screens/displays
- Horizontal reading flow
- Time-series or pipeline views
- Left-to-right workflows

❌ **Not ideal for:**

- Narrow screens
- Deep hierarchies (too wide)
- Vertical scrolling preference

### Parameters (DagreOptions)

| Parameter             | Type                                                  | Default             | Range    | Description                                   |
| --------------------- | ----------------------------------------------------- | ------------------- | -------- | --------------------------------------------- |
| **rankdir**           | `'TB' \| 'LR'`                                        | `'LR'`              | -        | Rank direction: Top-Bottom or Left-Right      |
| **ranksep**           | `number`                                              | `200`               | 50-500px | Horizontal space between ranks (more than TB) |
| **nodesep**           | `number`                                              | `80`                | 20-200px | Vertical space between nodes in same rank     |
| **edgesep**           | `number`                                              | `20`                | 10-100px | Space between edges                           |
| **ranker**            | `'network-simplex' \| 'tight-tree' \| 'longest-path'` | `'network-simplex'` | -        | Algorithm for rank assignment                 |
| **animate**           | `boolean`                                             | `true`              | -        | Enable smooth transitions                     |
| **animationDuration** | `number`                                              | `300`               | 0-1000ms | Animation speed                               |
| **fitView**           | `boolean`                                             | `true`              | -        | Auto-zoom to fit all nodes                    |

### Key Features

- Same as Vertical Tree but rotated
- Better for wide screens
- Horizontal flow more natural for timelines
- Utilizes horizontal space efficiently

### Performance

Same as Vertical Tree: 5-50ms depending on graph size

### Tips

- Increase `ranksep` to 250-300px for wider spacing
- Good for presentation on wide monitors
- Consider when hierarchy is shallow but wide

---

## 3. Radial Hub Layout (RADIAL_HUB)

### Description

Custom circular layout with EDGE at the center and nodes arranged in concentric rings based on their type. Visually striking and perfect for hub-spoke topologies.

### Visual Structure

```
                [EDGE]           ← Layer 0 (Center)

     [COMBINER]     [PULSE]      ← Layer 1 (r=500px)

  [ADAPTER-1]  [ADAPTER-2]  [BRIDGE-1]  ← Layer 2 (r=1000px)

[DEVICE-1]  [DEVICE-2]  [HOST-1]  ← Layer 3 (r=1500px)
```

### When to Use

✅ **Best for:**

- Hub-spoke architectures (perfect for EDGE!)
- Emphasizing central node importance
- Visual presentations
- Showing distance from center
- Radial relationships

❌ **Not ideal for:**

- Linear workflows
- Deep hierarchies (many layers)
- Graphs with unclear center
- When precision alignment matters

### Parameters (RadialOptions)

| Parameter             | Type      | Default      | Range     | Description                                                        |
| --------------------- | --------- | ------------ | --------- | ------------------------------------------------------------------ |
| **centerX**           | `number`  | `400`        | 0-∞       | X coordinate of center point                                       |
| **centerY**           | `number`  | `300`        | 0-∞       | Y coordinate of center point                                       |
| **layerSpacing**      | `number`  | `500`        | 200-800px | Distance between concentric layers (accounts for 245px node width) |
| **startAngle**        | `number`  | `-Math.PI/2` | 0-2π      | Starting angle in radians (-π/2 = top/12 o'clock)                  |
| **animate**           | `boolean` | `true`       | -         | Enable smooth transitions                                          |
| **animationDuration** | `number`  | `300`        | 0-1000ms  | Animation speed                                                    |
| **fitView**           | `boolean` | `true`       | -         | Auto-zoom to fit all nodes                                         |

### Layer Assignment (Type-Based)

| Layer | Node Types      | Radius Formula     |
| ----- | --------------- | ------------------ |
| **0** | EDGE, LISTENER  | 0px (exact center) |
| **1** | COMBINER, PULSE | 1 × 500 = 500px    |
| **2** | ADAPTER, BRIDGE | 2 × 500 = 1000px   |
| **3** | DEVICE, HOST    | 3 × 500 = 1500px   |

### Key Features

- **Fastest algorithm:** Simple trigonometry, no iterations
- **Deterministic:** Always same result
- **Type-aware:** Nodes grouped by semantic meaning
- **Visual appeal:** Striking circular arrangement
- **Glued node support:** DEVICE stays with ADAPTER

### Performance

- Small graphs (<10 nodes): <5ms ⚡
- Medium graphs (10-30 nodes): 5-10ms
- Large graphs (30-100 nodes): 10-20ms
- **Fastest of all algorithms!**

### Calculation Details

**Position per node:**

```typescript
radius = layerNumber × layerSpacing
angleStep = 360° / nodesInLayer
angle = startAngle + (angleStep × nodeIndex)

x = centerX + radius × cos(angle)
y = centerY + radius × sin(angle)
```

**Example:** Layer 2 with 4 adapters

```
radius = 2 × 500 = 1000px
angleStep = 360° / 4 = 90°
Positions: 0°, 90°, 180°, 270° (top, right, bottom, left)
```

### Tips

- Adjust `layerSpacing` to 400-600px based on node count per layer
- More nodes per layer → increase spacing to prevent crowding
- Use `startAngle` = 0 to start at right (3 o'clock)
- Use `startAngle` = π to start at left (9 o'clock)
- Perfect for demos and presentations!

---

## 4. WebCola Force-Directed Layout (COLA_FORCE)

### Description

Physics-based force simulation where nodes repel each other while edges act as springs. Creates organic, naturally-clustered layouts that emphasize connectivity patterns.

### Visual Structure

```
    [NODE-A]     [NODE-B]
       \  \       /  /
        \  \     /  /
         \  [NODE-C] /
          \    |    /
           [NODE-D]
```

_Organic positioning based on connectivity_

### When to Use

✅ **Best for:**

- Complex graphs with many interconnections
- Exploring relationship patterns
- Finding natural clusters
- Non-hierarchical structures
- Research and analysis

❌ **Not ideal for:**

- Need strict hierarchy
- Performance critical (slower)
- Large graphs (>100 nodes)
- Precise positioning requirements
- Production dashboards (non-deterministic)

### Parameters (ColaForceOptions)

| Parameter                | Type      | Default | Range     | Description                                                                   |
| ------------------------ | --------- | ------- | --------- | ----------------------------------------------------------------------------- |
| **linkDistance**         | `number`  | `350`   | 200-800px | Target distance between connected nodes (accounts for 245px node width + gap) |
| **avoidOverlaps**        | `boolean` | `true`  | -         | Enable automatic overlap removal                                              |
| **handleDisconnected**   | `boolean` | `true`  | -         | Handle disconnected subgraphs separately                                      |
| **convergenceThreshold** | `number`  | `0.01`  | 0.001-0.1 | Stop when changes smaller than this (lower = more accurate)                   |
| **maxIterations**        | `number`  | `1000`  | 100-5000  | Maximum simulation steps                                                      |
| **animate**              | `boolean` | `true`  | -         | Enable smooth transitions                                                     |
| **animationDuration**    | `number`  | `500`   | 0-1000ms  | Animation speed (longer for organic feel)                                     |
| **fitView**              | `boolean` | `true`  | -         | Auto-zoom to fit all nodes                                                    |

### Key Features

- **Organic:** Natural, aesthetically pleasing layouts
- **Cluster detection:** Related nodes naturally group together
- **Flexible:** Adapts to graph structure
- **Non-deterministic:** Each run may produce different (but similar) results
- **Overlap removal:** Built-in collision detection

### Performance

- Small graphs (<10 nodes): 50-100ms
- Medium graphs (10-30 nodes): 100-300ms
- Large graphs (30-100 nodes): 300-500ms
- **Slowest algorithm** (iterative simulation)

### Calculation Details

**Physics simulation:**

1. **Repulsion force:** Nodes push away from each other
2. **Spring force:** Edges pull connected nodes together
3. **Convergence:** Iterate until forces balance
4. **Overlap removal:** Adjust positions to prevent overlaps

**Performance tuning:**

- Lower `maxIterations` (500-700) for faster but less stable results
- Increase `convergenceThreshold` (0.02-0.05) to stop earlier
- Reduce `linkDistance` (250-300px) for denser layouts
- Increase `linkDistance` (400-500px) for sparser layouts

### Tips

- **For dense graphs:** Increase `linkDistance` to 400-500px
- **For sparse graphs:** Decrease to 250-300px
- **For faster results:** Set `maxIterations` to 500, `convergenceThreshold` to 0.02
- **For better clustering:** Keep defaults, allow full convergence
- Not suitable for real-time updates (too slow)
- Best for exploratory analysis and visualization

---

## 5. WebCola Hierarchical Constraint Layout (COLA_CONSTRAINED)

### Description

Layer-based hierarchical layout using WebCola's flow layout algorithm. Creates strict separation between layers while maintaining alignment within layers. Similar to Dagre but with WebCola's constraint solving.

### Visual Structure

```
         [EDGE]           ← Layer 0
           |
    ┌──────┴──────┐
[COMBINER]     [PULSE]    ← Layer 1 (gap: 350px)
    |              |
[ADAPTER-1]   [ADAPTER-2] ← Layer 2 (gap: 350px)
    |              |
[DEVICE-1]    [DEVICE-2]  ← Layer 3 (gap: 350px)
```

### When to Use

✅ **Best for:**

- Strict hierarchical structures
- Need perfect alignment within layers
- Precise layer separation required
- Alternative to Dagre with more control
- When layer order matters semantically

❌ **Not ideal for:**

- Graphs with cycles
- Flexible/organic layouts needed
- Very large graphs
- Non-hierarchical data

### Parameters (ColaConstrainedOptions)

| Parameter             | Type         | Default | Range     | Description                                                             |
| --------------------- | ------------ | ------- | --------- | ----------------------------------------------------------------------- |
| **flowDirection**     | `'x' \| 'y'` | `'y'`   | -         | Flow axis: 'y' for vertical (TB), 'x' for horizontal (LR)               |
| **layerGap**          | `number`     | `350`   | 200-800px | Space between layers (accounts for node height + visual gap)            |
| **nodeGap**           | `number`     | `300`   | 200-600px | Space between nodes in same layer (accounts for 245px node width + gap) |
| **animate**           | `boolean`    | `true`  | -         | Enable smooth transitions                                               |
| **animationDuration** | `number`     | `300`   | 0-1000ms  | Animation speed                                                         |
| **fitView**           | `boolean`    | `true`  | -         | Auto-zoom to fit all nodes                                              |

### Layer Assignment (Type-Based)

| Layer | Node Types      | Description  |
| ----- | --------------- | ------------ |
| **0** | EDGE, LISTENER  | Source layer |
| **1** | COMBINER, PULSE | First tier   |
| **2** | ADAPTER, BRIDGE | Second tier  |
| **3** | DEVICE, HOST    | Third tier   |

### Key Features

- **Strict layers:** Clear separation between hierarchy levels
- **Flow layout:** Uses WebCola's flowLayout() method
- **Alignment:** Nodes align within their layer
- **Type-based:** Automatic layer assignment by node type
- **Bidirectional:** Supports vertical (y) or horizontal (x) flow

### Performance

- Small graphs (<10 nodes): 50-100ms
- Medium graphs (10-30 nodes): 100-150ms
- Large graphs (30-100 nodes): 150-200ms
- **Faster than force-directed** but slower than Dagre

### Calculation Details

**Algorithm:**

1. Group nodes by type → layer
2. Apply flowLayout(axis, gap) constraint
3. WebCola solves for positions
4. Overlap removal if needed
5. Apply glued node offsets

**Gap calculations:**

```
layerGap = 350px
= Node height (~100px) + Visual gap (~250px)

nodeGap = 300px
= Node width (245px) + Visual gap (~55px)
```

### Tips

- **For tighter layouts:** Reduce `layerGap` to 250-300px
- **For looser layouts:** Increase `layerGap` to 400-500px
- **Horizontal flow:** Set `flowDirection: 'x'`, increase `layerGap` to 400px
- **More spacing within layers:** Increase `nodeGap` to 350-400px
- Alternative to Dagre when you need WebCola's constraint solving
- Better than force-directed when hierarchy is clear

---

## Parameter Quick Reference

### Spacing Parameters Comparison

| Algorithm                   | Primary Spacing | Value | Accounts For             |
| --------------------------- | --------------- | ----- | ------------------------ |
| **Dagre TB**                | ranksep         | 150px | Vertical ranks           |
| **Dagre LR**                | ranksep         | 200px | Horizontal ranks         |
| **Radial Hub**              | layerSpacing    | 500px | Node width (245px) + gap |
| **Force-Directed**          | linkDistance    | 350px | Node width (245px) + gap |
| **Hierarchical Constraint** | layerGap        | 350px | Node height + gap        |
| **Hierarchical Constraint** | nodeGap         | 300px | Node width (245px) + gap |

### Animation Parameters (All Algorithms)

| Parameter           | Default   | Recommended Range | Purpose                           |
| ------------------- | --------- | ----------------- | --------------------------------- |
| `animate`           | `true`    | -                 | Enable/disable smooth transitions |
| `animationDuration` | 300-500ms | 200-800ms         | Transition speed                  |
| `fitView`           | `true`    | -                 | Auto-zoom to fit all nodes        |

---

## Selection Guide

### Decision Tree

**Start here:**

1. **Is EDGE clearly the central hub?**

   - YES → Use **Radial Hub Layout** ✨ (fastest, most intuitive)
   - NO → Continue to #2

2. **Is the graph strictly hierarchical (clear parent-child)?**

   - YES → Continue to #3
   - NO → Continue to #4

3. **Hierarchical - Which direction?**

   - Top-to-bottom → **Dagre Vertical Tree** (standard)
   - Left-to-right → **Dagre Horizontal Tree** (wide screens)
   - Strict layer control → **Hierarchical Constraint** (alternative)

4. **Complex interconnections or exploring relationships?**
   - YES → **Force-Directed Layout** (organic, cluster finding)
   - NO → Use **Dagre Vertical Tree** (default fallback)

### By Use Case

| Use Case                  | Recommended Algorithm | Alternative             |
| ------------------------- | --------------------- | ----------------------- |
| **HiveMQ Edge workspace** | Radial Hub ⭐         | Dagre Vertical          |
| **Process workflows**     | Dagre Vertical        | Hierarchical Constraint |
| **Wide dashboards**       | Dagre Horizontal      | Radial Hub              |
| **Network analysis**      | Force-Directed        | Radial Hub              |
| **Org charts**            | Dagre Vertical        | Hierarchical Constraint |
| **Dependency graphs**     | Dagre Vertical        | Force-Directed          |
| **Social networks**       | Force-Directed        | Radial Hub              |
| **Presentations**         | Radial Hub ⭐         | Dagre Vertical          |

### By Performance Requirements

| Requirement             | Algorithm               | Speed       |
| ----------------------- | ----------------------- | ----------- |
| **Fastest possible**    | Radial Hub              | <20ms ⚡    |
| **Fast & hierarchical** | Dagre                   | 5-50ms ⚡   |
| **Moderate speed**      | Hierarchical Constraint | 50-200ms    |
| **Slowest but organic** | Force-Directed          | 50-500ms 🔥 |

---

## Advanced Configuration

### Combining with Constraints

All algorithms respect **glued node constraints**:

```typescript
// Example: DEVICE nodes stay glued to their ADAPTER
{
  gluedNodes: Map<
    nodeId,
    {
      parentId: string
      offset: { x: number; y: number }
      handle: 'source' | 'target'
    }
  >
}
```

This ensures:

- DEVICE nodes maintain fixed offset from ADAPTER
- LISTENER nodes stay with EDGE
- HOST nodes stay with BRIDGE

### Custom Configurations

**Example: Tight Radial Layout**

```typescript
{
  type: 'RADIAL_HUB',
  centerX: 500,
  centerY: 400,
  layerSpacing: 400, // Tighter spacing
  startAngle: 0,     // Start at right
  animate: true,
  animationDuration: 500
}
```

**Example: Fast Force-Directed**

```typescript
{
  type: 'COLA_FORCE',
  linkDistance: 300,
  maxIterations: 500,           // Half the default
  convergenceThreshold: 0.02,   // Stop earlier
  animate: false,               // Skip animation
}
```

**Example: Horizontal Hierarchy**

```typescript
{
  type: 'COLA_CONSTRAINED',
  flowDirection: 'x',  // Horizontal
  layerGap: 400,       // More space for horizontal
  nodeGap: 250,        // Less vertical space
}
```

---

## Troubleshooting

### Nodes Overlapping

**Radial Hub:**

- ✅ Increase `layerSpacing` to 600-700px

**Force-Directed:**

- ✅ Increase `linkDistance` to 400-500px
- ✅ Ensure `avoidOverlaps: true`
- ✅ Increase `maxIterations` to 1500-2000

**Hierarchical Constraint:**

- ✅ Increase `layerGap` to 400-500px
- ✅ Increase `nodeGap` to 350-400px

**Dagre:**

- ✅ Increase `ranksep` to 200-250px
- ✅ Increase `nodesep` to 100-150px

### Layout Too Spread Out

**All algorithms:**

- ✅ Reduce spacing parameters by 20-30%
- ✅ Use `fitView: true` to auto-zoom

### Poor Performance

**Force-Directed (slow):**

- ✅ Reduce `maxIterations` to 500-700
- ✅ Increase `convergenceThreshold` to 0.02-0.05
- ✅ Set `animate: false` for instant layout

**All algorithms:**

- ✅ Reduce `animationDuration` to 200ms or disable

### Non-deterministic Results

**Force-Directed:**

- ⚠️ This is expected behavior (physics simulation)
- ⚠️ Each run produces slightly different results
- ✅ Use Dagre or Radial for deterministic layouts

---

## Best Practices

### 1. Start with Radial Hub

For HiveMQ Edge workspaces, **Radial Hub is the best default**:

- Visually matches the hub-spoke architecture
- Fastest algorithm
- Most intuitive for users

### 2. Provide Multiple Options

Let users choose:

- Radial Hub (default)
- Vertical Tree (alternative)
- Force-Directed (exploration)

### 3. Adjust Spacing for Node Count

- **Few nodes (< 5 per layer):** Use smaller spacing
- **Many nodes (> 10 per layer):** Increase spacing significantly

### 4. Consider Screen Size

- **Wide screens:** Horizontal layouts work well
- **Standard screens:** Vertical or radial
- **Mobile:** Radial (most compact)

### 5. Performance Budget

- **Real-time updates:** Use Dagre or Radial only
- **Static layouts:** Any algorithm works
- **Large graphs:** Avoid Force-Directed

---

## Summary

### Algorithm Recommendations

🥇 **Best Overall:** Radial Hub Layout

- Fastest, most intuitive, perfect for edge architecture

🥈 **Best Hierarchical:** Dagre Vertical Tree

- Fast, clean, widely understood

🥉 **Best for Exploration:** Force-Directed Layout

- Finds natural clusters, organic appearance

### Key Takeaways

1. **Radial Hub** = 500px spacing between layers
2. **Force-Directed** = 350px link distance (node width + gap)
3. **Hierarchical Constraint** = 350px layer gap, 300px node gap
4. **Dagre** = 150-200px rank separation

5. All algorithms respect glued node constraints
6. All algorithms support animation (200-500ms)
7. All algorithms auto-fit viewport by default

---

**Document Version:** 1.0  
**Last Updated:** October 27, 2025  
**Total Algorithms:** 5  
**Status:** Production Ready ✅
