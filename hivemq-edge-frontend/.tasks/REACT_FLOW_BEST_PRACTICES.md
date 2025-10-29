# React Flow Best Practices & Migration Guide

**Last Updated:** October 25, 2025  
**Project:** HiveMQ Edge Frontend  
**Library:** React Flow ([@xyflow/react](https://reactflow.dev/))

## Purpose

This document records important learnings, best practices, and migration patterns for React Flow. It serves as a reference for AI assistants and developers to avoid deprecated APIs and use optimal patterns.

---

## ⚠️ Deprecation Notices & Migrations

### 1. `useHandleConnections` → `useNodeConnections` (October 2025)

**Status:** ⛔ **DEPRECATED**  
**Replacement:** `useNodeConnections`  
**Fixed In:** Task 32118-workspace-status (Phase 5 optimization)

#### Problem

Runtime warning appeared in console:

```
[DEPRECATED] `useHandleConnections` is deprecated. Instead use `useNodeConnections`
```

#### Migration Pattern

**❌ OLD (Deprecated):**

```typescript
import { useHandleConnections } from '@xyflow/react'

const connections = useHandleConnections({ type: 'target', id: 'Top' })
```

**✅ NEW (Correct):**

```typescript
import { useNodeConnections } from '@xyflow/react'

const connections = useNodeConnections({ handleType: 'target', handleId: 'Top' })
```

#### Key API Differences

| Old API (`useHandleConnections`) | New API (`useNodeConnections`)     |
| -------------------------------- | ---------------------------------- |
| `type: 'target' \| 'source'`     | `handleType: 'target' \| 'source'` |
| `id: string`                     | `handleId: string`                 |
| `nodeId: string`                 | `id: string`                       |

#### Complete Migration Example

```typescript
// Before
const connections = useHandleConnections({
  type: 'target', // ❌ Wrong parameter name
  id: 'Top', // ❌ Wrong parameter name
})

// After
const connections = useNodeConnections({
  handleType: 'target', // ✅ Correct parameter name
  handleId: 'Top', // ✅ Correct parameter name
})
```

#### Files Updated

All passive nodes in the workspace were updated:

- ✅ `NodeCombiner.tsx`
- ✅ `NodeDevice.tsx`
- ✅ `NodeHost.tsx`
- ✅ `NodeEdge.tsx`
- ✅ `NodeAssets.tsx`
- ✅ `NodeListener.tsx`

---

## 🎯 Best Practices for Computing Flows

### Pattern: Deriving Node Status from Connected Nodes

When implementing passive nodes that derive their state from upstream connections, follow this pattern:

```typescript
import { useNodeConnections, useNodesData, useReactFlow } from '@xyflow/react'

const MyPassiveNode: FC<NodeProps<MyNodeType>> = ({ id, data }) => {
  const { updateNodeData } = useReactFlow()

  // 1. Get connections efficiently (only for this node's handles)
  const connections = useNodeConnections({
    handleType: 'target',  // or 'source' for outgoing
    handleId: 'Top'        // specific handle ID (optional if node has only one)
  })

  // 2. Get data only for connected nodes (optimized subscriptions)
  const connectedNodes = useNodesData(
    connections.map((connection) => connection.source)
  )

  // 3. Compute derived state
  const derivedState = useMemo(() => {
    // Process connectedNodes data...
    return computedValue
  }, [connectedNodes])

  // 4. Store derived state in React Flow
  useEffect(() => {
    updateNodeData(id, { derivedState })
  }, [id, derivedState, updateNodeData])

  return <div>...</div>
}
```

### Why This Pattern?

✅ **Selective re-renders**: Only re-renders when connected nodes change  
✅ **No manual filtering**: React Flow tracks connections internally  
✅ **Optimized subscriptions**: Only subscribes to connected nodes' data  
✅ **Performance**: Avoids iterating through ALL nodes/edges

### Anti-Pattern: Using `useNodes()` + `useEdges()`

❌ **DON'T DO THIS:**

```typescript
// BAD: This subscribes to ALL nodes and edges!
const nodes = useNodes()
const edges = useEdges()

// Triggers re-computation when ANY node/edge changes ANYWHERE
const derivedState = useMemo(() => {
  const upstreamNodes = edges
    .filter((edge) => edge.target === id)
    .map((edge) => nodes.find((n) => n.id === edge.source))
  // ... compute from upstreamNodes
}, [nodes, edges, id]) // ❌ Re-computes on every graph change!
```

✅ **DO THIS INSTEAD:**

```typescript
// GOOD: Only subscribes to connected nodes!
const connections = useNodeConnections({ handleType: 'target' })
const connectedNodes = useNodesData(connections.map((c) => c.source))

// Only triggers when connected nodes change
const derivedState = useMemo(() => {
  // ... compute from connectedNodes
}, [connectedNodes]) // ✅ Only re-computes when connections change!
```

---

## 🔍 How to Detect Deprecation Warnings

### 1. Browser Console (Runtime)

**Where to look:** Browser DevTools Console during development

React Flow prints deprecation warnings at runtime:

```
[DEPRECATED] `useHandleConnections` is deprecated. Instead use `useNodeConnections`
```

**Action:** Search codebase for the deprecated API and migrate immediately.

### 2. TypeScript Compiler (Build-time)

**Where to look:** `pnpm exec tsc --noEmit` output

The TypeScript compiler may show:

```
warning TS1345: 'useHandleConnections' is deprecated.
```

**Action:** Check the JSDoc comments in the type definition file or React Flow docs.

### 3. IDE Warnings (Development)

**Where to look:** Editor inline warnings (VS Code, IntelliJ, etc.)

Modern IDEs show deprecation warnings with strikethrough text and hover info.

**Action:** Hover over the deprecated API to see the replacement suggestion.

### 4. ESLint (Static Analysis)

**Where to look:** ESLint output

If using `eslint-plugin-deprecation`:

```
error: This property is deprecated (deprecation/deprecation)
```

**Action:** Follow the error message guidance to migrate.

---

## 📚 React Flow Core Hooks Reference

### Data Management Hooks

| Hook                         | Purpose                           | Use When                                                      |
| ---------------------------- | --------------------------------- | ------------------------------------------------------------- |
| `useNodesData(nodeIds)`      | Get data for specific nodes       | Reading connected node data                                   |
| `useNodeConnections(params)` | Get connections for a node/handle | Finding connected nodes                                       |
| `useReactFlow()`             | Access React Flow instance        | Updating node/edge data programmatically                      |
| `updateNodeData(id, data)`   | Update specific node data         | Storing computed values in nodes                              |
| `useNodes()`                 | Get all nodes                     | ⚠️ Avoid for derived state (use `useNodesData` instead)       |
| `useEdges()`                 | Get all edges                     | ⚠️ Avoid for derived state (use `useNodeConnections` instead) |

### Best Practice Summary

1. **For reading connected node data**: Use `useNodeConnections` + `useNodesData`
2. **For writing node data**: Use `updateNodeData` from `useReactFlow`
3. **For accessing the full graph**: Only use `useNodes`/`useEdges` when truly needed
4. **For deriving state**: Compute in `useMemo` with minimal dependencies

---

## 📖 Official Documentation

- **React Flow Docs**: https://reactflow.dev/
- **Computing Flows Guide**: https://reactflow.dev/learn/advanced-use/computing-flows
- **API Reference**: https://reactflow.dev/api-reference
- **Migration Guides**: https://reactflow.dev/learn/troubleshooting/migrate-to-v12

---

## 🤖 Instructions for AI Assistants

When working with React Flow in this codebase:

1. ✅ **Always check this document** before using React Flow hooks
2. ✅ **Search for deprecation warnings** in browser console logs
3. ✅ **Use `useNodeConnections`** instead of `useHandleConnections`
4. ✅ **Use `handleType` and `handleId`** as parameter names (not `type` and `id`)
5. ✅ **Prefer `useNodesData(ids)`** over `useNodes()` for derived state
6. ✅ **Run TypeScript compiler** to catch deprecation warnings: `pnpm exec tsc --noEmit`
7. ✅ **Update this document** when discovering new deprecations or patterns

### Checklist for React Flow Changes

- [ ] Check if any hooks used are deprecated
- [ ] Use `useNodeConnections` for connection tracking
- [ ] Use `useNodesData` for reading connected node data
- [ ] Use `updateNodeData` for writing to nodes
- [ ] Avoid `useNodes`/`useEdges` for derived state
- [ ] Run `pnpm exec tsc --noEmit` to verify no errors
- [ ] Test in browser for runtime deprecation warnings
- [ ] Update this document if new patterns discovered

---

## 📝 Change Log

| Date       | Change                                                                            | Task/Issue                  |
| ---------- | --------------------------------------------------------------------------------- | --------------------------- |
| 2025-10-25 | Migrated from `useHandleConnections` to `useNodeConnections` in all passive nodes | Task 32118-workspace-status |
| 2025-10-25 | Created this best practices document                                              | Task 32118-workspace-status |

---

## 🔗 Related Documentation

- [Task 32118 Summary](./32118-workspace-status/TASK_SUMMARY.md)
- [Cypress Best Practices](./CYPRESS_BEST_PRACTICES.md)
- [Monaco Testing Guide](./MONACO_TESTING_GUIDE.md)
- [Error Message Tracing Pattern](./ERROR_MESSAGE_TRACING_PATTERN.md)
- [Active Tasks](./ACTIVE_TASKS.md)
