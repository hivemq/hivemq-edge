import { describe, expect, it } from 'vitest'
import type { Edge, Node } from '@xyflow/react'

import { NodeTypes } from '@/modules/Workspace/types.ts'
import { nodeIdFor, pruneDanglingEdges, reconcileWorkspace } from './reconcile-utils.ts'

/** A node as the graph builder emits it — server entity in `data`, computed position. */
const buildNode = (id: string, type: NodeTypes, data: Record<string, unknown> = {}): Node => ({
  id,
  type,
  data,
  position: { x: 0, y: 0 },
})

/** The same node after the user has dragged it somewhere. */
const atPosition = (node: Node, x: number, y: number): Node => ({ ...node, position: { x, y } })

const connector = (id: string, source: string, target: string): Edge => ({ id, source, target })

describe('nodeIdFor', () => {
  it('should build the id shapes the graph builder uses', () => {
    // Not uniform by design — this is the trap the reconcile has to respect.
    expect(nodeIdFor.adapter('opcua-1')).toBe('adapter@opcua-1')
    expect(nodeIdFor.adapterDevice('opcua-1')).toBe('device@adapter@opcua-1')
    expect(nodeIdFor.bridge('bridge-1')).toBe('bridge@bridge-1')
    expect(nodeIdFor.bridgeHost('bridge-1')).toBe('host@bridge-1')
    expect(nodeIdFor.listener('tcp-listener')).toBe('listener@tcp-listener')
    // The odd one out: a combiner node carries the bare entity id, with no type prefix.
    expect(nodeIdFor.combiner('combiner-1')).toBe('combiner-1')
  })
})

describe('pruneDanglingEdges', () => {
  it('should drop edges whose endpoints are gone', () => {
    const nodes = [buildNode('edge', NodeTypes.EDGE_NODE)]
    const edges = [connector('connect-edge-adapter@gone', 'adapter@gone', 'edge')]

    expect(pruneDanglingEdges(edges, nodes)).toHaveLength(0)
  })

  it('should keep edges whose endpoints both survive', () => {
    const nodes = [buildNode('edge', NodeTypes.EDGE_NODE), buildNode('adapter@a', NodeTypes.ADAPTER_NODE)]
    const edges = [connector('connect-edge-adapter@a', 'adapter@a', 'edge')]

    expect(pruneDanglingEdges(edges, nodes)).toHaveLength(1)
  })
})

describe('reconcileWorkspace', () => {
  it('should refresh server data while keeping the position the user dragged to', () => {
    const existing = [atPosition(buildNode('adapter@a', NodeTypes.ADAPTER_NODE, { id: 'a', name: 'old' }), 420, 99)]
    const incoming = [buildNode('adapter@a', NodeTypes.ADAPTER_NODE, { id: 'a', name: 'new' })]

    const { nodes, summary } = reconcileWorkspace(existing, [], incoming, [])

    expect(nodes).toHaveLength(1)
    // Server truth replaced — this is what the append-only store gets wrong today.
    expect(nodes[0].data).toStrictEqual({ id: 'a', name: 'new' })
    // User truth kept.
    expect(nodes[0].position).toStrictEqual({ x: 420, y: 99 })
    expect(summary).toStrictEqual({ updated: 1, added: 0, removed: 0 })
  })

  it('should remove a ghost node the server no longer returns', () => {
    const existing = [buildNode('adapter@a', NodeTypes.ADAPTER_NODE), buildNode('combiner-1', NodeTypes.COMBINER_NODE)]
    const incoming = [buildNode('adapter@a', NodeTypes.ADAPTER_NODE)]

    const { nodes, summary } = reconcileWorkspace(existing, [], incoming, [])

    expect(nodes.map((node) => node.id)).toStrictEqual(['adapter@a'])
    expect(summary).toStrictEqual({ updated: 1, added: 0, removed: 1 })
  })

  it('should remove the whole cluster of a deleted adapter, edges included', () => {
    // One entity is three things on the canvas: the adapter, its device satellite, and a connector.
    const existing = [
      buildNode('edge', NodeTypes.EDGE_NODE),
      buildNode(nodeIdFor.adapter('a'), NodeTypes.ADAPTER_NODE),
      buildNode(nodeIdFor.adapterDevice('a'), NodeTypes.DEVICE_NODE),
    ]
    const existingEdges = [
      connector('connect-edge-adapter@a', nodeIdFor.adapter('a'), 'edge'),
      connector('connect-device@adapter@a', nodeIdFor.adapterDevice('a'), nodeIdFor.adapter('a')),
    ]
    const incoming = [buildNode('edge', NodeTypes.EDGE_NODE)]

    const { nodes, edges, summary } = reconcileWorkspace(existing, existingEdges, incoming, [])

    expect(nodes.map((node) => node.id)).toStrictEqual(['edge'])
    // The point of the test: no dangling connectors left pointing at nothing.
    expect(edges).toHaveLength(0)
    expect(summary.removed).toBe(2)
  })

  it('should remove the whole cluster of a deleted bridge, host satellite included', () => {
    const existing = [
      buildNode('edge', NodeTypes.EDGE_NODE),
      buildNode(nodeIdFor.bridge('b'), NodeTypes.BRIDGE_NODE),
      buildNode(nodeIdFor.bridgeHost('b'), NodeTypes.HOST_NODE),
    ]
    const existingEdges = [connector('connect-edge-bridge@b', nodeIdFor.bridge('b'), 'edge')]
    const incoming = [buildNode('edge', NodeTypes.EDGE_NODE)]

    const { nodes, edges, summary } = reconcileWorkspace(existing, existingEdges, incoming, [])

    expect(nodes.map((node) => node.id)).toStrictEqual(['edge'])
    expect(edges).toHaveLength(0)
    expect(summary.removed).toBe(2)
  })

  it('should add an entity that appeared on the server', () => {
    const existing = [buildNode('edge', NodeTypes.EDGE_NODE)]
    const incoming = [buildNode('edge', NodeTypes.EDGE_NODE), buildNode('adapter@new', NodeTypes.ADAPTER_NODE)]
    const incomingEdges = [connector('connect-edge-adapter@new', 'adapter@new', 'edge')]

    const { nodes, edges, summary } = reconcileWorkspace(existing, [], incoming, incomingEdges)

    expect(nodes.map((node) => node.id)).toStrictEqual(['edge', 'adapter@new'])
    expect(edges).toHaveLength(1)
    expect(summary).toStrictEqual({ updated: 1, added: 1, removed: 0 })
  })

  it('should never remove a group, which the server knows nothing about', () => {
    const group = buildNode('group@1', NodeTypes.CLUSTER_NODE, { title: 'Line 4', childrenNodeIds: ['adapter@a'] })
    const existing = [group, buildNode('adapter@a', NodeTypes.ADAPTER_NODE)]
    const incoming = [buildNode('adapter@a', NodeTypes.ADAPTER_NODE)]

    const { nodes, summary } = reconcileWorkspace(existing, [], incoming, [])

    expect(nodes.map((node) => node.id)).toStrictEqual(['group@1', 'adapter@a'])
    expect(summary.removed).toBe(0)
  })

  it('should keep a grouped node inside its group', () => {
    // A child's position is group-relative, so carrying position without parentage would fling it away.
    const existing = [
      {
        ...buildNode('adapter@a', NodeTypes.ADAPTER_NODE),
        parentId: 'group@1',
        expandParent: true,
        position: { x: 10, y: 20 },
      },
    ]
    const incoming = [buildNode('adapter@a', NodeTypes.ADAPTER_NODE)]

    const { nodes } = reconcileWorkspace(existing, [], incoming, [])

    expect(nodes[0].parentId).toBe('group@1')
    expect(nodes[0].expandParent).toBe(true)
    expect(nodes[0].position).toStrictEqual({ x: 10, y: 20 })
  })

  it('should leave the graph untouched when nothing changed', () => {
    const existing = [atPosition(buildNode('adapter@a', NodeTypes.ADAPTER_NODE, { id: 'a' }), 7, 7)]
    const existingEdges = [connector('connect-edge-adapter@a', 'adapter@a', 'adapter@a')]
    const incoming = [buildNode('adapter@a', NodeTypes.ADAPTER_NODE, { id: 'a' })]

    const { nodes, edges, summary } = reconcileWorkspace(existing, existingEdges, incoming, existingEdges)

    expect(nodes[0].position).toStrictEqual({ x: 7, y: 7 })
    expect(edges).toHaveLength(1)
    expect(summary).toStrictEqual({ updated: 1, added: 0, removed: 0 })
  })

  it('should not reorder the nodes already on the canvas', () => {
    const existing = [
      buildNode('adapter@c', NodeTypes.ADAPTER_NODE),
      buildNode('adapter@a', NodeTypes.ADAPTER_NODE),
      buildNode('adapter@b', NodeTypes.ADAPTER_NODE),
    ]
    const incoming = [
      buildNode('adapter@a', NodeTypes.ADAPTER_NODE),
      buildNode('adapter@b', NodeTypes.ADAPTER_NODE),
      buildNode('adapter@c', NodeTypes.ADAPTER_NODE),
    ]

    const { nodes } = reconcileWorkspace(existing, [], incoming, [])

    expect(nodes.map((node) => node.id)).toStrictEqual(['adapter@c', 'adapter@a', 'adapter@b'])
  })
})
